package shopping.api.routes

import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.{HttpRoutes, AuthedRoutes}
import org.http4s.server.{Router, AuthMiddleware}
import shopping.utils.http4s.decoder._
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher

import cats.syntax.all._
import cats.{Monad, MonadThrow}
import shopping.domain.brand._
import shopping.domain.item._
import shopping.service.ItemService
import shopping.domain.user.AdminUser
import _root_.io.circe.JsonObject
import _root_.io.circe.syntax._

object ItemRoutes {

  private[routes] val prefixPath = "/items"

  object BrandQueryParam 
    extends OptionalQueryParamDecoderMatcher[BrandParam]("brand")

  private def routes [F[_]: Monad] (
    items: ItemService[F]
  ): HttpRoutes[F] = {

    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root :? BrandQueryParam(maybeBrand) => Ok(
        maybeBrand.fold (items.findAll) { b => items.findBy(b.toDomain) }
      )
    }
  }

  private def adminRoutes [F[_]: MonadThrow: JsonDecoder] (
    items: ItemService[F]
  ): AuthedRoutes[AdminUser, F] = {

    val dsl = Http4sDsl[F]
    import dsl._

    AuthedRoutes.of[AdminUser, F] {

      case post @ POST -> Root as _ => 
        post.req.decodeR[CreateItemParam] { item => 
          items.create(item.toDomain).flatMap { id => 
            Created(JsonObject.singleton("item_id", id.asJson))  
          }  
        }

      case put @ PUT -> Root as _ => 
        put.req.decodeR[UpdateItemParam] { item => 
          items.update(item.toDomain) *> Ok()  
        }
    }
  }

  def router [F[_]: Monad] (
    items: ItemService[F]
  ): HttpRoutes[F] = Router(
    prefixPath -> routes(items)
  )

  def adminRouter [F[_]: MonadThrow: JsonDecoder] (
    items: ItemService[F]
  ) (
    authMiddleware: AuthMiddleware[F, AdminUser]
  ): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(adminRoutes(items))
  )
}