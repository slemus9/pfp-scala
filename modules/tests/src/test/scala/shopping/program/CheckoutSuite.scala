package shopping.program

import scala.concurrent.duration._
import scala.util.control.NoStackTrace
import shopping.generators._
import shopping.domain.brand._
import shopping.domain.category._
import shopping.domain.item._
import shopping.domain.cart._
import shopping.domain.order._
import shopping.domain.payment._
import shopping.domain.checkout._
import shopping.domain.user._
import shopping.service.OrderService
import shopping.service.ShoppingCartService
import shopping.client.PaymentClient
import shopping.effect.{TestBackground, TestRetry}
import org.typelevel.log4cats.noop.NoOpLogger
import squants.market.{Money, USD}
import cats.syntax.all._
import cats.effect.{IO, Ref}
import cats.data.NonEmptyList
import retry.{RetryPolicy, RetryPolicies, RetryDetails}
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object CheckoutSuite extends SimpleIOSuite with Checkers {

  implicit val bg = TestBackground.NoOp
  implicit val lg = NoOpLogger[IO]

  // Happy path interpreters

  def successfulClient (pid: PaymentId) = new PaymentClient[IO] {
    
    def process (payment: Payment): IO[PaymentId] = IO.pure(pid)
  }

  def successfulCart (cartTotal: CartTotal): ShoppingCartService[IO] =
    new TestShoppingCartService {
    
      override def get (userId: UserId): IO[CartTotal] = 
        IO.pure(cartTotal)

      override def delete (userId: UserId): IO[Unit] = 
        IO.unit
    }

  def successfulOrders (oid: OrderId): OrderService[IO] =
    new TestOrderService {

      override def create (
        userId: UserId, 
        paymentId: PaymentId, 
        items: NonEmptyList[CartItem], 
        total: Money
      ): IO[OrderId] = IO.pure(oid)
    }

  private val maxRetries = 3

  private val retryPolicy: RetryPolicy[IO] = 
    RetryPolicies.limitRetries(maxRetries)

  private val gen = for {
    uid <- userIdGen
    pid <- paymentIdGen
    oid <- orderIdGen
    crt <- cartTotalGen
    crd <- cardGen
  } yield (uid, pid, oid, crt, crd)

  test("successful checkout") {
    forall (gen) {
      case (uid, pid, oid, crt, crd) => 
        Checkout[IO]( 
          successfulClient(pid), 
          successfulCart(crt), 
          successfulOrders(oid), 
          retryPolicy
        )
        .process(uid, crd)
        .map(expect.same(oid, _))
    }
  }

  // Empty cart case
  val emptyCart: ShoppingCartService[IO] = new TestShoppingCartService {

    override def get (userId: UserId): IO[CartTotal] = 
      IO.pure(CartTotal(List.empty, USD(0)))
  }

  test("empty cart") {
    forall (gen) {
      case (uid, pid, oid, crt, crd) => 
        Checkout[IO](
          successfulClient(pid),
          emptyCart,
          successfulOrders(oid),
          retryPolicy
        )
        .process(uid, crd)
        .attempt
        .map {
          case Left(EmptyCartError) =>
            success
          case _ => 
            failure("Cart was not empty as expected")
        }
    }
  }

  // Unreachable payment client case
  val unreachableClient = new PaymentClient[IO] {

    def process (payment: Payment): IO[PaymentId] = 
      IO.raiseError(PaymentError(""))
  }

  test("unreachable payment client") {
    forall (gen) {
      case (uid, _, oid, crt, crd) => 
        Ref.of[IO, Option[RetryDetails.GivingUp]](None).flatMap {
          retries => 
            implicit val rh = TestRetry.givingUp(retries)

            Checkout[IO](
              unreachableClient,
              successfulCart(crt),
              successfulOrders(oid),
              retryPolicy
            )
            .process(uid, crd)
            .attempt
            .flatMap {
              case Left(PaymentError(_)) => retries.get.map {
                case Some(g) => expect.same(g.totalRetries, maxRetries)

                case None => failure("expected GivingUp")
              }

              case _ => IO.pure(failure("expected payment error"))
            }
        }
    }
  }

  // Recovering payment client case
  def recoveringClient (
    attemptsSoFar: Ref[IO, Int],
    paymentId: PaymentId
  ) = new PaymentClient[IO] {

    def process (payment: Payment): IO[PaymentId] = attemptsSoFar.get.flatMap {
      case n if n === 1 => IO.pure(paymentId)
      case _            => attemptsSoFar.update(_ + 1) *> IO.raiseError(
        PaymentError("")
      )
    }
  }

  test("failing payment client succeeds after one retry") {
    forall (gen) {
      case (uid, pid, oid, crt, crd) => 
        ( Ref.of[IO, Option[RetryDetails.WillDelayAndRetry]](None)
        , Ref.of[IO, Int](0)
        ).flatMapN { case (retries, cliRef) =>
          
          implicit val rh = TestRetry.recovering(retries)

          Checkout[IO](
            recoveringClient(cliRef, pid),
            successfulCart(crt),
            successfulOrders(oid),
            retryPolicy
          )
          .process(uid, crd)
          .attempt
          .flatMap {
            case Right(id) => retries.get.map {
              case Some(w) => expect.same(id, oid) |+| expect.same(0, w.retriesSoFar)
              case None    => failure("expected one retry")
            }
            case Left(_)   => IO.pure(failure("expected PaymentId"))
          }
        }
    }
  }

  // Failing orders case
  val failingOrders: OrderService[IO] = new TestOrderService {

    override def create (
      userId: UserId, 
      paymentId: PaymentId, 
      items: NonEmptyList[CartItem], 
      total: Money
    ): IO[OrderId] = IO.raiseError(OrderError(""))
  }

  test("cannot create order, run in the background") {
    forall (gen) {
      case (uid, pid, oid, crt, crd) => 
        ( Ref.of[IO, (Int, FiniteDuration)](0, 0.seconds)
        , Ref.of[IO, Option[RetryDetails.GivingUp]](None)
        ).flatMapN { case (acc, retries) =>

          implicit val bg = TestBackground.counter(acc)
          implicit val rh = TestRetry.givingUp(retries)

          Checkout[IO](
            successfulClient(pid),
            successfulCart(crt),
            failingOrders,
            retryPolicy
          )
          .process(uid, crd)
          .attempt
          .flatMap {
            case Left(OrderError(_)) => (acc.get, retries.get).mapN {
              case (c, Some(g)) => 
                expect.same(c, 1 -> 1.hour) |+| expect.same(g.totalRetries, maxRetries)

              case _            => 
                failure(s"expected $maxRetries retries and reschedule")
            }

            case _ => IO.pure(failure("expected order error"))
          }
        }
    }
  }

  // Failing cart deletion case
  def failingCart (cartTotal: CartTotal): ShoppingCartService[IO] = new TestShoppingCartService {

    override def get (userId: UserId): IO[CartTotal] = 
      IO.pure(cartTotal)

    override def delete (userId: UserId): IO[Unit] = 
      IO.raiseError(new NoStackTrace {})
  }

  test("failing to delete cart does not affect checkout") {
    forall (gen) {
      case (uid, pid, oid, crt, crd) => 
        Checkout[IO](
          successfulClient(pid),
          failingCart(crt),
          successfulOrders(oid),
          retryPolicy
        )
        .process(uid, crd)
        .map(expect.same(oid, _))
    }
  }
}

protected class TestOrderService () extends OrderService[IO] {

  def get (userId: UserId, orderId: OrderId): IO[Option[Order]] = ???

  def findBy (userId: UserId): IO[List[Order]] = ???

  def create (userId: UserId, paymentId: PaymentId, items: NonEmptyList[CartItem], total: Money): IO[OrderId] = ???
}

protected class TestShoppingCartService () extends ShoppingCartService[IO] {

  def add (userId: UserId, itemId: ItemId, quantity: Quantity): IO[Unit] = ???

  def get (userId: UserId): IO[CartTotal] = ???

  def delete (userId: UserId): IO[Unit] = ???

  def removeItem (userId: UserId, itemId: ItemId): IO[Unit] = ???

  def update (userId: UserId, cart: Cart): IO[Unit] = ???
}