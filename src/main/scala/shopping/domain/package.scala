package shopping

import cats.syntax.contravariant._
import cats.{Eq, Show, Monoid}
import io.circe.{Encoder, Decoder}
import squants.market.{Money, USD, Currency}
import dev.profunktor.auth.jwt.JwtToken

package object domain extends OrphanInstances

trait OrphanInstances {

  implicit val moneyEncoder: Encoder[Money] =
    Encoder[BigDecimal].contramap(_.amount)

  implicit val moneyDecoder: Decoder[Money] =
    Decoder[BigDecimal].map(USD(_))

  implicit val moneyMonoid: Monoid[Money] = new Monoid[Money] {

    def empty: Money = USD(0)

    def combine (x: Money, y: Money): Money = x + y
  }

  implicit val currencyEq: Eq[Currency] =
    Eq.and(
      Eq.and(Eq.by(_.code), Eq.by(_.symbol)),
      Eq.by(_.name)
    )

  implicit val moneyEq: Eq[Money] =
    Eq.and(
      Eq.by(_.amount),
      Eq.by(_.currency)
    )

  implicit val moneyShow: Show[Money] =
    Show.fromToString

  implicit val tokenEq: Eq[JwtToken] =
    Eq.by(_.value)

  implicit val tokenShow: Show[JwtToken] =
    Show[String].contramap(_.value)

  implicit val tokenEncoder: Encoder[JwtToken] =
    Encoder.forProduct1 ("access_token") (_.value)
}