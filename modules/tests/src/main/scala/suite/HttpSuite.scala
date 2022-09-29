package suite

import scala.util.control.NoStackTrace
import cats.effect.IO
import cats.implicits._
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import weaver.scalacheck.Checkers
import weaver.{ Expectations, SimpleIOSuite }
import fs2.Stream

trait HttpSuite extends SimpleIOSuite with Checkers {

  def expectHttpBodyAndStatus [A: Encoder] (
    routes: HttpRoutes[IO], req: Request[IO]
  ) (
    expectedBody: A, expectedStatus: Status
  ): IO[Expectations] = routes.run(req).value.flatMap {

    case Some(res) => res.asJson.map { json => 
      expect.same(res.status, expectedStatus) |+|
      expect.same(
        json.deepDropNullValues,
        expectedBody.asJson.dropNullValues
      )
    }

    case None => IO.pure(
      failure("route not found")
    )
  }
  
  def expectStreamedHttpBodyAndStatus [A: Encoder] (
    routes: HttpRoutes[IO], req: Request[IO]
  ) (
    expectedBody: Stream[IO, A], expectedStatus: Status
  ): IO[Expectations] = routes.run(req).value.flatMap {
    case None => IO.pure(failure("route not found"))

    case Some(res) =>
      val receivedJsonBody = res.bodyText.mapFilter {
        parse(_).map(_.deepDropNullValues).toOption
      }
      val expectedJsonBody = expectedBody.map(_.asJson.deepDropNullValues)

      val areSameSize = 
        ( receivedJsonBody.compile.count
        , expectedJsonBody.compile.count
        ).parMapN { (s1, s2) => expect.same(s1, s2)}

      val arePairwiseEq = receivedJsonBody
        .parZipWith (expectedJsonBody) { 
          (j1, j2) => expect.same(j1, j2)
        }
        .compile
        .fold (success) (_ |+| _)

      (areSameSize, arePairwiseEq).parMapN(_ |+| _)
  } 

  def expectHttpStatus (
    routes: HttpRoutes[IO], req: Request[IO]
  ) (expectedStatus: Status): IO[Expectations] =
    routes.run(req).value.map {
      case Some(res) => expect.same(res.status, expectedStatus)
      case None       => failure("route not found")
    }

  def expectHttpFailure (
    routes: HttpRoutes[IO],
    req: Request[IO]
  ): IO[Expectations] = routes.run(req).value.attempt.map {
    case Left(_)  => success
    case Right(_) => failure("expected a failure")
  }

  final case object DummyError extends NoStackTrace
}