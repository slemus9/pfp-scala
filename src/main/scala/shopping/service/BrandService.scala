package shopping.service

import skunk._
import skunk.implicits._
import cats.syntax.all._
import cats.effect.{MonadCancelThrow, Resource}
import shopping.domain.brand._
import shopping.sql.codecs.{brandId, brandName}
import shopping.utils.uuid.GenUUID
import shopping.domain.id.ID

trait BrandService [F[_]] {

  def findAll: F[List[Brand]]

  def create (name: BrandName): F[BrandId]
}

object BrandService {

  def make [F[_]: GenUUID: MonadCancelThrow] (
    postgres: Resource[F, Session[F]]
  ) = new BrandService[F] {

    import BrandSQL._

    def findAll: F[List[Brand]] = 
      postgres.use(_.execute(selectAll))

    def create (name: BrandName): F[BrandId] = postgres.use { session => 
      session.prepare(insertBrand).use { cmd => 
        ID.make[F, BrandId].flatMap { id => 
          cmd.execute(Brand(id, name)).as(id)  
        }
      }  
    }
  }
}

private object BrandSQL {

  val codec: Codec[Brand] = 
    (brandId ~ brandName).imap {
      case i ~ n => Brand(i, n)
    } {
      b => b.uuid ~ b.name
    }

  val selectAll: Query[Void, Brand] =
    sql"""
    SELECT * FROM brands
    """.query(codec)

  val insertBrand: Command[Brand] =
    sql"""
    INSERT INTO brands
    VALUES ($codec)
    """.command
}