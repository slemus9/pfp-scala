package shopping.service

import shopping.domain._
import shopping.auth._
import shopping.domain.user._
import dev.profunktor.auth.jwt.{JwtToken}
import pdi.jwt.JwtClaim
import io.circe.syntax._
import io.circe.parser.decode
import cats.syntax.all._
import cats.{Functor, Applicative, MonadThrow}
import dev.profunktor.redis4cats.RedisCommands
import shopping.config.types.TokenExpiration
import scala.tools.nsc.ast.parser.Tokens

trait AuthService [F[_]] {

  def newUser (username: UserName, password: Password): F[JwtToken]

  def login (username: UserName, password: Password): F[JwtToken]

  def logout (token: JwtToken, username: UserName): F[Unit]
}

object AuthService {

  def make [F[_]: MonadThrow] (
    tokenExpiration: TokenExpiration,
    tokens: Tokens[F],
    users: UserService[F],
    redis: RedisCommands[F, String, String],
    crypto: Crypto
  ) = new AuthService[F] {

    private val tokenExp = tokenExpiration.value

    def newUser(username: UserName, password: Password): F[JwtToken] = 
      users.find(username).flatMap {
        case Some(_) => UserNameInUse(username).raiseError
        case None    => for {
          id <- users.create(username, crypto.encrypt(password))
          tk <- tokens.create
          u = User(id, username).asJson.noSpaces
          _  <- redis.setEx(tk.value, u, tokenExp)
          _  <- redis.setEx(username.show, tk.value, tokenExp)
        } yield tk
      }

    def login(username: UserName, password: Password): F[JwtToken] = 
      users.find(username).flatMap {
        case None => UserNotFound(username).raiseError
        
        case Some(user) if user.password =!= crypto.encrypt(password) =>
          InvalidPassword(user.name).raiseError
        
        case Some(user)    => redis.get(username.show).flatMap {
          case Some(token) => JwtToken(token).pure
          case None        => 
            val u = User(user.id, user.name)
            tokens.create.flatTap { token => 
              redis.setEx(
                token.value, u.asJson.noSpaces, tokenExp
              ) *> redis.setEx(
                username.show, token.value, tokenExp
              )
            }
        }
      }

    def logout(token: JwtToken, username: UserName): F[Unit] = 
      redis.del(token.show) *>
      redis.del(username.show).void
  }
}

trait UserAuthService [F[_], A] {

  def findUser (token: JwtToken) (claim: JwtClaim): F[Option[A]]
}

object UserAuthService {

  def common [F[_]: Functor] (
    redis: RedisCommands[F, String, String]
  ) = new UserAuthService[F, CommonUser] {

    def findUser (token: JwtToken) (claim: JwtClaim): F[Option[CommonUser]] = 
      redis
        .get(token.value)
        .map(_.flatMap { u => 
          println(token.value)
          println(u)
          println(decode[User](u).toOption)
          decode[User](u).toOption.map(CommonUser(_))
        })
  }

  def admin [F[_]: Applicative] (
    adminToken: JwtToken,
    adminUser: AdminUser
  ) = new UserAuthService[F, AdminUser] {

    def findUser (token: JwtToken) (claim: JwtClaim): F[Option[AdminUser]] = 
      (token === adminToken) // unique admin user
        .guard[Option]
        .as(adminUser)
        .pure[F]
  }
}