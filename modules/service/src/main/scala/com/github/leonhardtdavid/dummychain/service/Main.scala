package com.github.leonhardtdavid.dummychain.service

import cats.effect._
import com.github.leonhardtdavid.dummychain.service.api.Transactions
import com.github.leonhardtdavid.dummychain.shared.{ BlockHashGenerator, KeyPairGenerator, TransactionSigner }
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object Main extends IOApp {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    val serverResource = for {
      config           <- ServiceConfig.resource[IO]
      keyPairGenerator <- KeyPairGenerator.resource[IO]
      keyPair          <- Resource.eval(keyPairGenerator.generate)
      signer           <- TransactionSigner.resource[IO]
      hashGenerator    <- BlockHashGenerator.resource[IO](config.blockchain.numberOfZeros)
      store <- BlockchainStore.resource(config.blockchain.maxTransactionsPerBlock)(
        keyPair.publicKey,
        BigDecimal("10000"),
        keyPair.privateKey
      )(signer, hashGenerator)
      transactions = new Transactions[IO](store)
      apiEndpoints = transactions.routes
      docEndpoints = SwaggerInterpreter().fromServerEndpoints[IO](transactions.routes, BuildInfo.name, BuildInfo.version)
      server <- Server.resource[IO](apiEndpoints ++ docEndpoints, config.server)
    } yield server -> keyPair

    serverResource.use {
      case (server, keyPair) =>
        Logger[IO].info(s"Server started at port ${server.address.getPort}.") *>
          Logger[IO].debug(s"Private Key: ${keyPair.privateKey}") *>
          Logger[IO].debug(s"Public Key: ${keyPair.publicKey}") *>
          IO.never
    }
      .as(ExitCode.Success)
  }

}
