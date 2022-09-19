package shopping.effect

import scala.concurrent.duration.FiniteDuration
import cats.effect.std.Supervisor
import cats.effect.Temporal
import cats.syntax.apply._
import cats.syntax.functor._

trait Background [F[_]] {

  def schedule [A] (
    fa: F[A], duration: FiniteDuration
  ): F[Unit]
}

object Background {

  def apply [F[_]] (implicit b: Background[F]) = b

  implicit def defaultBgInstance [F[_]] (
    implicit 
      S: Supervisor[F],
      T: Temporal[F]
  ): Background[F] = new Background[F] {
    
    def schedule [A] (
      fa: F[A], duration: FiniteDuration
    ): F[Unit] = 
      S.supervise(
        T.sleep(duration) *> fa
      ).void
  }
}