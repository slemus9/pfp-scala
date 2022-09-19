package shopping.api

import org.http4s.dsl._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.HttpRoutes
import org.http4s.server.Router
import cats.Monad
import shopping.service.BrandService

object BrandRoutes {

  private[api] val prefixPath = "/brands"

  private def routes [F[_]: Monad] (
    brands: BrandService[F]
  ): HttpRoutes[F] = {
    
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root => Ok(brands.findAll)
    }
  }

  def router [F[_]: Monad] (
    brands: BrandService[F]
  ) = Router[F](
    prefixPath -> routes(brands)
  )
}