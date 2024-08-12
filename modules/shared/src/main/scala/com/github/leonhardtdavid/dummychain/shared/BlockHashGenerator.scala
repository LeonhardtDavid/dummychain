package com.github.leonhardtdavid.dummychain.shared

import cats.effect.kernel.{ Resource, Sync }
import cats.implicits._
import com.github.leonhardtdavid.dummychain.shared.BlockHashGenerator.GeneratedHash

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class BlockHashGenerator[F[_]: Sync](numberOfZeros: Int) {

  def generate(sequence: Long, transactionsHash: List[String], previousHash: Option[String]): F[GeneratedHash] = {
    def hash(nonce: Long) = Sync[F].delay {
      val hs = MessageDigest
        .getInstance("SHA-256")
        .digest(
          s"$sequence$nonce${transactionsHash.mkString}${previousHash.getOrElse("")}"
            .getBytes(StandardCharsets.UTF_8)
        )

      hs.map("%02x".format(_)).mkString
    }

    Sync[F]
      .iterateWhileM(-1L -> "") {
        case (n, _) =>
          val nonce = n + 1
          hash(nonce).map(nonce -> _)
      } {
        case (_, hs) => !hs.startsWith("0".repeat(numberOfZeros))
      }
      .map {
        case (nonce, hash) => GeneratedHash(nonce, hash)
      }
  }

}

object BlockHashGenerator {

  case class GeneratedHash(nonce: Long, hash: String)

  def resource[F[_]: Sync](numberOfZeros: Int): Resource[F, BlockHashGenerator[F]] =
    Resource.pure(new BlockHashGenerator[F](numberOfZeros))

}
