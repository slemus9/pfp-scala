package shopping.config

import io.estatico.newtype.macros.newtype
import scala.concurrent.duration.FiniteDuration
import derevo.derive
import derevo.cats.show

object types {

  @newtype case class TokenExpiration(value: FiniteDuration)

  @derive(show)
  @newtype final case class ShoppingCartExpiration (value: FiniteDuration)
}