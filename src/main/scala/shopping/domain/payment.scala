package shopping.domain

import shopping.domain.user.UserId
import shopping.domain.checkout.Card
import squants.market.Money

object payment {

  final case class Payment (
    id: UserId,
    total: Money,
    card: Card,
  )
}