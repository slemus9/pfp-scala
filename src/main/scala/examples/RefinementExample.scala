package examples

import eu.timepit.refined.api.{Refined, RefinedTypeOps}
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.numeric.Greater
import eu.timepit.refined.collection.{Contains, NonEmpty}
import eu.timepit.refined.api.Validate
import eu.timepit.refined.auto._
import eu.timepit.refined._
import io.estatico.newtype.macros._
import _root_.cats.data._
import _root_.cats.implicits._


object RefinementExample extends App {

  type Username = String Refined Contains['g'] // should contain a 'g' and also must be non-empty

  final case class User (username: Username)

  trait UserRepo [F[_]] {

    def lookup (username: Username): F[Option[User]]
  }

  def userRepo [F[_]] = new UserRepo[F] {

    def lookup(username: Username): F[Option[User]] = ???
  }

  // userRepo.lookup("") // Compilation error
  // userRepo.lookup("aeinstein") // Compilation error
  def csagan [F[_]] = userRepo.lookup("csagan")


  // We can use refinements with Newtypes
  @newtype case class Brand (value: NonEmptyString)
  @newtype case class Category (value: NonEmptyString)

  // val invalidBrand = Brand("") // Compilation error
  val brand = Brand("foo")

  // Runtime validation
  val s = "some runtime value"

  val res: Either[String, NonEmptyString] =
    refineV[NonEmpty](s)

  println(
    NonEmptyString.from(s)
  )

  type GTFive = Int Refined Greater[5]
  object GTFive extends RefinedTypeOps[GTFive, Int]

  val n = 23

  println(
    GTFive.from(n)
  )

  // Validation with accumulation
  case class MyType (a: NonEmptyString, b: GTFive)

  def validate1 (a: String, b: Int): ValidatedNel[String, MyType] = 
    ( NonEmptyString.from(a).toValidatedNel
    , GTFive.from(b).toValidatedNel
    ).mapN(MyType.apply)

  println(
    validate1("", 3)
  )

  def validate2 (a: String, b: Int): EitherNel[String, MyType] = 
    ( NonEmptyString.from(a).toEitherNel
    , GTFive.from(b).toEitherNel
    ).parMapN(MyType.apply)

  println(
    validate2("", 3)
  )

  // Another example
  type UserNameR = NonEmptyString
  object UserNameR extends RefinedTypeOps[UserNameR, String]

  type NameR = NonEmptyString
  object NameR extends RefinedTypeOps[NameR, String]

  type EmailR = String Refined Contains['@']
  object EmailR extends RefinedTypeOps[EmailR, String]

  @newtype case class UserName (value: UserNameR)
  @newtype case class Name (value: NameR)
  @newtype case class Email (value: EmailR)

  case class Person (
    username: UserName,
    name: Name,
    email: Email
  )

  // We need an extra map for each refinement to lift them into the newtypes
  def mkPerson1 (
    u: String, 
    n: String, 
    e: String
  ): EitherNel[String, Person] =
    ( UserNameR.from(u).toEitherNel.map(UserName(_))
    , NameR.from(n).toEitherNel.map(Name(_))
    , EmailR.from(e).toEitherNel.map(Email(_))
    ).parMapN(Person)

  object NewtypeRefinedOps {

    import io.estatico.newtype.Coercible
    import io.estatico.newtype.ops._

    final class NewtypeRefinedPartiallyApplied [A] {

      def apply [T, P] (raw: T) (implicit
        c: Coercible[Refined[T, P], A],
        v: Validate[T, P]
      ): EitherNel[String, A] =
        refineV[P](raw).toEitherNel.map(_.coerce[A])

      }

      def validate [A] = new NewtypeRefinedPartiallyApplied[A]
  }

  import NewtypeRefinedOps._

  def mkPerson2 (
    u: String, 
    n: String, 
    e: String
  ): EitherNel[String, Person] =
    ( validate[UserName](u)
    , validate[Name](n)
    , validate[Email](e)
    ).parMapN(Person)

  println(
    mkPerson2("", "", "emailexample.com")
  )
}