package shopping.domain

import io.circe.generic.semiauto._
import io.circe.{Encoder, Decoder}
import shopping.utils.circe.CirceCodec
import shopping.utils.circe.USDMoneyCodec._
import shopping.domain.item._
import io.estatico.newtype.macros.newtype
import squants.market.Money
import scala.util.control.NoStackTrace

object cart {

  @newtype final case class Quantity (value: Int)

  object Quantity {
    implicit val jsonCodec = CirceCodec.from[Int, Quantity](
      Quantity(_), _.value
    )
  }

  @newtype final case class Cart (items: Map[ItemId, Quantity])

  object Cart {
    implicit val jsonEncoder: Encoder[Cart] =
      Encoder.forProduct1("items")(_.items)

    implicit val jsonDecoder: Decoder[Cart] =
      Decoder.forProduct1("items")(Cart(_))
  }

  final case class CartItem (item: Item, quantity: Quantity)

  object CartItem {
    implicit val jsonCodec = deriveCodec[CartItem]
  }

  final case class CartTotal (items: List[CartItem], total: Money)

  object CartTotal {
    implicit val jsonCodec = deriveCodec[CartTotal]
  }

  // Errors
  final case class CartNotFound (userId: user.UserId) extends NoStackTrace
}