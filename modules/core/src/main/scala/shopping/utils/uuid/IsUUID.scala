package shopping.utils.uuid

import monocle.Iso
import java.util.UUID
import shopping.utils.derevo.Derive

trait IsUUID [A] {

  val _UUID: Iso[UUID, A]
}

object IsUUID {

  def apply [A] (implicit u: IsUUID[A]) = u

  implicit val identityUUID = new IsUUID[UUID] {

    val _UUID: Iso[UUID, UUID] = 
      Iso[UUID, UUID] (identity) (identity)
  }

  // For deriving uuid in newtypes using derevo
  object uuid extends Derive[IsUUID]
}

