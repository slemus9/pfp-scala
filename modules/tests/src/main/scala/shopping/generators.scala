package shopping

import org.scalacheck.Gen
import java.util.UUID
import squants.market.{Money, USD}
import eu.timepit.refined.api.Refined
import shopping.domain.brand._
import shopping.domain.category._
import shopping.domain.item._
import shopping.domain.cart._
import shopping.domain.order._
import shopping.domain.payment._
import shopping.domain.checkout._
import shopping.domain.user._

object generators {

  val nonEmptyStringGen: Gen[String] = 
    Gen
      .chooseNum(21, 40)
      .flatMap {
        Gen.buildableOfN[String, Char](_, Gen.alphaChar)
      }

  def nesGen [A] (f: String => A): Gen[A] =
    nonEmptyStringGen.map(f)

  def idGen [A] (f: UUID => A): Gen[A] =
    Gen.uuid.map(f)
  
  // Brand generators

  val brandIdGen: Gen[BrandId] = 
    idGen(BrandId(_))

  val brandNameGen: Gen[BrandName] =
    nesGen(BrandName(_))

  val brandGen: Gen[Brand] =
    for {
      id   <- brandIdGen
      name <- brandNameGen
    } yield Brand(id, name)

  // Category generators

  val categoryIdGen: Gen[CategoryId] = 
    idGen(CategoryId(_))

  val categoryNameGen: Gen[CategoryName] =
    nesGen(CategoryName(_))

  val categoryGen: Gen[Category] =
    for {
      id   <- categoryIdGen
      name <- categoryNameGen
    } yield Category(id, name)

  // Money generators

  val moneyGen: Gen[Money] =
    Gen.posNum[Long].map { n =>
      USD(BigDecimal(n))
    }

  // Item generators

  val itemIdGen: Gen[ItemId] =
    idGen(ItemId(_))

  val itemNameGen: Gen[ItemName] =
    nesGen(ItemName(_))

  val itemDescriptionGen: Gen[ItemDescription] =
    nesGen(ItemDescription(_))

  val itemGen: Gen[Item] =
    for {
      id    <- itemIdGen
      name  <- itemNameGen
      desc  <- itemDescriptionGen
      price <- moneyGen
      brand <- brandGen
      cat   <- categoryGen
    } yield Item(id, name, desc, price, brand, cat)

  // Cart generators

  val quatityGen: Gen[Quantity] =
    Gen.posNum[Int].map(Quantity(_))

  val cartItemGen: Gen[CartItem] =
    for {
      item     <- itemGen
      quantity <- quatityGen
    } yield CartItem(item, quantity)

  val cartTotalGen: Gen[CartTotal] =
    for {
      items <- Gen.nonEmptyListOf(cartItemGen)
      total <- moneyGen
    } yield CartTotal(items, total)

  val itemMapGen: Gen[(ItemId, Quantity)] =
    for {
      id <- itemIdGen
      q  <- quatityGen
    } yield id -> q

  val cartGen: Gen[Cart] =
    Gen.nonEmptyMap(itemMapGen).map(Cart(_))

  // Card generators

  val cardNameGen: Gen[CardName] = 
    Gen.stringOf(
      Gen.oneOf(('a' to 'z') ++ ('A' to 'Z'))
    ).map { s => 
      CardName(Refined.unsafeApply(s))  
    }

  private def sized (size: Int): Gen[Long] = {
    def go (acc: String, s: Int): Gen[Long] =
      Gen.oneOf(1 to 9).flatMap { n => 
        if (s == size) acc.toLong
        else go(s"$acc$n", s + 1)  
      }

    go("", 0)
  }

  val cardGen: Gen[Card] = 
    for {
      name   <- cardNameGen
      number <- sized(16).map { n => CardNumber(Refined.unsafeApply(n)) }
      exp    <- sized(4).map { n => CardExpiration(Refined.unsafeApply(n.toString)) }
      cvv    <- sized(3).map { n => CardCVV(Refined.unsafeApply(n.toInt)) }
    } yield Card(name, number, exp, cvv)

  // User genarators

  val userIdGen: Gen[UserId] =
    idGen(UserId(_))

  val userNameGen: Gen[UserName] =
    nesGen(UserName(_))

  val userGen: Gen[User] =
    for {
      id   <- userIdGen
      name <- userNameGen
    } yield User(id, name)

  val commonUserGen: Gen[CommonUser] =
    userGen.map(CommonUser(_))

  val passwordGen: Gen[Password] =
    nesGen(Password.apply)

  val encryptedPasswordGen: Gen[EncryptedPassword] =
    nesGen(EncryptedPassword.apply)

  val userWithPasswordGen: Gen[UserWithPassword] =
    for {
      id   <- userIdGen
      name <- userNameGen
      pass <- encryptedPasswordGen
    } yield UserWithPassword(id, name, pass)

  // Order generators

  val orderIdGen: Gen[OrderId] =
    idGen(OrderId(_))

  val paymentIdGen: Gen[PaymentId] =
    idGen(PaymentId(_))

  val orderGen: Gen[Order] =
    for {
      id    <- orderIdGen
      pid   <- paymentIdGen
      items <- Gen.nonEmptyMap(itemMapGen)
      total <- moneyGen
    } yield Order(id, pid, items, total)

  // Payment generators

  val paymentGen: Gen[Payment] = 
    for {
      id    <- userIdGen
      total <- moneyGen
      card  <- cardGen
    } yield Payment(id, total, card)
}