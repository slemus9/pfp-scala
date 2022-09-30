package shopping.domain

import weaver.FunSuiteIO
import weaver.discipline.Discipline
import org.scalacheck.Arbitrary
import cats.kernel.laws.discipline.MonoidTests
import squants.market.Money
import shopping.generators.moneyGen

object OrphanLaws extends FunSuiteIO with Discipline {
  
  implicit val arbMoney: Arbitrary[Money] = 
    Arbitrary(moneyGen)

  checkAll("Monoid[Money]", MonoidTests[Money].monoid)
}