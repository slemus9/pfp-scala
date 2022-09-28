package shopping.service

import skunk._
import skunk.implicits._
import cats.syntax.all._
import cats.effect.{MonadCancelThrow, Resource}
import shopping.domain.user._
import shopping.sql.codecs._
import shopping.utils.uuid.GenUUID
import shopping.domain.id.ID

trait UserService [F[_]] {

  def find (username: UserName): F[Option[UserWithPassword]]

  def create (username: UserName, password: EncryptedPassword): F[UserId]
}

object UserService {

  import UserSQL._

  def make [F[_]: MonadCancelThrow: GenUUID] (
    postgres: Resource[F, Session[F]]
  ) = new UserService[F] {

    def find (username: UserName): F[Option[UserWithPassword]] = postgres.use {
      _.prepare(selectUser).use { 
        _.option(username)
      }
    }

    def create (
      username: UserName, 
      password: EncryptedPassword
    ): F[UserId] = postgres.use(_.prepare(insertUser).use { cmd => 
      ID.make[F, UserId].flatMap { id => 
        val user = UserWithPassword(id, username, password)
        cmd
          .execute(user)
          .as(id)
          .recoverWith {
            case SqlState.UniqueViolation(_) => // Username already exists
              UserNameInUse(username).raiseError
          }
      }
    })
  }
}

private object UserSQL {

  val codec: Codec[UserWithPassword] =
    ( userId
    ~ userName
    ~ encryptedPassword
    ).imap {
      case id ~ name ~ pass => 
        UserWithPassword(id, name, pass)
    } {
      case user =>
        user.id ~ user.name ~ user.password
    }

  val selectUser: Query[UserName, UserWithPassword] =
    sql"""
    SELECT * FROM users
    WHERE name = $userName
    """.query(codec)

  val insertUser: Command[UserWithPassword] =
    sql"""
    INSERT INTO users
    VALUES ($codec)
    """.command
}