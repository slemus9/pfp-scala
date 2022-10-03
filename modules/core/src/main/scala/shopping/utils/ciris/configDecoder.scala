package shopping.utils.ciris

import shopping.utils.derevo.Derive
import ciris.ConfigDecoder

object configDecoder extends Derive[Decoder.Id]

object Decoder {
  
  type Id [A] = ConfigDecoder[String, A]
}