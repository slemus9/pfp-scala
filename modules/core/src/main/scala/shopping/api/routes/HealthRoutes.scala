package shopping.api.routes

import org.http4s.dsl._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.HttpRoutes
import org.http4s.server.Router

import cats.Monad

import shopping.domain.health._
import shopping.service.HealthService

object HealthRoutes {

  private[routes] val prefixPath = "/healthcheck"

  def router [F[_]: Monad] (
    healthCheck: HealthService[F]
  ): HttpRoutes[F] = {

    val dsl = Http4sDsl[F]
    import dsl._

    val routes = HttpRoutes.of[F] {
      case GET -> Root => Ok(healthCheck.status)
    }

    Router(
      prefixPath -> routes
    )
  }
}