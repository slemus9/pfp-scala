package shopping.client

import cats.syntax.all._
import cats.effect.{MonadCancelThrow, Concurrent}
import shopping.domain.payment.Payment
import shopping.domain.order.{PaymentId, PaymentError}
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.Client
import org.http4s.circe._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.Uri
import org.http4s.Method
import org.http4s.Status
import shopping.config.types.PaymentConfig
import com.ongres.scram.common.bouncycastle.base64.Encoder

trait PaymentClient [F[_]] {

  def process (payment: Payment): F[PaymentId]
}

object PaymentClient {

  // TODO: add configuration parameter
  def make [F[_]: Concurrent: JsonDecoder] (
    config: PaymentConfig,
    client: Client[F]
  ): PaymentClient[F] = new PaymentClient[F] {
  
    val dsl = Http4sClientDsl[F]
    import dsl._

    val baseUri = config.uri.value.value

    implicit val entityDecoder = jsonDecoder

    def process (payment: Payment): F[PaymentId] = 
      Uri
        .fromString(baseUri + "/payments")
        .liftTo[F]
        .flatMap { uri => 
          val query = Method.POST(payment, uri)
          client.run(query).use { res => 
            res.status match {
              case Status.Ok | Status.Conflict => 
                res.asJsonDecode[PaymentId]
              
              case status => 
                PaymentError(
                  Option.when 
                    (!status.reason.isBlank) 
                    (status.reason) 
                    .getOrElse("unknown")
                ).raiseError
            }
          }
        }
  } 
}