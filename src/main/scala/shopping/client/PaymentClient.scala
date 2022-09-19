package shopping.client

import shopping.domain.payment.Payment
import shopping.domain.order.PaymentId

trait PaymentClient [F[_]] {

  def process (payment: Payment): F[PaymentId]
}