package shopping.program

import shopping.domain.order._
import shopping.domain.user.UserId
import shopping.domain.checkout.Card
import shopping.domain.payment.Payment
import shopping.domain.cart.CartItem
import shopping.client.PaymentClient
import shopping.service._

import cats.syntax.all._
import cats.data.NonEmptyList
import cats.MonadThrow
import shopping.effect.Retry
import shopping.effect.Retry._
import shopping.effect.Background
import retry.RetryPolicy
import squants.market.Money
import org.typelevel.log4cats.Logger
import scala.concurrent.duration._

final case class Checkout [F[_]: MonadThrow: Retry: Logger: Background] (
  payments: PaymentClient[F],
  cart: ShoppingCartService[F],
  orders: OrderService[F],
  policy: RetryPolicy[F]
) {

  def processPayment (in: Payment): F[PaymentId] =
    Retry[F]
      .retry (policy, Retriable.Payments) (payments.process(in))
      .adaptError { case e => 
        PaymentError(
          Option(e.getMessage).getOrElse("Unknown") // e could be null since it's a java.lang.Throwable
        )
      }

  private def ensureNonEmpty [A] (xs: List[A]): F[NonEmptyList[A]] =
    MonadThrow[F].fromOption(
      NonEmptyList.fromList(xs),
      EmptyCartError
    )
      
  def createOrder (
    userId: UserId,
    paymentId: PaymentId,
    items: NonEmptyList[CartItem],
    total: Money
  ): F[OrderId] = {

    val action = Retry[F]
      .retry(policy, Retriable.Orders) (
        orders.create(userId, paymentId, items, total)
      )
      .adaptError {
        case e => OrderError(e.getMessage)
      }

    def background (fa: F[OrderId]): F[OrderId] = fa.onError {
      case _ => Logger[F].error(
        s"Failed to create order for ${paymentId.show}"
      ) *> Background[F].schedule(background(fa), 1.hour)
    }

    background(action)
  }

  def process (userId: UserId, card: Card): F[OrderId] = 
    for {
      c   <- cart.get(userId)
      its <- ensureNonEmpty(c.items)
      pid <- processPayment(Payment(userId, c.total, card))
      oid <- createOrder(userId, pid, its, c.total)
      _   <- cart.delete(userId).attempt.void
    } yield oid
}