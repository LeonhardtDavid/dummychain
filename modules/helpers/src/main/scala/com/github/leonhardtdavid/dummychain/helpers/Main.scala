package com.github.leonhardtdavid.dummychain.helpers

import cats.effect.std.Console
import cats.effect.{ ExitCode, IO, IOApp }
import cats.implicits._
import com.github.leonhardtdavid.dummychain.shared.{ KeyPairGenerator, TransactionSigner }

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val resources = for {
      signer    <- TransactionSigner.resource[IO]
      generator <- KeyPairGenerator.resource[IO]
    } yield signer -> generator

    resources.use {
      case (signer, generator) =>
        args match {
          case "sign" :: source :: destination :: amount :: privateKey :: _ =>
            signer.sign(source, destination, BigDecimal(amount), privateKey).flatMap { sign =>
              Console[IO].println(s"The sign is: $sign")
            }

          case "generate" :: _ =>
            generator.generate.flatMap { pair =>
              Console[IO].println(s"Generated private key is: ${pair.privateKey}") *>
                Console[IO].println(s"Generated public key is: ${pair.publicKey}")
            }

          case _ =>
            IO.unit
        }
    }
      .as(ExitCode.Success)
  }

}
