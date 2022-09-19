package examples

import cats.effect._
import cats.effect.std.{Semaphore, Supervisor}

import scala.concurrent.duration._
import scala.util.Random

object Regions extends IOApp.Simple {

  def randomSleep: IO[Unit] =
    IO(Random.nextInt(100)).flatMap { ms =>
      IO.sleep((ms + 700).millis)  
    }.void

  def p1 (sem: Semaphore[IO]): IO[Unit] =
    sem.permit.surround(
      IO.println("Running P1")
    ) >> randomSleep

  def p2 (sem: Semaphore[IO]): IO[Unit] = 
    sem.permit.surround(
      IO.println("Running P2")
    ) >> randomSleep

  def run: IO[Unit] = Supervisor[IO].use { s => 
    Semaphore[IO](1).flatMap { sem => 
      // region of sharing
      // the same semaphore is being shared for p1 and p2
      s.supervise(p1(sem).foreverM).void *>
      s.supervise(p2(sem).foreverM).void *>
      IO.sleep(5.seconds).void  
    }  
  }
}

object LeakyState extends IOApp.Simple {

  import cats.effect.unsafe.implicits.global

  // We no longer have control of where the Semaphore is beign shared
  lazy val sem = Semaphore[IO](1).unsafeRunSync()

  // As an example, this could acquire a single permit and never release it,
  // and we wouldn't be able to track easily this issue
  val doSomethingBad = IO.println("Something Bad!!!")

  def launchMissiles: IO[Unit] = 
    sem.permit.surround(doSomethingBad)

  def p1: IO[Unit] =
    sem.permit.surround(
      IO.println("Running P1")
    ) >> Regions.randomSleep

  def p2: IO[Unit] =
    sem.permit.surround(
      IO.println("Running P2")
    ) >> Regions.randomSleep

  def run: IO[Unit] = Supervisor[IO].use { s => 
    s.supervise(launchMissiles)   *> 
    s.supervise(p1.foreverM).void *> 
    s.supervise(p2.foreverM).void *>
    IO.sleep(5.seconds).void  
  }
    
}