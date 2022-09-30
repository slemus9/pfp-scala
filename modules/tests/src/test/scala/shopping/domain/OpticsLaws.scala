package shopping.domain

import weaver.FunSuiteIO
import weaver.discipline.Discipline
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import shopping.generators.brandIdGen
import shopping.domain.health._
import shopping.domain.brand.BrandId
import org.scalacheck.Cogen
import java.util.UUID
import monocle.law.discipline.IsoTests
import shopping.utils.uuid.IsUUID

object OpticsLaws extends FunSuiteIO with Discipline {

  implicit val arbStatus: Arbitrary[Status] =
    Arbitrary(Gen.oneOf(
      Status.Okay, Status.Unreachable
    ))

  implicit val brandIdArb: Arbitrary[brand.BrandId] =
    Arbitrary(brandIdGen)

  implicit val brandIdCogen: Cogen[BrandId] =
    Cogen[UUID].contramap[BrandId](_.value)

  checkAll("ISO[Status._Bool]", IsoTests(Status._Bool))

  checkAll("IsUUID[UUID]", IsoTests(IsUUID[UUID]._UUID))
  checkAll("IsUUID[BrandId]", IsoTests(IsUUID[BrandId]._UUID))
}