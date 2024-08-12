package com.github.leonhardtdavid.dummychain.shared

import cats.effect.Sync
import cats.effect.kernel.Resource
import cats.implicits._
import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.nio.charset.StandardCharsets
import java.security.spec.PKCS8EncodedKeySpec
import java.security.{ KeyFactory, MessageDigest, PrivateKey, PublicKey, Security, Signature }
import java.util.Base64

class TransactionSigner[F[_]: Sync] {

  private def signatureInstance(): Signature = Signature.getInstance("SHA256withECDSA", "BC")

  private def hash(source: String, destination: String, amount: BigDecimal) = Sync[F].delay {
    MessageDigest
      .getInstance("SHA-256")
      .digest(
        s"$source$destination$amount".getBytes(StandardCharsets.UTF_8)
      )
  }

  private def loadPrivateKey(key: String): F[PrivateKey] = Sync[F].delay {
    val keyFactory      = KeyFactory.getInstance("ECDSA", "BC")
    val privateKeyBytes = Base64.getUrlDecoder.decode(key)
    val keySpec         = new PKCS8EncodedKeySpec(privateKeyBytes)

    keyFactory.generatePrivate(keySpec)
  }

  def sign(source: String, destination: String, amount: BigDecimal, privateKey: String): F[String] =
    for {
      key <- loadPrivateKey(privateKey)
      hs  <- hash(source, destination, amount)
    } yield {
      val instance = signatureInstance()
      instance.initSign(key)
      instance.update(hs)

      val signature = instance.sign()
      Base64.getUrlEncoder.withoutPadding().encodeToString(signature)
    }

  def validate(source: String, destination: String, amount: BigDecimal, signature: String, key: PublicKey): F[Boolean] =
    hash(source, destination, amount).map { hs =>
      val instance = signatureInstance()
      instance.initVerify(key)
      instance.update(hs)

      val signatureBytes = Base64.getUrlDecoder.decode(signature)
      instance.verify(signatureBytes)
    }

}

object TransactionSigner {

  def resource[F[_]: Sync]: Resource[F, TransactionSigner[F]] =
    Resource.eval {
      Sync[F].delay(Security.addProvider(new BouncyCastleProvider())) *>
        Sync[F].pure(new TransactionSigner)
    }

}
