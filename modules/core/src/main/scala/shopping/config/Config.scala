package shopping.config

import scala.concurrent.duration._
import types._
import ciris._
import ciris.refined._
import cats.syntax.all._
import cats.effect.Async
import com.comcast.ip4s._
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import shopping.config.AppEnvironment.Test
import shopping.config.AppEnvironment.Prod

object Config {

  private val makeAppConfig = (redisUri: RedisURI, paymentUri: PaymentURI) =>
    ( jwtSecretKey: Secret[JwtSecretKeyConfig]
    , jwtClaim: Secret[JwtClaimConfig]
    , tokenKey: Secret[JwtAccessTokenKeyConfig]
    , adminToken: Secret[AdminUserTokenConfig]
    , salt: Secret[PasswordSalt]
    , dbPassword: Secret[NonEmptyString]
    ) => AppConfig(
      AdminJwtConfig(jwtSecretKey, jwtClaim, adminToken),
      tokenKey,
      salt,
      TokenExpiration(30.minutes),
      ShoppingCartExpiration(30.minutes),
      CheckoutConfig(
        retriesLimit = 3, retriesBackoff = 10.milliseconds
      ),
      PaymentConfig(paymentUri),
      HttpClientConfig(
        timeout = 60.seconds, idleTimeInPool = 30.seconds
      ),
      PostgreSQLConfig(
        host     = "postgres",
        port     = 5432,
        user     = "postgres",
        password = dbPassword,
        database = "store",
        max      = 10        
      ),
      RedisConfig(redisUri),
      HttpServerConfig(
        host = host"0.0.0.0",
        port = port"8080"
      )
    )


  private def default [F[_]] (
    redisUri: RedisURI,
    paymentUri: PaymentURI
  ): ConfigValue[F, AppConfig] =
    ( env("SC_JWT_SECRET_KEY").as[JwtSecretKeyConfig].secret
    , env("SC_JWT_CLAIM").as[JwtClaimConfig].secret
    , env("SC_ACCESS_TOKEN_SECRET_KEY").as[JwtAccessTokenKeyConfig].secret
    , env("SC_ADMIN_USER_TOKEN").as[AdminUserTokenConfig].secret
    , env("SC_PASSWORD_SALT").as[PasswordSalt].secret
    , env("SC_POSTGRES_PASSWORD").as[NonEmptyString].secret     
    ).parMapN(
      makeAppConfig(redisUri, paymentUri)
    )

  def load [F[_]: Async]: F[AppConfig] = 
    env("SC_APP_ENV")
      .as[AppEnvironment]
      .flatMap { 
        case Test => default(
          RedisURI("redis://redis"),
          PaymentURI("https://payments.free.beeceptor.com")
        )
        case Prod => default(
          RedisURI("redis://10.123.154.176"),
          PaymentURI("https://payments.net/api")
        )
      }
      .load[F]
}