package shopping

import cats.syntax.all._
import cats.effect.{IO, IOApp}
import cats.effect.std.Supervisor
import shopping.config.Config
import dev.profunktor.redis4cats.log4cats._
import eu.timepit.refined.auto._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import shopping.resources.AppResources
import shopping.modules.Security
import shopping.modules.HttpClients
import shopping.modules.Services
import shopping.modules.Programs
import shopping.modules.HttpApi
import shopping.resources.MkHttpServer

object Main extends IOApp.Simple {

  implicit val logger = Slf4jLogger.getLogger[IO]

  def run: IO[Unit] = Config.load[IO].flatMap { config => 
    Logger[IO].info(s"loaded config $config") >> 
    Supervisor[IO].use { implicit sp => 
      AppResources
        .make[IO](config)
        .evalMap { res => 
          Security.make[IO](
            config, res.postgres, res.redis
          )
          .map { security => 
          
            val clients = HttpClients.make[IO](
              config.paymentConfig, res.client
            )

            val services = Services.make[IO](
              res.redis, res.postgres, config.cartExpiration
            )

            val programs = Programs.make[IO](
              config.checkoutConfig, services, clients
            )

            val api = HttpApi.make[IO](
              services, programs, security
            )

            config.httpServerConfig -> api.httpApp
          }
        }
        .flatMap { case (config, httpApp) => 
          MkHttpServer[IO].newEmber(config, httpApp)
        }
        .useForever
    }
  }
}