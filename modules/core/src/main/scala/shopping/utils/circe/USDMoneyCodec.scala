package shopping.utils.circe

import squants.market._

object USDMoneyCodec {

  implicit val bigDecimalCodec = CirceCodec.from[BigDecimal, Money](
    USD(_), _.amount
  )
}