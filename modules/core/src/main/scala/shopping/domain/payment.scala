package shopping.domain

import io.circe.generic.semiauto.deriveCodec
import shopping.domain.user.UserId
import shopping.domain.checkout.Card
import squants.market.Money
import shopping.utils.circe.USDMoneyCodec._
import derevo.derive
import derevo.cats.{eqv, show}

object payment {

  @derive(show)
  final case class Payment (
    id: UserId,
    total: Money,
    card: Card,
  )

  object Payment {
    implicit val jsonCodec = deriveCodec[Payment]
  }
}