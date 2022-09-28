package shopping.domain

import cats.syntax.functor._
import cats.Functor
import shopping.utils.uuid.{GenUUID, IsUUID}

object id {

  object ID {

    def make [ 
      F[_]: Functor: GenUUID,
      A: IsUUID
    ]: F[A] =
      GenUUID[F].make.map(
        IsUUID[A]._UUID.get
      )

    def read [ 
      F[_]: Functor: GenUUID,
      A: IsUUID
    ] (s: String): F[A] =
      GenUUID[F].read(s).map(
        IsUUID[A]._UUID.get
      )
  }
}