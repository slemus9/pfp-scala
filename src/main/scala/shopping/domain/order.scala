package shopping.domain

import shopping.domain.cart._
import shopping.domain.item._
import java.util.UUID
import io.estatico.newtype.macros.newtype
import squants.market.Money
import scala.util.control.NoStackTrace
import derevo.derive
import derevo.cats.show

object order {

  @newtype final case class OrderId (uuid: UUID)

  @derive(show)
  @newtype 
  final case class PaymentId (uuid: UUID)

  final case class Order (
    id: OrderId,
    pid: PaymentId,
    items: Map[ItemId, Quantity],
    total: Money
  )

  // Errors
  case object EmptyCartError extends NoStackTrace

  final case class PaymentError (message: String) extends NoStackTrace

  final case class OrderError (message: String) extends NoStackTrace
}