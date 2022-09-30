package shopping.storage

import suite.ResourceSuite
import cats.syntax.all._
import cats.effect.{IO, Resource}
import skunk._
import skunk.implicits._
import natchez.Trace.Implicits.noop
import shopping.domain._
import shopping.generators._
import shopping.service._
import shopping.domain.brand._
import shopping.domain.category._
import shopping.domain.item._
import shopping.domain.user._
import org.scalacheck.Gen
import cats.data.NonEmptyList

object PostgresSuite extends ResourceSuite {

  type Res = Resource[IO, Session[IO]]

  val flushTables: List[Command[Void]] = List(
    "items", "brands", "categories", "orders", "users"
  ).map { table => 
    sql"DELETE FROM #$table".command  
  }

  def sharedResource: Resource[IO, Res] = 
    Session.pooled[IO](
      host = "localhost",
      port = 5432,
      user = "postgres",
      password = Some("my-password"),
      database = "store",
      max = 10
    )
    .beforeAll {
      _.use { session => flushTables.traverse_(session.execute) }
    }

  test ("BrandService") { postgres => 
    forall (brandGen) { brand => 
      val service = BrandService.make[IO](postgres)
      for {
        brands1  <- service.findAll
        _        <- service.create(brand.name)
        brands2  <- service.findAll
        creation <- service.create(brand.name).attempt
      } yield expect.all(
        brands1.isEmpty,
        brands2.count(_.name === brand.name) === 1,
        creation.isLeft
      )
    }  
  }

  test ("CategoryService") { postgres => 
    forall (categoryGen) { cat => 
      val service = CategoryService.make[IO](postgres)
      for {
        cats1    <- service.findAll
        _        <- service.create(cat.name)
        cats2    <- service.findAll
        creation <- service.create(cat.name).attempt
      } yield expect.all(
        cats1.isEmpty,
        cats2.count(_.name === cat.name) === 1,
        creation.isLeft
      )
    }
  }

  test ("ItemService") { postgres => 
    forall (itemGen) { item => 
      def newItem (bid: Option[BrandId], cid: Option[CategoryId]) = CreateItem(
        item.name, item.description, item.price,
        bid.getOrElse(item.brand.uuid),
        cid.getOrElse(item.category.uuid)
      )

      val brandService = BrandService.make[IO](postgres)
      val categoryService = CategoryService.make[IO](postgres)
      val itemService = ItemService.make[IO](postgres)

      for {
        items1 <- itemService.findAll.compile.toList
        _      <- brandService.create(item.brand.name)
        bid    <- brandService.findAll.map(_.headOption.map(_.uuid))
        _      <- categoryService.create(item.category.name)
        cid    <- categoryService.findAll.map(_.headOption.map(_.uuid))
        _      <- itemService.create(newItem(bid, cid))
        items2 <- itemService.findAll.compile.toList
      } yield expect.all(
        items1.isEmpty,
        items2.count(_.name === item.name) === 1
      )
    }
  }

  test ("UserService") { postgres => 
    val gen = for {
      name <- userNameGen
      pass <- encryptedPasswordGen
    } yield name -> pass

    forall (gen) { case (name, pass) =>
      val service = UserService.make[IO](postgres)
      for {
        id       <- service.create(name, pass)
        user     <- service.find(name)
        creation <- service.create(name, pass).attempt
      } yield expect.all(
        user.count(_.id === id) === 1,
        creation.isLeft
      )
    }
  }

  test ("OrderService") { postgres => 
    val itemsGen = 
      Gen
        .nonEmptyListOf(cartItemGen)
        .map(NonEmptyList.fromListUnsafe)
         
    val gen = for {
      oid <- orderIdGen
      pid <- paymentIdGen
      un  <- userNameGen
      pw  <- encryptedPasswordGen
      its <- itemsGen
      pr  <- moneyGen
    } yield (oid, pid, un, pw, its, pr)

    forall (gen) { case (oid, pid, un, pw, its, pr) =>
      val orderService = OrderService.make[IO](postgres)
      val userService = UserService.make[IO](postgres)
      for {
        userId  <- userService.create(un, pw)
        orders  <- orderService.findBy(userId)
        order   <- orderService.get(userId, oid)
        orderId <- orderService.create(userId, pid, its, pr)
      } yield expect.all(
        orders.isEmpty,
        order.isEmpty,
        orderId.value.version === 4
      )
    }
  }
}