package shopping.service

import shopping.domain.item._
import shopping.domain.cart._
import shopping.domain.user.UserId

trait ShoppingCartService [F[_]] {

  def add (
    userId: UserId,
    itemId: ItemId,
    quantity: Quantity
  ): F[Unit]

  def get (userId: UserId): F[CartTotal]

  def delete (userId: UserId): F[Unit]

  def removeItem (userId: UserId, item: ItemId): F[Unit]

  def update (userId: UserId, cart: Cart): F[Unit]
}