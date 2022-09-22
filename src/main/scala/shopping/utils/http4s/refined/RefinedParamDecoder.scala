package shopping.utils.http4s.refined

import org.http4s.QueryParamDecoder
import eu.timepit.refined.api.{Validate, Refined}
import eu.timepit.refined.refineV
import org.http4s.ParseFailure
import shopping.utils.derevo.Derive

object RefinedParamDecoder {

  implicit def decoder [T: QueryParamDecoder, P] (
    implicit ev: Validate[T, P]
  ): QueryParamDecoder[T Refined P] = QueryParamDecoder[T].emap {
    refineV[P](_).left.map(e => ParseFailure(e, e))
  }
}

object queryParam extends Derive[QueryParamDecoder]