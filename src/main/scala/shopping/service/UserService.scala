package shopping.service

import shopping.domain.user._

trait UserService [F[_]] {

  def find (username: UserName): F[Option[UserWithPassword]]

  def create (username: UserName, password: EncryptedPassword): F[UserId]
}