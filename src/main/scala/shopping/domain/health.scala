package shopping.domain

import monocle.Iso
import io.circe.Encoder
import derevo.derive
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

  @newtype
  final case class RedisStatus (value: Status)

  @newtype
  final case class PostgresStatus (value: Status)

  final case class AppStatus (
    redis: RedisStatus,
    postgres: PostgresStatus
  )
}