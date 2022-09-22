package shopping.domain

import cats.syntax.either._
import io.circe.generic.semiauto.deriveCodec
import io.circe.{KeyDecoder, KeyEncoder, Codec}
import shopping.utils.circe.CirceCodec
import shopping.utils.circe.USDMoneyCodec._
import shopping.utils.circe.refined._
import shopping.domain.brand._
import shopping.domain.category._
import io.estatico.newtype.macros.newtype
import squants.market.{Money, USD}
import java.util.UUID
import scala.util.Try
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.string.{Uuid, ValidBigDecimal}
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.api.Refined

object item {

  @newtype final case class ItemId (value: UUID)

  object ItemId {
    implicit val jsonCodec = CirceCodec.from[UUID, ItemId](
      ItemId(_), _.value
    )

    implicit val jsonKeyEncoder: KeyEncoder[ItemId] = 
      _.value.toString

    implicit val jsonKeyDecoder: KeyDecoder[ItemId] = {
      s => Try(UUID.fromString(s)).toOption.map(ItemId(_))
    }
  }

  @newtype final case class ItemName (value: String)

  object ItemName {
    implicit val jsonCodec = CirceCodec.from[String, ItemName](
      ItemName(_), _.value
    )
  }

  @newtype final case class ItemDescription (value: String)

  object ItemDescription {
    implicit val jsonCodec = CirceCodec.from[String, ItemDescription](
      ItemDescription(_), _.value
    )
  }

  final case class Item (
    uuid: ItemId,
    name: ItemName,
    description: ItemDescription,
    price: Money,
    brand: Brand,
    category: Category
  )

  object Item {
    implicit val jsonCodec = deriveCodec[Item]
  }

  // Create Item
  @newtype final case class ItemNameParam (value: NonEmptyString)

  object ItemNameParam {
    implicit val jsonCodec: Codec[ItemNameParam] = 
      codecOf[String, NonEmpty].iemap (ItemNameParam(_).asRight) (_.value)
  }

  @newtype final case class ItemDescriptionParam (value: NonEmptyString)

  object  ItemDescriptionParam {
    implicit val jsonCodec: Codec[ItemDescriptionParam] =
      codecOf[String, NonEmpty].iemap (ItemDescriptionParam(_).asRight) (_.value)
  }

  @newtype final case class PriceParam (value: String Refined ValidBigDecimal)

  object PriceParam {
    implicit val jsonCodec: Codec[PriceParam] =
      codecOf[String, ValidBigDecimal].iemap (PriceParam(_).asRight) (_.value)
  }

  final case class CreateItemParam (
    name: ItemNameParam,
    description: ItemDescriptionParam,
    price: PriceParam,
    brandId: BrandId,
    categoryId: CategoryId
  ) {

    def toDomain = CreateItem(
      ItemName(name.value.value),
      ItemDescription(description.value.value),
      USD(BigDecimal(price.value.value)),
      brandId,
      categoryId
    )
  }

  object CreateItemParam {
    implicit val jsonCodec = deriveCodec[CreateItemParam]
  }

  final case class CreateItem (
    name: ItemName,
    description: ItemDescription,
    price: Money,
    brandId: BrandId,
    categoryId: CategoryId
  )

  // Update Item

  @newtype final case class ItemIdParam (value: String Refined Uuid)

  object ItemIdParam {
    implicit val jsonCodec: Codec[ItemIdParam] =
      codecOf[String, Uuid].iemap (ItemIdParam(_).asRight) (_.value)
  }

  final case class UpdateItemParam (
    id: ItemIdParam,
    price: PriceParam
  ) {

    def toDomain = UpdateItem(
      ItemId(UUID.fromString(id.value.value)),
      USD(BigDecimal(price.value.value))
    )
  }

  object UpdateItemParam {
    implicit val jsonCodec = deriveCodec[UpdateItemParam]
  }

  final case class UpdateItem (
    id: ItemId,
    price: Money
  )  
}