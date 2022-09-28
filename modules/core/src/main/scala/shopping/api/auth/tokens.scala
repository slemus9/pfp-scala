package shopping.api.auth

import shopping.domain.user._
import io.circe.generic.semiauto.deriveCodec
import io.estatico.newtype.macros.newtype
import dev.profunktor.auth.jwt

object tokens {

  @newtype final case class AdminJwtAuth (value: jwt.JwtSymmetricAuth)


  @newtype final case class UserJwtAuth (value: jwt.JwtSymmetricAuth)

}