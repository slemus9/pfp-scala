package shopping.service

import shopping.domain.user._
import dev.profunktor.auth.jwt.JwtToken

trait AuthService [F[_]] {

  def findUser (token: JwtToken): F[Option[User]]

  def newUser (username: UserName, password: Password): F[JwtToken]

  def login (username: UserName, password: Password): F[JwtToken]

  def logout (token: JwtToken, username: UserName): F[Unit]
}