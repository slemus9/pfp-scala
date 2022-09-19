package shopping.domain

import monocle.Iso
import io.circe.Encoder
import derevo.derive
import derevo.circe.magnolia.encoder
import io.estatico.newtype.macros.newtype

object health {

  sealed trait Status
  object Status {

    case object Okay extends Status
    case object Unreachable extends Status

    val _Bool = Iso[Status, Boolean] {
      case Okay        => true
      case Unreachable => false
    } {
      if (_) Okay else Unreachable
    }

    implicit val jsonEncoder: Encoder[Status] =
      Encoder.forProduct1("status")(_.toString)
  }

  @derive(encoder)
  @newtype
  final case class RedisStatus (value: Status)

  @derive(encoder)
  @newtype
  final case class PostgresStatus (value: Status)

  @derive(encoder)
  final case class AppStatus (
    redis: RedisStatus,
    postgres: PostgresStatus
  )
}