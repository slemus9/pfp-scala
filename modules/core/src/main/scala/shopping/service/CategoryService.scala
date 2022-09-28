package shopping.service

import shopping.domain.category._

trait CategoryService [F[_]] {

  def findAll: F[List[Category]]

  def create (name: CategoryName): F[CategoryId]
}