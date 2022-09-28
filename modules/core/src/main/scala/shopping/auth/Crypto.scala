package shopping.auth

import shopping.domain.user.{Password, EncryptedPassword}

trait Crypto {

  def encrypt (value: Password): EncryptedPassword

  def decrypt (value: EncryptedPassword): Password
}