package shopping.domain

import io.circe.generic.semiauto._
import io.circe.{Encoder, Decoder}
import shopping.utils.circe.CirceCodec
import shopping.utils.circe.USDMoneyCodec._
import shopping.domain.item._
import io.estatico.newtype.macros.newtype
import squants.market.{Money, USD}
import scala.util.control.NoStackTrace
import derevo.derive
import derevo.cats.{eqv, show}

object cart {

  @derive(eqv, show)
  @newtype final case class Quantity (value: Int)

  object Quantity {
    implicit val jsonCodec = CirceCodec.from[Int, Quantity](
      Quantity(_), _.value
    )
  }

  @derive(eqv, show)
  @newtype final case class Cart (items: Map[ItemId, Quantity])

  object Cart {
    implicit val jsonEncoder: Encoder[Cart] =
      Encoder.forProduct1("items")(_.items)

    implicit val jsonDecoder: Decoder[Cart] =
      Decoder.forProduct1("items")(Cart(_))
  }

  @derive(eqv, show)
  final case class CartItem (item: Item, quantity: Quantity) {
    def subTotal: Money = USD(
      item.price.amount * quantity.value
    )
  }

  object CartItem {
    implicit val jsonCodec = deriveCodec[CartItem]
  }

  @derive(eqv, show)
  final case class CartTotal (items: List[CartItem], total: Money)

  object CartTotal {
    implicit val jsonCodec = deriveCodec[CartTotal]
  }

  // Errors
  final case class CartNotFound (userId: user.UserId) extends NoStackTrace
}