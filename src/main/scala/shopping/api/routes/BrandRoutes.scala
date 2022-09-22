package shopping.api.routes

import org.http4s.dsl._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.{HttpRoutes, AuthedRoutes}
import org.http4s.server.{Router, AuthMiddleware}
import shopping.utils.http4s.decoder._

import cats.syntax.all._
import cats.{Monad, MonadThrow}
import shopping.service.BrandService
import shopping.domain.user.AdminUser
import shopping.domain.brand._
import _root_.io.circe.JsonObject
import _root_.io.circe.syntax._

object BrandRoutes {

  private[routes] val prefixPath = "/brands"

  private def routes [F[_]: Monad] (
    brands: BrandService[F]
  ): HttpRoutes[F] = {
    
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root => Ok(brands.findAll)
    }
  }

  private def adminRoutes [F[_]: MonadThrow: JsonDecoder] (
    brands: BrandService[F]
  ): AuthedRoutes[AdminUser, F] = {

    val dsl = Http4sDsl[F]
    import dsl._

    AuthedRoutes.of[AdminUser, F] {

      case post @ POST -> Root as _ => 
        post.req.decodeR[BrandParam] { bp => 
          brands.create(bp.toDomain).flatMap { id => 
            Created(JsonObject.singleton("brand_id", id.asJson))
          }
        }
    }
  }

  def router [F[_]: Monad] (
    brands: BrandService[F]
  ) = Router[F](
    prefixPath -> routes(brands)
  )

  def adminRouter [F[_]: MonadThrow: JsonDecoder] (
    brands: BrandService[F]
  ) (
    authMiddleware: AuthMiddleware[F, AdminUser]
  ): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(adminRoutes(brands))
  )
}