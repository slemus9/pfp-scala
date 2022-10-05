package shopping.domain

import io.circe.generic.semiauto.deriveCodec
import io.estatico.newtype.macros.newtype
import java.util.UUID
import shopping.utils.circe.CirceCodec
import derevo.derive
import derevo.cats.{eqv, show}
import shopping.utils.uuid.IsUUID.uuid
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.collection.NonEmpty
import cats.syntax.either._
import io.circe.Decoder
import io.circe.refined._

object category {

  @derive(eqv, show, uuid)
  @newtype final case class CategoryId (value: UUID)

  object CategoryId {
    implicit val jsonCodec = CirceCodec.from[UUID, CategoryId] (
      CategoryId(_), _.value
    )
  }

  @derive(eqv, show)
  @newtype final case class CategoryName (value: String)

  object CategoryName {
    implicit val jsonCodec = CirceCodec.from[String, CategoryName] (
      CategoryName(_), _.value
    )
  }

  @derive(eqv, show)
  final case class Category (uuid: CategoryId, name: CategoryName) 

  object Category {
    implicit val jsonCodec = deriveCodec[Category]
  }

  @newtype final case class CategoryParam (value: NonEmptyString) {

    def toDomain = CategoryName(value.value.toLowerCase.capitalize)
  }

  object CategoryParam {

    implicit val jsonDecoder: Decoder[CategoryParam] = 
      Decoder.forProduct1 ("name") (CategoryParam(_))
  }
}