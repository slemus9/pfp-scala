package shopping.client

import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers
import cats.syntax.all._
import cats.effect.IO
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.{HttpRoutes, Response}
import eu.timepit.refined.auto._
import shopping.generators._
import shopping.domain.payment.Payment
import shopping.domain.order.{PaymentId, PaymentError}
import shopping.config.types.{PaymentConfig, PaymentURI}

object PaymentClientSuite extends SimpleIOSuite with Checkers {

  val config = PaymentConfig(PaymentURI("http://localhost"))

  def routes (makeResponse: IO[Response[IO]]) = 
    HttpRoutes.of[IO] {
      case POST -> Root / "payments" => makeResponse
    }.orNotFound

  val gen = for {
    id <- paymentIdGen
    p  <- paymentGen
  } yield id -> p

  test ("Response Ok (200)") {
    forall (gen) {
      case (id, payment) => 
        val client = Client.fromHttpApp(routes(Ok(id)))

        PaymentClient
          .make[IO](config, client)
          .process(payment)
          .map(expect.same(id, _))
    }
  }

  test ("Response Conflict (409)") {
    forall (gen) {
      case (id, payment) =>
        val client = Client.fromHttpApp(routes(Conflict(id)))

        PaymentClient
          .make[IO](config, client)
          .process(payment)
          .map(expect.same(id, _))
    }
  }

  test ("Internal Server Error response (500)") {
    forall (paymentGen) { payment => 
      val client = Client.fromHttpApp(routes(InternalServerError()))

      PaymentClient
        .make[IO](config, client)
        .process(payment)
        .attempt
        .map {
          case Left(e)  => expect.same(PaymentError("Internal Server Error"), e)
          
          case Right(_) => failure("expected payment error")
        }  
    }
  }
}