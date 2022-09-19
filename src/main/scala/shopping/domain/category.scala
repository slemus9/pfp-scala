package shopping.domain

import io.circe.generic.semiauto.deriveCodec
import io.estatico.newtype.macros.newtype
import java.util.UUID
import shopping.utils.circe.CirceCodec

object category {

  @newtype final case class CategoryId (value: UUID)

  object CategoryId {
    implicit val jsonCodec = CirceCodec.from[UUID, CategoryId] (
      CategoryId(_), _.value
    )
  }

  @newtype final case class CategoryName (value: String)

  object CategoryName {
    implicit val jsonCodec = CirceCodec.from[String, CategoryName] (
      CategoryName(_), _.value
    )
  }

  final case class Category (uuid: CategoryId, name: CategoryName) 

  object Category {
    implicit val jsonEncoder = deriveCodec[Category]
  }
}