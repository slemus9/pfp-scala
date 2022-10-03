package shopping.modules

import cats.syntax.semigroup._
import cats.effect.Temporal
import org.typelevel.log4cats.Logger
import shopping.effect.Background
import shopping.config.types.CheckoutConfig
import retry.{RetryPolicy, RetryPolicies}
import shopping.program.Checkout

sealed abstract class Programs [
  F[_]: Background: Logger: Temporal
] private (
  config: CheckoutConfig,
  services: Services[F],
  clients: HttpClients[F]
) {

  val retryPolicy: RetryPolicy[F] = 
    RetryPolicies.limitRetries(
      config.retriesLimit.value
    ) |+| RetryPolicies.exponentialBackoff(
      config.retriesBackoff
    )

  val checkout = Checkout[F](
    clients.payment,
    services.cart,
    services.orders,
    retryPolicy
  )
}

object Programs {

  def make [F[_]: Background: Logger: Temporal] (
    checkoutConfig: CheckoutConfig,
    services: Services[F],
    clients: HttpClients[F]
  ) = new Programs[F](checkoutConfig, services, clients) {}
}