import cats.effect.IOApp
import cats.effect.{ExitCode, IO}

object Main extends IOApp {

  def run (args: List[String]): IO[ExitCode] = 
    IO.println("core module") as ExitCode.Success
}