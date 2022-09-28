package examples

import cats.Functor
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.effect.{IO, IOApp, ExitCode, Sync, Ref}

object StateExample extends IOApp {

  trait Counter [F[_]] {
    def incr: F[Unit]
    def get: F[Int]
  }

  object Counter {

    def make [F[_]: Functor : Ref.Make]: F[Counter[F]] =
      Ref.of[F, Int](0).map { ref => 
        new Counter[F] {
          
          def incr: F[Unit] = ref.update(_ + 1)

          def get: F[Int] = ref.get
        }  
      }
  }

  // Alternative definition using a class instead of an anonymous class
  class LiveCounter [F[_]] private (
    ref: Ref[F, Int]
  ) extends Counter[F] {

    def get: F[Int] = ref.get

    def incr: F[Unit] = ref.update(_ + 1)
  }

  object LiveCounter {

    def make [F[_]: Sync]: F[Counter[F]] =
      Ref.of[F, Int](0).map(new LiveCounter[F](_))      
  }

  def program (c: Counter[IO]): IO[Unit] = {

    val getAndPrint = c.get >>= IO.println

    getAndPrint          >>
    c.incr               >>
    getAndPrint          >>
    c.incr.replicateA(5) >>
    getAndPrint
  }

  def run (args: List[String]): IO[ExitCode] = 
    LiveCounter
      .make[IO]
      .flatMap(program)
      .as(ExitCode.Success)
}