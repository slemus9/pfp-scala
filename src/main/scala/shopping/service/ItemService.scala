package shopping.service

import skunk._
import skunk.implicits._
import cats.syntax.all._
import cats.effect.{Concurrent, Resource}
import shopping.domain.brand._
import shopping.domain.category._
import shopping.domain.item._
import shopping.sql.codecs._
import shopping.utils.uuid.GenUUID
import shopping.domain.id.ID
import fs2.Stream

trait ItemService [F[_]] {

  def findAll: Stream[F, Item]

  def findBy (brand: BrandName): Stream[F, Item]

  def findById (itemId: ItemId): F[Option[Item]]

  def create (item: CreateItem): F[ItemId]

  def update (item: UpdateItem): F[Unit]
}

object ItemService {

  import ItemSQL._

  def make [F[_]: Concurrent: GenUUID] (
    postgres: Resource[F, Session[F]]
  ) = new ItemService[F] {

    def findAll: Stream[F, Item] = 
      for {
        session <- Stream.resource(postgres)
        query   <- Stream.resource(session.prepare(selectAll))
        item    <- query.stream(Void, 1024)
      } yield item

    def findBy (brand: BrandName): Stream[F, Item] = 
      for {
        session <- Stream.resource(postgres)
        query   <- Stream.resource(session.prepare(selectByBrand))
        item    <- query.stream(brand, 1024)
      } yield item

    def findById (itemId: ItemId): F[Option[Item]] = postgres.use {
      _.prepare(selectById).use {
        _.option(itemId)
      }
    }

    def create (item: CreateItem): F[ItemId] = postgres.use {
      _.prepare(insertItem).use { cmd => 
        ID.make[F, ItemId].flatMap { id => 
          cmd.execute(id ~ item).as(id)  
        }
      }
    }

    def update (item: UpdateItem): F[Unit] = postgres.use {
      _.prepare(updateItem).use {
        _.execute(item).void
      }
    }
  }
}

private object ItemSQL {

  val decoder: Decoder[Item] =
    ( itemId
    ~ itemName
    ~ itemDescription
    ~ money
    ~ brandId ~ brandName
    ~ categoryId ~ categoryName
    ).map {
      case id ~ name ~ desc ~ price 
        ~ bId ~ bName 
        ~ cId ~ cName => Item(
          id, name, desc, price, 
          Brand(bId, bName), 
          Category(cId, cName)
        )
    }

  private val selectItemsFragment: Fragment[Void] = 
    sql"""
    SELECT i.uuid, i.name, i.description, i.price,
           b.uuid, b.name, c.uuid, c.name
    FROM items as i
    INNER JOIN brands as b ON i.brand_id = b.uuid
    INNER JOIN categories AS c ON i.category_id = c.uuid
    """

  val selectAll: Query[Void, Item] =
    selectItemsFragment.query(decoder)

  val selectByBrand: Query[BrandName, Item] =
    sql"$selectItemsFragment WHERE b.name LIKE $brandName".query(decoder)

  val selectById: Query[ItemId, Item] =
    sql"$selectItemsFragment WHERE i.uuid = $itemId".query(decoder)

  val insertItem: Command[ItemId ~ CreateItem] =
    sql"""
    INSERT INTO items
    VALUES (
      $itemId, $itemName, $itemDescription,
      $money, $brandId, $categoryId
    )
    """.command.contramap {
      case id ~ item =>
        id ~ item.name ~ item.description ~
        item.price ~ item.brandId ~ item.categoryId
    }

  val updateItem: Command[UpdateItem] =
    sql"""
    UPDATE items
    SET price = $money
    WHERE uuid = $itemId
    """.command.contramap { item => 
      item.price ~ item.id  
    }
}