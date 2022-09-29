package shopping.api.routes

import org.scalacheck.Gen
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import org.http4s.Method._
import org.http4s.Status
import suite.HttpSuite
import cats.effect.IO
import shopping.generators._
import shopping.domain.brand._
import shopping.service.BrandService
import shopping.domain.id.ID


object BrandRoutesSuite extends HttpSuite {

  def dataBrands (brands: List[Brand]) = new TestBrands {

    override def findAll: IO[List[Brand]] = 
      IO.pure(brands)
  }

  test("GET brands succeeds") {
    forall (Gen.listOf(brandGen)) { brands => 

      val req = GET(uri"/brands")
      val routes = BrandRoutes.router[IO](dataBrands(brands))
      expectHttpBodyAndStatus (routes, req) (brands, Status.Ok)
    }
  }

  def failingBrands (brands: List[Brand]) = new TestBrands {
    
    override def findAll: IO[List[Brand]] = 
      IO.raiseError(DummyError) *> IO.pure(brands)
  }

  test("GET brands fails") {
    forall(Gen.listOf(brandGen)) { brands => 

      val req = GET(uri"/brands")
      val routes = BrandRoutes.router[IO](failingBrands(brands))
      expectHttpFailure(routes, req)
    }
  }
}

protected class TestBrands extends BrandService[IO] {

  def create(name: BrandName): IO[BrandId] = ID.make[IO, BrandId]

  def findAll: IO[List[Brand]]             = IO.pure(List.empty)
}