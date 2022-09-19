package shopping.api

import org.http4s.dsl._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.HttpRoutes
import org.http4s.server.Router
import cats.Monad
import shopping.service.CategoryService

object CategoryRoutes {

  private[api] val prefixPath = "/categories"

  private def routes [F[_]: Monad] (
    categories: CategoryService[F]
  ): HttpRoutes[F] = {

    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root => Ok(categories.findAll)
    }
  }

  def router [F[_]: Monad] (
    categories: CategoryService[F]
  ) = Router[F](
    prefixPath -> routes(categories)
  )
}