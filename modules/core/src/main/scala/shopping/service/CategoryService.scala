package shopping.service

import skunk._
import skunk.implicits._
import cats.syntax.all._
import cats.effect.{MonadCancelThrow, Resource}
import shopping.domain.category._
import shopping.sql.codecs.{categoryId, categoryName}
import shopping.utils.uuid.GenUUID
import shopping.domain.id.ID

trait CategoryService [F[_]] {

  def findAll: F[List[Category]]

  def create (name: CategoryName): F[CategoryId]
}

object CategoryService {

  import CategorySQL._

  def make [F[_]: MonadCancelThrow: GenUUID] (
    postgres: Resource[F, Session[F]]
  ) = new CategoryService[F] {

    def findAll: F[List[Category]] = postgres.use { 
      _.execute(selectAll)
    }

    def create (name: CategoryName): F[CategoryId] = {
      val cmdResource = for {
        session <- postgres
        cmd     <- session.prepare(insert)
      } yield cmd

      cmdResource.use { cmd => 
        ID.make[F, CategoryId].flatMap { id =>
          cmd.execute(Category(id, name)) as id
        }
      }
    }
  }
}

private object CategorySQL {

  val codec: Codec[Category] =
    ( categoryId
    ~ categoryName
    ).imap { case id ~ name =>
      Category(id, name)
    } { cat => cat.uuid ~ cat.name }

  val selectAll: Query[Void, Category] =
    sql"SELECT * FROM categories".query(codec)

  val insert: Command[Category] = 
    sql"""
    INSERT INTO categories
    VALUES ($codec)
    """.command
}