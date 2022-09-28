package shopping.sql

import shopping.domain.brand.{BrandId, BrandName}
import shopping.domain.category.{CategoryId, CategoryName}
import shopping.domain.item.{ItemId, ItemName, ItemDescription}
import shopping.domain.order.{OrderId, PaymentId}
import shopping.domain.user.{UserId, UserName, EncryptedPassword}

import skunk.Codec
import skunk.codec.all.{uuid, varchar, numeric}
import squants.market.{Money, USD}

object codecs {

  val brandId: Codec[BrandId] = uuid.imap (BrandId(_)) (_.value)

  val brandName: Codec[BrandName] = varchar.imap (BrandName(_)) (_.value)

  val categoryId: Codec[CategoryId] = uuid.imap (CategoryId(_)) (_.value)

  val categoryName: Codec[CategoryName] = varchar.imap (CategoryName(_)) (_.value)

  val itemId: Codec[ItemId] = uuid.imap (ItemId(_)) (_.value)

  val itemName: Codec[ItemName] = varchar.imap (ItemName(_)) (_.value)

  val itemDescription: Codec[ItemDescription] = 
    varchar.imap (ItemDescription(_)) (_.value)

  val orderId: Codec[OrderId] = uuid.imap (OrderId(_)) (_.value)

  val paymentId: Codec[PaymentId] = uuid.imap (PaymentId(_)) (_.value)

  val userId: Codec[UserId] = uuid.imap (UserId(_)) (_.value)

  val userName: Codec[UserName] = varchar.imap (UserName(_)) (_.value)

  val encryptedPassword: Codec[EncryptedPassword] =
    varchar.imap (EncryptedPassword(_)) (_.value)

  val money: Codec[Money] = numeric.imap (USD(_)) (_.amount)
}