package shopping.auth

import dev.profunktor.auth.jwt

trait Tokens [F[_]] {

  def create: F[jwt.JwtToken]
}