package shopping.service

import shopping.domain.health._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import cats.syntax.all._
import cats.effect.implicits._
import cats.effect.{Temporal, Resource}
import scala.concurrent.duration._
import dev.profunktor.redis4cats.RedisCommands

trait HealthService [F[_]] {

  def status: F[AppStatus]
}

object HealthService {

  def make [F[_]: Temporal] (
    postgres: Resource[F, Session[F]],
    redis: RedisCommands[F, String, String]
  ) = new HealthService[F] {

    private val q: Query[Void, Int] = 
      sql"SELECT pid FROM pg_stat_activity".query(int4)

    private val redisHealth: F[RedisStatus] = 
      redis.ping
        .map(_.nonEmpty)
        .timeout(1.second)
        .map(Status._Bool.reverseGet)
        .orElse(Status.Unreachable.pure[F].widen)
        .map(RedisStatus(_))

    private val postgresHealth: F[PostgresStatus] = 
      postgres.use(_.execute(q))
        .map(_.nonEmpty)
        .timeout(1.second)
        .map(Status._Bool.reverseGet)
        .orElse(Status.Unreachable.pure[F].widen)
        .map(PostgresStatus(_))

    def status: F[AppStatus] = 
      ( redisHealth
      , postgresHealth
      ).parMapN(AppStatus.apply)
  }
}