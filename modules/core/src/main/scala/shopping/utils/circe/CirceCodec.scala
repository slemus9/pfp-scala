package shopping.utils.circe

import io.circe.{Encoder, Decoder, Codec}
import io.circe.Json

object CirceCodec {

  def from [A: Encoder: Decoder, B] (to: A => B, from: B => A): Codec[B] = {
    val encoder = Encoder[A].contramap[B](from)
    val decoder = Decoder[A].map(to)
    Codec.from(decoder, encoder)
  }
}