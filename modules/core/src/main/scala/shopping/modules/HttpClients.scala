package shopping.modules

import shopping.client._
import org.http4s.circe.JsonDecoder
import org.http4s.client.Client
import cats.effect.Concurrent
import shopping.config.types.PaymentConfig

sealed trait HttpClients [F[_]] {
  
  def payment: PaymentClient[F]
}

object HttpClients {

  def make [F[_]: Concurrent: JsonDecoder] (
    config: PaymentConfig,
    client: Client[F]
  ) = new HttpClients[F] {

    def payment: PaymentClient[F] = PaymentClient.make(client)
  }
}