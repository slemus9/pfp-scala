package shopping.service

import shopping.domain.brand._

trait BrandService [F[_]] {

  def findAll: F[List[Brand]]

  def create (name: BrandName): F[BrandId]
}