package shopping.api.routes


import org.http4s.dsl._
import org.http4s.circe._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.{HttpRoutes, AuthedRoutes}
import org.http4s.server.{Router, AuthMiddleware}
import shopping.utils.http4s.decoder._

import cats.syntax.all._
import cats.{MonadThrow, Monad}
import shopping.domain.user._
import shopping.service.AuthService
import dev.profunktor.auth.AuthHeaders

object AuthRoutes {

  private [routes] val prefixPath = "/auth"

  private def loginRoutes [F[_]: MonadThrow: JsonDecoder] (
    auth: AuthService[F]
  ): HttpRoutes[F] = {

    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {

      case post @ POST -> Root / "login" => 
        post.decodeR[LoginUser] { user => 
          auth
            .login(user.username.toDomain, user.password.toDomain)
            .flatMap { t => Ok(t.value) }
            .recoverWith {
              case UserNotFound(_) | InvalidPassword(_) =>
                Forbidden() 
            }  
        }
    }
  }

  private def logoutRoutes [F[_]: Monad] (
    auth: AuthService[F]
  ): AuthedRoutes[CommonUser, F] = {
  
    val dsl = Http4sDsl[F]
    import dsl._

    AuthedRoutes.of[CommonUser, F] {
      
      case post @ POST -> Root / "logout" as user =>
        AuthHeaders
          .getBearerToken(post.req)
          .traverse_ { 
            auth.logout(_, user.value.name) 
          } *> NoContent() 
    }
  }

  private def userRoutes [F[_]: MonadThrow: JsonDecoder] (
    auth: AuthService[F]
  ): HttpRoutes[F] = {

    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {

      case post @ POST -> Root / "users" => 
        post.decodeR[CreateUser] { user => 
          auth
            .newUser(
              user.username.toDomain,
              user.password.toDomain
            )
            .flatMap { t => Ok(t.value) }
            .recoverWith {
              case UserNameInUse(username) => 
                Conflict(username.show)
            }
        }
    }
  }

  def router [F[_]: MonadThrow: JsonDecoder] (
    auth: AuthService[F]
  ): HttpRoutes[F] = Router(
    prefixPath -> (loginRoutes(auth) <+> userRoutes(auth))
  )
  

  def securedRouter [F[_]: Monad] (
    auth: AuthService[F]
  ) (
    authMiddleware: AuthMiddleware[F, CommonUser]
  ): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(logoutRoutes(auth))
  )
}