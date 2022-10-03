package shopping.resources

import cats.effect.{Async, Resource}
import org.http4s.HttpApp
import org.http4s.server.Server
import org.http4s.server.defaults.Banner
import org.typelevel.log4cats.Logger
import shopping.config.types.HttpServerConfig
import org.http4s.ember.server.EmberServerBuilder

trait MkHttpServer [F[_]] {

  def newEmber (
    config: HttpServerConfig,
    httpApp: HttpApp[F]
  ): Resource[F, Server]
}

object MkHttpServer {

  def apply [F[_]] (implicit m: MkHttpServer[F]) = m 

  private def showEmberBanner [F[_]: Logger] (s: Server): F[Unit] =
    Logger[F].info(
      s"\n${Banner.mkString("\n")}Http Server started at ${s.address}"
    )

  implicit def forAsyncLogger [F[_]: Async: Logger] = new MkHttpServer[F] {

    def newEmber (
      config: HttpServerConfig, 
      httpApp: HttpApp[F]
    ): Resource[F,Server] = 
      EmberServerBuilder
        .default[F]
        .withHost(config.host)
        .withPort(config.port)
        .withHttpApp(httpApp)
        .build
        .evalTap(showEmberBanner[F])
  }
}