package shopping.service

import shopping.domain.order._
import shopping.domain.cart._
import shopping.domain.user.UserId
import cats.data.NonEmptyList
import squants.market.Money

trait OrderService [F[_]] {

  def get (userId: UserId, orderId: OrderId): F[Option[Order]]

  def findBy (userId: UserId): F[List[Order]]

  def create (
    userId: UserId,
    paymentId: PaymentId,
    items: NonEmptyList[CartItem],
    total: Money
  ): F[OrderId]
}