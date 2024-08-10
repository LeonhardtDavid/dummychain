package com.github.leonhardtdavid.dummychain

import cats.effect._
import com.github.leonhardtdavid.dummychain.api.Transactions
import com.github.leonhardtdavid.dummychain.config.AppConfig
import com.github.leonhardtdavid.dummychain.server.Server
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    val serverResource = for {
      config <- AppConfig.resource[IO]
      transactions = new Transactions[IO]
      apiEndpoints = transactions.routes
      docEndpoints = SwaggerInterpreter().fromServerEndpoints[IO](transactions.routes, BuildInfo.name, BuildInfo.version)
      routes       = if (config.docsEnabled) apiEndpoints ++ docEndpoints else apiEndpoints
      server <- Server.resource[IO](routes, config.server)
    } yield server

    serverResource.use { server =>
      Logger[IO].info(s"Server started at port ${server.address.getPort}. Press ENTER key to exit.") *>
        IO.never
    }
      .as(ExitCode.Success)
  }

}
