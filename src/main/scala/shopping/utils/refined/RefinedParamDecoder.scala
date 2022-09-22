package shopping.utils.refined

import org.http4s.QueryParamDecoder
import eu.timepit.refined.api.{Validate, Refined}
import eu.timepit.refined.refineV
import org.http4s.ParseFailure

object RefinedParamDecoder {

  implicit def decoder [T: QueryParamDecoder, P] (
    implicit ev: Validate[T, P]
  ): QueryParamDecoder[T Refined P] = QueryParamDecoder[T].emap {
    refineV[P](_).left.map(e => ParseFailure(e, e))
  }
}