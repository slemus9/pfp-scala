package shopping.service

import shopping.domain.health._

trait HealthService [F[_]] {

  def status: F[AppStatus]
}