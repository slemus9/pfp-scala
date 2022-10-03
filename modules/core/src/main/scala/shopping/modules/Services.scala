package shopping.modules

import cats.effect.{Temporal, Resource}
import skunk.Session
import dev.profunktor.redis4cats.RedisCommands
import shopping.service._
import shopping.utils.uuid.GenUUID
import shopping.config.types.ShoppingCartExpiration

sealed abstract class Services [F[_]] private (
  val cart: ShoppingCartService[F],
  val brands: BrandService[F],
  val categories: CategoryService[F],
  val items: ItemService[F],
  val orders: OrderService[F],
  val healthCheck: HealthService[F]
)

object Services {

  def make [F[_]: GenUUID: Temporal] (
    redis: RedisCommands[F, String, String],
    postgres: Resource[F, Session[F]],
    cartExpiration: ShoppingCartExpiration
  ): Services[F] = {

    val _items = ItemService.make(postgres)
    new Services[F](
      cart        = ShoppingCartService.make(_items, redis, cartExpiration),
      brands      = BrandService.make(postgres),
      categories  = CategoryService.make(postgres),
      items       = _items,
      orders      = OrderService.make(postgres),
      healthCheck = HealthService.make(postgres, redis)
    ) {}
  }
}