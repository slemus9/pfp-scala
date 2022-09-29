package shopping.domain

import io.circe.generic.semiauto.deriveCodec
import shopping.utils.circe.CirceCodec
import shopping.utils.circe.USDMoneyCodec._
import shopping.domain.cart._
import shopping.domain.item._
import java.util.UUID
import io.estatico.newtype.macros.newtype
import squants.market.Money
import scala.util.control.NoStackTrace
import io.circe.Encoder
import derevo.derive
import derevo.cats.{eqv, show}
import shopping.utils.uuid.IsUUID.uuid

object order {

  @derive(eqv, show, uuid)
  @newtype final case class OrderId (value: UUID)

  object OrderId {
    implicit val jsonCodec = CirceCodec.from[UUID, OrderId](
      OrderId(_), _.value
    )
  }

  @derive(eqv, show, uuid)
  @newtype 
  final case class PaymentId (value: UUID)

  object PaymentId {
    implicit val jsonCodec = CirceCodec.from[UUID, PaymentId](
      PaymentId(_), _.value
    )
  }

  @derive(eqv, show)
  final case class Order (
    id: OrderId,
    pid: PaymentId,
    items: Map[ItemId, Quantity],
    total: Money
  )

  object Order {
    implicit val jsonCodec = deriveCodec[Order]
  }

  // Errors
  @derive(eqv, show)
  case object EmptyCartError extends NoStackTrace

  @derive(eqv, show)
  sealed trait OrderOrPaymentError extends NoStackTrace

  final case class PaymentError (message: String) extends OrderOrPaymentError

  final case class OrderError (message: String) extends OrderOrPaymentError
}