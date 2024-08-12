package com.github.leonhardtdavid.dummychain.shared

import cats.effect.Sync
import cats.effect.kernel.Resource
import cats.implicits._
import com.github.leonhardtdavid.dummychain.shared.KeyPairGenerator.KeyPair
import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.security.{ KeyPairGenerator => JKeyPairGenerator, SecureRandom, Security }
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class KeyPairGenerator[F[_]: Sync] {

  def generate: F[KeyPair] = Sync[F].delay {
    val keyGen = JKeyPairGenerator.getInstance("ECDSA", "BC")
    val ecSpec = new ECGenParameterSpec("secp256k1")
    keyGen.initialize(ecSpec, new SecureRandom())
    val keyPair = keyGen.generateKeyPair()

    KeyPair(
      privateKey = Base64.getUrlEncoder.withoutPadding().encodeToString(keyPair.getPrivate.getEncoded),
      publicKey = Base64.getUrlEncoder.withoutPadding().encodeToString(keyPair.getPublic.getEncoded)
    )
  }

}

object KeyPairGenerator {

  case class KeyPair(privateKey: String, publicKey: String)

  def resource[F[_]: Sync]: Resource[F, KeyPairGenerator[F]] =
    Resource.eval {
      Sync[F].delay(Security.addProvider(new BouncyCastleProvider())) *>
        Sync[F].pure(new KeyPairGenerator)
    }

}
