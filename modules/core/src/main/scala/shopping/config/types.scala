package shopping.config

import io.estatico.newtype.macros.newtype
import scala.concurrent.duration.FiniteDuration
import derevo.derive
import derevo.cats.show
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString

object types {

  @derive(show)
  @newtype case class AdminUserTokenConfig(secret: NonEmptyString)
  
  @derive(show)
  @newtype case class JwtSecretKeyConfig(secret: NonEmptyString)
  
  @derive(show)
  @newtype case class JwtAccessTokenKeyConfig(secret: NonEmptyString)
  
  @derive(show)
  @newtype case class JwtClaimConfig(secret: NonEmptyString)
  

  @newtype case class TokenExpiration(value: FiniteDuration)

  @derive(show)
  @newtype final case class ShoppingCartExpiration (value: FiniteDuration)

  case class PasswordSalt(secret: NonEmptyString)
}