package shopping.domain

import io.circe.generic.semiauto._
import io.estatico.newtype.macros.newtype
import java.util.UUID
import shopping.utils.circe.CirceCodec
import shopping.utils.circe.refined.codecOf
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.collection.NonEmpty
import scala.util.control.NoStackTrace
import io.circe.{Decoder, Codec}
import cats.syntax.either._
import derevo.derive
import derevo.cats.{eqv, show}
import shopping.utils.uuid.IsUUID.uuid

object user {

  @derive(eqv, show, uuid)
  @newtype final case class UserId (value: UUID)

  object UserId {
    implicit val jsonCodec = CirceCodec.from[UUID, UserId](
      UserId(_), _.value
    )
  }

  // @newtype final case class JwtToken (value: String)

  // object JwtToken {
  //   implicit val jsonCodec = CirceCodec.from[String, JwtToken](
  //     JwtToken(_), _.value
  //   )
  // }

  @derive(eqv, show)
  @newtype final case class UserName (value: String)

  object UserName {
    implicit val jsonCodec = CirceCodec.from[String, UserName](
      UserName(_), _.value
    )
  }

  @derive(eqv, show)
  @newtype final case class Password (value: String)

  object Password {
    implicit val jsonCodec = CirceCodec.from[String, Password](
      Password(_), _.value
    )
  }

  @derive(eqv, show)
  @newtype final case class EncryptedPassword (value: String)

  object EncryptedPassword {
    implicit val jsonCodec = CirceCodec.from[String, EncryptedPassword](
      EncryptedPassword(_), _.value
    )
  }

  @derive(eqv, show)
  final case class User (
    id: UserId,
    name: UserName
  )

  object User {
    implicit val jsonCodec = deriveCodec[User]
  }

  @derive(eqv, show)
  final case class UserWithPassword (
    id: UserId,
    name: UserName,
    password: EncryptedPassword
  )

  object UserWithPassword {
    implicit val jsonCodec = deriveCodec[UserWithPassword]
  }

  @derive(eqv, show)
  final case class CommonUser (value: User)

  object CommonUser {
    implicit val jsonCodec = deriveCodec[CommonUser]
  }

  @derive(eqv, show)
  final case class AdminUser (value: User)

  object AdminUser {
    implicit val jsonCodec = deriveCodec[AdminUser]
  }

  // User registration

  final case class UserNameParam (value: NonEmptyString) {
    def toDomain = UserName(value.value.toLowerCase)
  }

  object UserNameParam {
    implicit val jsonCodec: Codec[UserNameParam] = 
      codecOf[String, NonEmpty].iemap (UserNameParam(_).asRight) (_.value)
  }

  final case class PasswordParam (value: NonEmptyString) {
    def toDomain = Password(value.value.toLowerCase)
  }

  object PasswordParam {
    implicit val jsonCodec: Codec[PasswordParam] =
      codecOf[String, NonEmpty].iemap (PasswordParam(_).asRight) (_.value)
  }

  final case class CreateUser (
    username: UserNameParam,
    password: PasswordParam
  )

  object CreateUser {
    implicit val jsonCodec = deriveCodec[CreateUser]
  }

  final case class UserNotFound(username: UserName)    extends NoStackTrace
  final case class UserNameInUse(username: UserName)   extends NoStackTrace
  final case class InvalidPassword(username: UserName) extends NoStackTrace
  final case object UnsupportedOperation               extends NoStackTrace

  case object TokenNotFound extends NoStackTrace

  // User login 
  final case class LoginUser(
    username: UserNameParam,
    password: PasswordParam
  )

  object LoginUser {
    implicit val jsonCodec = deriveCodec[LoginUser]
  }

  // Admin auth
  @newtype final case class ClaimContent (uuid: UUID)

  object ClaimContent {
    implicit val JsonDecoder: Decoder[ClaimContent] =
      Decoder.forProduct1("uuid")(ClaimContent(_))
  }
}