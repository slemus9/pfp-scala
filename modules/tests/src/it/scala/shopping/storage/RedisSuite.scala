package shopping.storage

import scala.concurrent.duration._
import fs2.Stream
import suite.ResourceSuite
import dev.profunktor.auth.jwt._
import dev.profunktor.redis4cats.log4cats._
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import cats.syntax.all._
import cats.effect.{IO, Resource, Ref}
import org.typelevel.log4cats.noop.NoOpLogger
import shopping.config.types.ShoppingCartExpiration
import shopping.generators._
import shopping.domain.brand._
import shopping.domain.category._
import shopping.domain.item._
import shopping.domain.cart._
import shopping.service.ItemService
import shopping.domain.brand
import shopping.domain.id.ID
import shopping.service.ShoppingCartService

object RedisSuite extends ResourceSuite {

  type Res = RedisCommands[IO, String, String]

  implicit val logger = NoOpLogger[IO]

  val expiration = ShoppingCartExpiration(30.seconds)

  def sharedResource: Resource[IO, Res] = 
    Redis[IO]
      .utf8("redis://localhost")
      .beforeAll(_.flushAll)

  test ("Shopping Cart") { redis => 
    val gen = for {
      uid <- userIdGen
      it1 <- itemGen
      it2 <- itemGen
      q1  <- quatityGen
      q2  <- quatityGen
    } yield (uid, it1, it2, q1, q2)

    forall (gen) { case (uid, it1, it2, q1, q2) =>
      Ref.of[IO, Map[ItemId, Item]](
        Map(it1.uuid -> it1, it2.uuid -> it2)
      ).flatMap { ref => 
        val items = new TestItemService(ref)
        val cart  = ShoppingCartService.make[IO](
          items, redis, expiration
        ) 

        for {
          c1 <- cart.get(uid)
          _  <- cart.add(uid, it1.uuid, q1)
          _  <- cart.add(uid, it2.uuid, q2)
          c2 <- cart.get(uid)
          _  <- cart.removeItem(uid, it1.uuid)
          c3 <- cart.get(uid)
          _  <- cart.update(uid, Cart(Map(it2.uuid -> q2)))
          c4 <- cart.get(uid)
          _  <- cart.delete(uid)
          c5 <- cart.get(uid)
        } yield expect.all(
          c1.items.isEmpty,
          c2.items.size === 2,
          c3.items.size === 1,
          c4.items.headOption.fold (false) (_.quantity === q2),
          c5.items.isEmpty,
        )
      }
    }
  }
}

private class TestItemService (
  ref: Ref[IO, Map[ItemId, Item]]
) extends ItemService[IO] {

  def findAll: Stream[IO, Item] = 
    Stream.eval(ref.get).flatMap { 
      db => Stream.unfold (db.valuesIterator) {
        it => Option.when (it.hasNext) (it.next -> it)
      }
    }

  def findBy(brand: BrandName): Stream[IO,Item] = 
    findAll.filter(_.brand.name === brand)

  def findById(itemId: ItemId): IO[Option[Item]] = 
    ref.get.map { db => db.get(itemId) }

  def create(item: CreateItem): IO[ItemId] = 
    ID.make[IO, ItemId].flatMap { id => 
      val brand = Brand(item.brandId, BrandName("foo"))
      val category = Category(item.categoryId, CategoryName("foo"))
      val newItem = Item(
        id, item.name, item.description, 
        item.price, brand, category
      )

      ref.update(_ + (id -> newItem)) as id
    }

  def update(item: UpdateItem): IO[Unit] = 
    ref.update { db =>
      db.get(item.id).fold (db) { existingItem => 
        db.updated(item.id, existingItem.copy(price = item.price))
      }  
    }
}