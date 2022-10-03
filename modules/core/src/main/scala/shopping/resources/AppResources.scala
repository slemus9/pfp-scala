package shopping.resources

import cats.syntax.all._
import cats.effect.{Concurrent, Resource}
import cats.effect.std.Console
import natchez.Trace.Implicits.noop
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import skunk.Session
import skunk.implicits._
import skunk.codec.text.text
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import dev.profunktor.redis4cats.effect.MkRedis
import fs2.io.net.Network
import shopping.config.types._
import dev.profunktor.redis4cats.Redis

sealed abstract class AppResources [F[_]] (
  val client: Client[F],
  val postgres: Resource[F, Session[F]],
  val redis: RedisCommands[F, String, String]
)

object AppResources {

  def make [
    F[_]
  : Concurrent
  : Console // read and write from standard console
  : Logger
  : MkHttpClient // capability for building an http client
  : MkRedis
  : Network // Used by skink to create a Postgres connection
  ] (
    config: AppConfig
  ): Resource[F, AppResources[F]] = {
    
    def checkPostgresConnection (
      postgres: Resource[F, Session[F]]
    ): F[Unit] = postgres.use { session => 
      session
        .unique(sql"SELECT version();".query(text))
        .flatMap { v => 
          Logger[F].info(s"Connected to Postgres $v")
        }
    }

    def checkRedisConnection (
      redis: RedisCommands[F, String, String]
    ): F[Unit] = redis.info.flatMap {
      _.get("redis_version").traverse_ { v => 
        Logger[F].info(s"Connected to Redis $v")  
      }
    }

    def mkPostgreSqlResource (
      c: PostgreSQLConfig
    ) = Session.pooled[F](
      host = c.host.value,
      port = c.port.value,
      user = c.user.value,
      password = Some(c.password.value.value),
      database = c.database.value,
      max = c.max.value
    ).evalTap(checkPostgresConnection)

    def mkRedisResource (
      c: RedisConfig
    ): Resource[F, RedisCommands[F, String, String]] =
      Redis[F].utf8(c.uri.value.value).evalTap(
        checkRedisConnection
      )

    ( MkHttpClient[F].newEmber(config.httpClientConfig)
    , mkPostgreSqlResource(config.postgreSQL)
    , mkRedisResource(config.redis)
    ).parMapN(
      new AppResources[F](_, _, _) {}
    )
  }
}