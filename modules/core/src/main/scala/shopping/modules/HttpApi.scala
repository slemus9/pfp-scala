package shopping.modules

import cats.syntax.all._
import cats.effect.Async
import org.http4s.{HttpRoutes, HttpApp}
import org.http4s.server.Router
import org.http4s.server.middleware.{
  AutoSlash, CORS, Timeout,
  RequestLogger, ResponseLogger
}
import dev.profunktor.auth.JwtAuthMiddleware
import shopping.domain.user._
import shopping.api.routes._
import scala.concurrent.duration._
import org.http4s.server

sealed abstract class HttpApi [F[_]: Async] private (
  val services: Services[F],
  val programs: Programs[F],
  val security: Security[F]
)

object HttpApi {

  def make [F[_]: Async] (
    services: Services[F],
    programs: Programs[F],
    security: Security[F]
  ) = new HttpApi[F](
    services, programs, security
  ) {

    private val adminMiddleware =
      JwtAuthMiddleware[F, AdminUser](
        security.adminJwtAuth.value, security.adminAuth.findUser
      )

    private val usersMiddleware = 
      JwtAuthMiddleware[F, CommonUser](
        security.userJwtAuth.value, security.usersAuth.findUser
      )

    private val openRoutes: HttpRoutes[F] = List(
      AuthRoutes.router(security.auth),
      BrandRoutes.router(services.brands),
      CategoryRoutes.router(services.categories),
      HealthRoutes.router(services.healthCheck),
      ItemRoutes.router(services.items),
    ).reduce(_ <+> _)

    private val securedCommonRoutes: HttpRoutes[F] = List(
      AuthRoutes.securedRouter(security.auth) _,
      CheckoutRoutes.securedRouter(programs.checkout) _,
      OrderRoutes.securedRoutes(services.orders) _,
      ShoppingCartRoutes.securedRouter(services.cart) _
    ).foldMapK { r => r(usersMiddleware) }

    private val securedAdminRoutes: HttpRoutes[F] = List(
      BrandRoutes.adminRouter(services.brands) _,
      ItemRoutes.adminRouter(services.items) _,
    ).foldMapK { r => r(adminMiddleware) }

    private val middleware: HttpRoutes[F] => HttpRoutes[F] = {
      { http: HttpRoutes[F] =>
        AutoSlash(http)
      } andThen { http: HttpRoutes[F] =>
        CORS(http)
      } andThen { http: HttpRoutes[F] =>
        Timeout(60.seconds)(http)
      }
    }

    private val loggers: HttpApp[F] => HttpApp[F] = {
      { http: HttpApp[F] =>
        RequestLogger.httpApp(true, true)(http)
      } andThen { http: HttpApp[F] =>
        ResponseLogger.httpApp(true, true)(http)
      }
    }

    private val routes: HttpRoutes[F] = Router(
      version.v1            -> (openRoutes <+> securedCommonRoutes),
      version.v1 + "/admin" -> securedAdminRoutes
    )

    val httpApp: HttpApp[F] =
      loggers(middleware(routes).orNotFound)
  }

}