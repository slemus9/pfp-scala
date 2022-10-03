package shopping.auth

import shopping.config.types.TokenExpiration
import shopping.effect.JwtClock

import cats.effect.Sync
import cats.syntax.all._
import pdi.jwt.JwtClaim

trait JwtExpire [F[_]] {
  def expiresIn(claim: JwtClaim, exp: TokenExpiration): F[JwtClaim]
}

object JwtExpire {
  def make [F[_]: Sync]: F[JwtExpire[F]] =
    JwtClock[F].utc.map { implicit jClock =>
      new JwtExpire[F] {
        def expiresIn(claim: JwtClaim, exp: TokenExpiration): F[JwtClaim] =
          Sync[F].delay(claim.issuedNow.expiresIn(exp.value.toMillis))
      }
    }
}