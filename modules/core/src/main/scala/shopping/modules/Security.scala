package shopping.modules

import cats.syntax.all._
import cats.ApplicativeThrow
import cats.effect.{Sync, Resource}
import io.circe.parser.{ decode => jsonDecode }
import skunk.Session
import dev.profunktor.redis4cats.RedisCommands
import dev.profunktor.auth.jwt._
import pdi.jwt.JwtAlgorithm
import shopping.service.{AuthService, UserAuthService}
import shopping.api.auth.tokens.{AdminJwtAuth, UserJwtAuth}
import shopping.config.types.AppConfig
import shopping.domain.user._
import shopping.auth._
import shopping.service.UserService

sealed abstract class Security [F[_]] private (
  val auth: AuthService[F],
  val adminAuth: UserAuthService[F, AdminUser],
  val usersAuth: UserAuthService[F, CommonUser],
  val adminJwtAuth: AdminJwtAuth,
  val userJwtAuth: UserJwtAuth
)

object Security {

  def make [F[_]: Sync] (
    config: AppConfig,
    postgres: Resource[F, Session[F]],
    redis: RedisCommands[F, String, String]
  ): F[Security[F]] = {

    val adminJwtAuth = AdminJwtAuth(
      JwtAuth.hmac(
        config.adminJwtConfig.secretKey.value.secret.value,
        JwtAlgorithm.HS256
      )
    )

    val userJwtAuth = UserJwtAuth(
      JwtAuth.hmac(
        config.tokenConfig.value.secret.value,
        JwtAlgorithm.HS256
      )
    )

    val adminToken = JwtToken(
      config.adminJwtConfig.adminToken.value.secret.value
    )

    for {
      adminClaim <- jwtDecode(adminToken, adminJwtAuth.value)
      content    <- ApplicativeThrow[F].fromEither(
        jsonDecode[ClaimContent](adminClaim.content)
      )
      adminUser  =  AdminUser(
        User(UserId(content.uuid), UserName("admin"))
      )
      tokens     <- JwtExpire.make.map(
        Tokens.make(_, config.tokenConfig.value, config.tokenExpiration)
      )
      val users  =  UserService.make(postgres)
      crypto     <- Crypto.make(config.passwordSalt.value)
      auth       =  AuthService.make[F](
        config.tokenExpiration, tokens, users, redis, crypto
      )
      adminAuth  =  UserAuthService.admin(adminToken, adminUser)
      usersAuth  = UserAuthService.common(redis)
    } yield new Security[F](
      auth, adminAuth, usersAuth, adminJwtAuth, userJwtAuth
    ) {}
  }
}