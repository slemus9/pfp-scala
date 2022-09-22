package shopping.api.routes

import org.http4s.dsl._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.server.AuthMiddleware
import org.http4s.AuthedRoutes

import cats.syntax.all._
import cats.Monad
import shopping.service.OrderService
import shopping.domain.user.CommonUser
import shopping.api.vars.OrderIdVar

object OrderRoutes {

  private[routes] val prefixPath = "/orders"

  private def routes [F[_]: Monad] (
    orders: OrderService[F]
  ): AuthedRoutes[CommonUser, F] = {

    val dsl = Http4sDsl[F]
    import dsl._

    AuthedRoutes.of[CommonUser, F] {

      case GET -> Root as user => 
        Ok(orders.findBy(user.value.id))

      case GET -> Root / OrderIdVar(orderId) as user => 
        Ok(orders.get(user.value.id, orderId))
    }
  }

  def securedRoutes [F[_]: Monad] (
    orders: OrderService[F]
  ) (
    authMiddleware: AuthMiddleware[F, CommonUser]
  ): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(routes(orders))
  )
}