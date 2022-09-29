package shopping.api.routes

import shopping.domain.user._
import shopping.domain.cart._
import shopping.domain.item._
import shopping.generators._
import shopping.api.auth.tokens._
import shopping.service.ShoppingCartService

import cats.data.Kleisli
import cats.effect._
import org.http4s.Method._
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.dsl.io._
import org.http4s.server.AuthMiddleware
import org.http4s.syntax.literals._
import squants.market.USD
import suite.HttpSuite

object ShoppingCartRoutesSuite extends HttpSuite {

  def authMiddleware (
    authUser: CommonUser
  ): AuthMiddleware[IO, CommonUser] =
    AuthMiddleware(Kleisli.pure(authUser))

  def dataCart(cartTotal: CartTotal) = new TestShoppingCartService {

    override def get (userId: UserId): IO[CartTotal] =
      IO.pure(cartTotal)
  }

  test ("GET shopping cart succeeds") {
    val gen = for {
      user      <- commonUserGen
      cartTotal <- cartTotalGen
    } yield user -> cartTotal

    forall (gen) { case (user, cartTotal) => 
      val req = GET(uri"/cart")
      val routes = ShoppingCartRoutes.securedRouter[IO](
        dataCart(cartTotal)
      )(
        authMiddleware(user)
      )

      expectHttpBodyAndStatus (routes, req) (cartTotal, Status.Ok)
    }
  }

  test ("POST add item to shopping cart succeeds") {
    val gen = for {
      user <- commonUserGen
      cart <- cartGen
    } yield user -> cart

    forall (gen) { case (user, cart) => 
      val req = POST(cart, uri"/cart")
      val routes = ShoppingCartRoutes.securedRouter[IO](
        new TestShoppingCartService
      ) (
        authMiddleware(user)
      )

      expectHttpStatus (routes, req) (Status.Created)
    }
  }
} 

protected class TestShoppingCartService extends ShoppingCartService[IO] {

  def add (userId: UserId, itemId: ItemId, quantity: Quantity): IO[Unit] = IO.unit
  
  def get (userId: UserId): IO[CartTotal] =
    IO.pure(CartTotal(List.empty, USD(0)))
  
  def delete (userId: UserId): IO[Unit]                     = IO.unit
  
  def removeItem (userId: UserId, itemId: ItemId): IO[Unit] = IO.unit
  
  def update(userId: UserId, cart: Cart): IO[Unit]         = IO.unit
}
