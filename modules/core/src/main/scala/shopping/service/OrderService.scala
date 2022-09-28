package shopping.service

import skunk._
import skunk.implicits._
import skunk.circe.codec.all._
import cats.syntax.all._
import cats.effect.{Concurrent, Resource}
import shopping.domain.order._
import shopping.domain.cart._
import shopping.domain.user.UserId
import shopping.domain.item.ItemId
import cats.data.NonEmptyList
import squants.market.Money
import shopping.sql.codecs._
import shopping.utils.uuid.GenUUID
import shopping.domain.id.ID

trait OrderService [F[_]] {

  def get (userId: UserId, orderId: OrderId): F[Option[Order]]

  def findBy (userId: UserId): F[List[Order]]

  def create (
    userId: UserId,
    paymentId: PaymentId,
    items: NonEmptyList[CartItem],
    total: Money
  ): F[OrderId]
}

object OrderService {

  import OrderSQL._

  def make [F[_]: Concurrent: GenUUID] (
    postgres: Resource[F, Session[F]]
  ) = new OrderService [F] {

    def get (userId: UserId, orderId: OrderId): F[Option[Order]] = postgres.use {
      _.prepare(selectByUserIdAndOrderId).use { 
        _.option(userId ~ orderId)
      }
    }

    def findBy (userId: UserId): F[List[Order]] = postgres.use {
      _.prepare(selectByUserId).use { q => 
        q.stream(userId, 1024).compile.toList  
      }
    }

    def create (
      userId: UserId, 
      paymentId: PaymentId, 
      items: NonEmptyList[CartItem], 
      total: Money
    ): F[OrderId] = postgres.use(_.prepare(insertOrder).use { cmd => 
      
      val makeOrder = ID.make[F, OrderId].map { id => Order(
        id, paymentId,
        Map.from(items.map { x => (x.item.uuid, x.quantity) }.iterator),
        total
      )}

      makeOrder.flatMap { order => 
        cmd.execute(userId ~ order) as order.id
      }
    })
  }
}

private object OrderSQL {

  val decoder: Decoder[Order] = 
    ( orderId ~ userId ~ paymentId
    ~ jsonb[Map[ItemId, Quantity]]
    ~ money
    ).map { 
      case id ~ _ ~ pid ~ items ~ total => Order(
        id, pid, items, total
      )
    }

  val selectByUserId: Query[UserId, Order] = 
    sql"""
    SELECT * FROM orders
    WHERE user_id = $userId
    """.query(decoder)

  val selectByUserIdAndOrderId: Query[UserId ~ OrderId, Order] = 
    sql"""
    SELECT * FROM orders
    WHERE user_id = $userId
    AND uuid = $orderId
    """.query(decoder)

  val encoder: Encoder[UserId ~ Order] = 
    ( orderId ~ userId ~ paymentId
    ~ jsonb[Map[ItemId, Quantity]]
    ~ money
    ).contramap { case userId ~ order => 
      order.id ~ userId ~ order.pid ~ order.items ~ order.total
    }

  val insertOrder: Command[UserId ~ Order] =
    sql"""
    INSERT INTO orders
    VALUES ($encoder)
    """.command
}