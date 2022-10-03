package shopping.config

import io.estatico.newtype.macros.newtype
import scala.concurrent.duration.FiniteDuration
import derevo.derive
import derevo.cats.show
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.net.UserPortNumber
import ciris.refined._
import ciris.Secret
import com.comcast.ip4s.{Host, Port}
import shopping.utils.ciris.configDecoder

object types {

  @derive(show, configDecoder)
  @newtype case class AdminUserTokenConfig(secret: NonEmptyString)
  
  @derive(show, configDecoder)
  @newtype case class JwtSecretKeyConfig(secret: NonEmptyString)
  
  @derive(show, configDecoder)
  @newtype case class JwtAccessTokenKeyConfig(secret: NonEmptyString)
  
  @derive(show, configDecoder)
  @newtype case class JwtClaimConfig(secret: NonEmptyString)
  
  @derive(show, configDecoder)
  @newtype case class PasswordSalt(secret: NonEmptyString)
  
  @newtype case class TokenExpiration(value: FiniteDuration)

  @newtype final case class ShoppingCartExpiration (value: FiniteDuration)

  case class CheckoutConfig(
      retriesLimit: PosInt,
      retriesBackoff: FiniteDuration
  )

  case class AppConfig(
      adminJwtConfig: AdminJwtConfig,
      tokenConfig: Secret[JwtAccessTokenKeyConfig],
      passwordSalt: Secret[PasswordSalt],
      tokenExpiration: TokenExpiration,
      cartExpiration: ShoppingCartExpiration,
      checkoutConfig: CheckoutConfig,
      paymentConfig: PaymentConfig,
      httpClientConfig: HttpClientConfig,
      postgreSQL: PostgreSQLConfig,
      redis: RedisConfig,
      httpServerConfig: HttpServerConfig
  )

  case class AdminJwtConfig(
      secretKey: Secret[JwtSecretKeyConfig],
      claimStr: Secret[JwtClaimConfig],
      adminToken: Secret[AdminUserTokenConfig]
  )

  case class PostgreSQLConfig(
      host: NonEmptyString,
      port: UserPortNumber,
      user: NonEmptyString,
      password: Secret[NonEmptyString],
      database: NonEmptyString,
      max: PosInt
  )

  @newtype case class RedisURI(value: NonEmptyString)
  @newtype case class RedisConfig(uri: RedisURI)

  @newtype case class PaymentURI(value: NonEmptyString)
  @newtype case class PaymentConfig(uri: PaymentURI)

  case class HttpServerConfig(
      host: Host,
      port: Port
  )

  case class HttpClientConfig(
      timeout: FiniteDuration,
      idleTimeInPool: FiniteDuration
  )
}