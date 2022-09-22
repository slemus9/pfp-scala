package shopping.utils.derevo

import derevo.Derivation
import derevo.NewTypeDerivation
import scala.annotation.implicitNotFound

trait Derive [F[_]] 
  extends Derivation[F] with NewTypeDerivation[F] {

  @implicitNotFound("Only newtype instances can be derived")
  abstract final class OnlyNewtypes {
    def absurd: Nothing = ???
  }

  def instance (implicit ev: OnlyNewtypes): Nothing = 
    ev.absurd
}