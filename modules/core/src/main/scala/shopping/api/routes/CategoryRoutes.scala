package shopping.api.routes

import org.http4s.dsl._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.{HttpRoutes, AuthedRoutes}
import org.http4s.server.{Router, AuthMiddleware}
import shopping.utils.http4s.decoder._

import cats.syntax.all._
import cats.{Monad, MonadThrow}
import shopping.domain.category._
import shopping.domain.user.AdminUser
import shopping.service.CategoryService
import shopping.utils.http4s.decoder._
import _root_.io.circe.JsonObject
import _root_.io.circe.syntax._

object CategoryRoutes {

  private[routes] val prefixPath = "/categories"

  private def routes [F[_]: Monad] (
    categories: CategoryService[F]
  ): HttpRoutes[F] = {

    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root => Ok(categories.findAll)
    }
  }

  private def adminRoutes [F[_]: MonadThrow: JsonDecoder] (
    categories: CategoryService[F]
  ): AuthedRoutes[AdminUser, F] = {

    val dsl = Http4sDsl[F]
    import dsl._

    AuthedRoutes.of[AdminUser, F] {
      case post @ POST -> Root as _ =>
        post.req.decodeR[CategoryParam] { cp => 
          categories.create(cp.toDomain).flatMap { id => 
            Created(JsonObject.singleton("category_id", id.asJson))  
          }
        }
    }
  }

  def router [F[_]: Monad] (
    categories: CategoryService[F]
  ) = Router[F](
    prefixPath -> routes(categories)
  )

  def adminRouter [F[_]: MonadThrow: JsonDecoder] (
    categories: CategoryService[F]
  ) (
    authMiddleware: AuthMiddleware[F, AdminUser]
  ) = Router(
    prefixPath -> authMiddleware(adminRoutes(categories))
  )
}