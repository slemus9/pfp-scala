package shopping.api

import java.util.UUID
import shopping.domain.item.ItemId
import shopping.domain.order.OrderId
import scala.util.Try

import cats.implicits._

object vars {

  protected class UUIDVar [A] (f: UUID => A) {

    def unapply (s: String): Option[A] =
      Either.catchNonFatal {
        f(UUID.fromString(s))
      }.toOption
  }

  object ItemIdVar extends UUIDVar(ItemId(_))

  object OrderIdVar extends UUIDVar(OrderId(_))
}