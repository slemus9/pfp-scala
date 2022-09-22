package shopping.utils.uuid

import java.util.UUID
import cats.effect.Sync
import cats.ApplicativeThrow

trait GenUUID [F[_]] {

  def make: F[UUID]

  def read (s: String): F[UUID]
}

object GenUUID {

  def apply [F[_]] (implicit g: GenUUID[F]) = g

  implicit def forSync [F[_]: Sync] = new GenUUID[F] {

    def make: F[UUID] = Sync[F].delay(UUID.randomUUID())

    def read (s: String): F[UUID] = 
      ApplicativeThrow[F].catchNonFatal(
        UUID.fromString(s)
      )
  }
}