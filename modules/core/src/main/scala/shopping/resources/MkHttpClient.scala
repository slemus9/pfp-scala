package shopping.resources

import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import shopping.config.types.HttpClientConfig
import cats.effect.{Async, Resource}

trait MkHttpClient [F[_]] {

  def newEmber (c: HttpClientConfig): Resource[F, Client[F]]
}

object MkHttpClient {

  def apply [F[_]] (implicit m: MkHttpClient[F]) = m

  implicit def forAsync [F[_]: Async] = new MkHttpClient[F] {

    def newEmber (c: HttpClientConfig): Resource[F, Client[F]] = 
      EmberClientBuilder
        .default[F]
        .withTimeout(c.timeout)
        .withIdleTimeInPool(c.idleTimeInPool)
        .build
  }
}