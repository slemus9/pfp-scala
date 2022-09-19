package shopping.service

import shopping.domain.brand._
import shopping.domain.item._

trait ItemService [F[_]] {

  def findAll: F[List[Item]]

  def findBy (brand: BrandName): F[List[Item]]

  def findById (itemId: ItemId): F[Option[Item]]

  def create (item: CreateItem): F[ItemId]

  def update (item: UpdateItem): F[Unit]
}