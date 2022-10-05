package shopping.domain

import eu.timepit.refined.cats._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{MatchesRegex, ValidInt}
import eu.timepit.refined.collection.{Size, NonEmpty}
import eu.timepit.refined.generic.Equal
import eu.timepit.refined.boolean.And
import io.estatico.newtype.macros.newtype
import shopping.utils.circe.refined.codecOf
import shopping.utils.refined.numeric.IntegralOfSize
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Encoder, Decoder, Codec}
import cats.syntax.either._
import derevo.derive
import derevo.cats.show

object checkout {

  // Type Predicates
  type Rgx = "^[a-zA-Z]+(([',. -][a-zA-Z ])?[a-zA-Z]*)*$"

  type CardNamePred = MatchesRegex[Rgx]

  type CardNumberPred = IntegralOfSize[16]

  type CardExpirationPred = Size[Equal[4]] And ValidInt

  type CardCVVPred = Equal[3]

  // Models
  @derive(show)
  @newtype final case class CardName (value: String Refined CardNamePred)

  object CardName {
    implicit val jsonCodec: Codec[CardName] =
      codecOf[String, CardNamePred].iemap (CardName(_).asRight) (_.value)
  }

  @derive(show)
  @newtype final case class CardNumber (value: Long Refined CardNumberPred)

  object CardNumber {
    implicit val jsonCodec: Codec[CardNumber] =
      codecOf[Long, CardNumberPred].iemap (CardNumber(_).asRight) (_.value)
  }

  @derive(show)
  @newtype final case class CardExpiration (value: String Refined CardExpirationPred)

  object CardExpiration {
    implicit val jsonCodec = 
      codecOf[String, CardExpirationPred].iemap (CardExpiration(_).asRight) (_.value)
  }

  @derive(show)
  @newtype final case class CardCVV (value: Int Refined CardCVVPred)

  object CardCVV {
    implicit val jsonCodec =
      codecOf[Int, CardCVVPred].iemap (CardCVV(_).asRight) (_.value)
  } 

  @derive(show)
  final case class Card (
    name: CardName,
    number: CardNumber,
    expiration: CardExpiration,
    cvv: CardCVV
  )

  object Card {
    implicit val jsonCodec = deriveCodec[Card]
  }
}