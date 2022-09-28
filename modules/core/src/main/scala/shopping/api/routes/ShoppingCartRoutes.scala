package shopping.api.routes

import org.http4s.dsl._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.circe._
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.server.AuthMiddleware
import org.http4s.AuthedRoutes

import cats.syntax.all._
import cats.Monad
import shopping.domain.user.CommonUser
import shopping.domain.cart._
import shopping.domain.item._
import shopping.service.ShoppingCartService
import shopping.api.vars.ItemIdVar

object ShoppingCartRoutes {

  private[routes] val prefixPath = "/cart"

  private def routes [F[_]: Monad: JsonDecoder] (
    shoppingCart: ShoppingCartService[F]
  ): AuthedRoutes[CommonUser, F] = {

    val dsl = Http4sDsl[F]
    import dsl._

    AuthedRoutes.of[CommonUser, F] {

      case GET -> Root as user => Ok(
        shoppingCart.get(user.value.id)
      )

      case post @ POST -> Root as user => 

        def addToCart (item: (ItemId, Quantity)) = {
          val (id, quantity) = item
          shoppingCart.add(user.value.id, id, quantity)
        }

        post
          .req.asJsonDecode[Cart]
          .flatMap { 
            _.items.toList.traverse_(addToCart) *> Created()
          }

      case put @ PUT -> Root as user =>
        put
          .req.asJsonDecode[Cart]
          .flatMap { cart => 
            shoppingCart.update(
              user.value.id, cart
            ) *> Ok()
          }
      
      case DELETE -> Root / ItemIdVar(itemId) as user => 
        shoppingCart.removeItem(
          user.value.id, itemId
        ) *> NoContent()
    }
  }

  def securedRouter [F[_]: Monad: JsonDecoder] (
    shoppingCart: ShoppingCartService[F]
  ) (
    authMiddleware: AuthMiddleware[F, CommonUser]
  ): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(routes(shoppingCart))
  )
}