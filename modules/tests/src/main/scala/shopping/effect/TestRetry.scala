package shopping.effect

import cats.effect.{IO, Ref}
import retry._
import scala.annotation.nowarn

object TestRetry {

  private def handleFor [A <: RetryDetails] (
    ref: Ref[IO, Option[A]]
  ) = new Retry[IO] {

    def retry[B] (
      policy: RetryPolicy[IO], 
      retriable: Retry.Retriable
    ) (fa: IO[B]): IO[B] = {

      @nowarn
      def onError (e: Throwable, details: RetryDetails): IO[Unit] = 
        details match {
          case a: A => ref.set(Some(a))
          case _    => IO.unit
        }

      retryingOnAllErrors (policy, onError) (fa)
    }
  }

  def givingUp (
    ref: Ref[IO, Option[RetryDetails.GivingUp]]
  ): Retry[IO] = handleFor[RetryDetails.GivingUp](ref)

  def recovering (
    ref: Ref[IO, Option[RetryDetails.WillDelayAndRetry]]
  ): Retry[IO] = handleFor[RetryDetails.WillDelayAndRetry](ref)
}