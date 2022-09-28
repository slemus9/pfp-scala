package shopping.utils.circe

import io.circe.{Encoder, Decoder, Codec}
import eu.timepit.refined.api.Validate
import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV

object refined {

  def encoderOf [T, P] (implicit
    v: Validate[T, P],
    e: Encoder[T]
  ): Encoder[T Refined P] =
    e.contramap[T Refined P](_.value)

  def decoderOf [T, P] (implicit
    v: Validate[T, P],
    d: Decoder[T]
  ): Decoder[T Refined P] =
    d.emap(refineV[P].apply[T](_))

  def codecOf [T: Encoder: Decoder, P] (implicit
    v: Validate[T, P],
  ): Codec[T Refined P] = Codec.from(
    decoderOf[T, P], encoderOf[T, P]
  )
}