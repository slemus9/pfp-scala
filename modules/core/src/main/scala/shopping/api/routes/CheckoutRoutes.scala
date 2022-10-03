package shopping.api.routes

import org.http4s.dsl._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.server.AuthMiddleware
import org.http4s.AuthedRoutes
import shopping.utils.http4s.decoder._

import cats.syntax.all._
import cats.MonadThrow
import shopping.domain.checkout._
import shopping.domain.cart._
import shopping.domain.user.CommonUser
import shopping.program.Checkout
import shopping.domain.order.{EmptyCartError, OrderOrPaymentError}

object CheckoutRoutes {

  private[routes] val prefixPath = "/checkout"

  private def routes [F[_]: MonadThrow: JsonDecoder] (
    checkout: Checkout[F]
  ): AuthedRoutes[CommonUser, F] = {

    val dsl = Http4sDsl[F]
    import dsl._

    AuthedRoutes.of[CommonUser, F] {

      case post @ POST -> Root as user =>
        post.req.decodeR[Card] { card => 
          checkout
            .process(user.value.id, card)
            .flatMap(Created(_))
            .recoverWith {
              case CartNotFound(userId) => NotFound(
                s"Cart not found for user: ${userId.value}"
              )

              case EmptyCartError => 
                BadRequest("Shopping cart is empty!")

              case e: OrderOrPaymentError => 
                BadRequest(e.show)
            }  
        }
    }
  }

  def securedRouter [F[_]: MonadThrow: JsonDecoder] (
    checkout: Checkout[F]
  ) (
    authMiddleware: AuthMiddleware[F, CommonUser]
  ): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(routes(checkout))
  )
}