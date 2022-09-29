package shopping.api.routes

import shopping.domain.id.ID
import shopping.domain.brand._
import shopping.domain.item._
import shopping.generators._
import shopping.service.ItemService

import cats.effect._
import cats.syntax.all._
import org.http4s.Method._
import org.http4s.Status
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import org.scalacheck.Gen
import suite.HttpSuite
import fs2.Stream
import shopping.domain.item

object ItemRoutesSuite extends HttpSuite {

  def dataItems (items: List[Item]) = new TestItems {

    override def findAll: Stream[IO,Item] = Stream(items: _*)

    override def findBy (brand: BrandName): Stream[IO,Item] = 
      Stream(items: _*).filter(_.brand.name === brand)
  }

  def failingItems(items: List[Item]) = new TestItems {
    
    override def findAll: Stream[IO,Item] = 
      Stream.eval(IO.raiseError(DummyError)) *> Stream(items: _*)

    override def findBy (brand: BrandName): Stream[IO,Item] = 
      findAll
  }

  test("Get items by brand succeeds") {

    val gen = for {
      item  <- Gen.listOf(itemGen)
      brand <- brandGen
    } yield item -> brand

    forall (gen) { case (items, brand) => 
      val uri = uri"/items"
      val req = GET(uri.withQueryParam("brand", brand.name.value))
      val expected = Stream(items.filter(_.brand.name === brand.name): _*)
      val routes = ItemRoutes.router[IO](dataItems(items))
      expectStreamedHttpBodyAndStatus (routes, req) (expected, Status.Ok)
    }
  }

  test ("GET items succeeds") {
    forall (Gen.listOf(itemGen)) { items => 
      val req = GET(uri"/items")
      val routes = ItemRoutes.router(dataItems(items))
      expectStreamedHttpBodyAndStatus (routes, req) (Stream(items: _*), Status.Ok)
    }
  }
}

protected class TestItems extends ItemService[IO] {

  def findAll: Stream[IO, Item]                  = Stream.empty

  def findBy(brand: BrandName): Stream[IO, Item] = Stream.empty

  def findById(itemId: ItemId): IO[Option[Item]] = IO.pure(none[Item])

  def create(item: CreateItem): IO[ItemId]       = ID.make[IO, ItemId]

  def update(item: UpdateItem): IO[Unit]         = IO.unit
}