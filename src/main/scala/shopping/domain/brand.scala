package shopping.domain

import io.circe.generic.semiauto.deriveCodec
import io.estatico.newtype.macros.newtype
import shopping.utils.circe.CirceCodec
import java.util.UUID

object brand {

  @newtype final case class BrandId (value: UUID)

  object BrandId {
    implicit val jsonCodec = CirceCodec.from[UUID, BrandId](
      BrandId(_), _.value 
    )
  }

  @newtype final case class BrandName (value: String)

  object BrandName{
    implicit val jsonCodec = CirceCodec.from[String, BrandName](
      BrandName(_), _.value 
    )
  }

  final case class Brand (uuid: BrandId, name: BrandName)

  object Brand {
    implicit val jsonCodec = deriveCodec[Brand]
  }
}