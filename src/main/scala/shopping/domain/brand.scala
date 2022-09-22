package shopping.domain

import cats.syntax.either._
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Decoder, Codec}
import io.estatico.newtype.macros.newtype
import shopping.utils.circe.CirceCodec
import shopping.utils.circe.refined.codecOf
import java.util.UUID
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.collection.NonEmpty
import org.http4s.QueryParamDecoder
import org.http4s.ParseFailure
import derevo.derive
import derevo.cats.{eqv, show}
import shopping.utils.uuid.IsUUID.uuid

object brand {

  @derive(eqv, show, uuid)
  @newtype final case class BrandId (value: UUID)

  object BrandId {
    implicit val jsonCodec = CirceCodec.from[UUID, BrandId](
      BrandId(_), _.value 
    )
  }

  @derive(eqv, show)
  @newtype final case class BrandName (value: String)

  object BrandName{
    implicit val jsonCodec = CirceCodec.from[String, BrandName](
      BrandName(_), _.value 
    )
  }

  @derive(eqv, show)
  final case class Brand (uuid: BrandId, name: BrandName)

  object Brand {
    implicit val jsonCodec = deriveCodec[Brand]
  }

  // Query params
  @newtype final case class BrandParam (value: NonEmptyString) {

    def toDomain = BrandName(value.value.toLowerCase.capitalize)
  }

  object BrandParam {
    
    implicit val jsonCodec: Codec[BrandParam] = 
      codecOf[String, NonEmpty].iemap (BrandParam(_).asRight) (_.value)

    // TODO: Replace with automatic derivation
    implicit val paramDecoder: QueryParamDecoder[BrandParam] =
      QueryParamDecoder[String].emap { 
        NonEmptyString.from(_).bimap(
          e => ParseFailure(e, e),
          BrandParam(_)
        )
      }
  }
}