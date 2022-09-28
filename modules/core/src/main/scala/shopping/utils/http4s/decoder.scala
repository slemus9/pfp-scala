package shopping.utils.http4s

import org.http4s._
import org.http4s.circe._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import cats.syntax.all._
import cats.MonadThrow
import io.circe.Decoder

object decoder {

  /*
    Custom decoding function that deals with validation errors from the Refined
    library
  */
  implicit class RefinedRequestDecoder [F[_]: MonadThrow: JsonDecoder] (
    req: Request[F]
  ) {
    
    private val dsl = Http4sDsl[F]
    import dsl._
    
    def decodeR [A: Decoder] (f: A => F[Response[F]]): F[Response[F]] =
      req.asJsonDecode[A].attempt.flatMap {

        case Left(e) => Option(e.getCause) match {

          case Some(c) if c.getMessage.startsWith("Predicate") => 
            BadRequest(c.getMessage)

          case _ => UnprocessableEntity()
        }

        case Right(a) => f(a)  
      }
  }
}