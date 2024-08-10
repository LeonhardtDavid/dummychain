package com.github.leonhardtdavid.dummychain.api

import cats.effect.Sync
import cats.implicits._
import com.github.leonhardtdavid.dummychain.api.Transactions._
import io.circe.generic.auto._
import io.circe.syntax._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.ServerEndpoint

class Transactions[F[_]: Sync] {

  private implicit val logger: Logger[F] = Slf4jLogger.getLogger[F]

  private val createTransaction = postTransaction.serverLogic[F] { transaction =>
    for {
      _ <- Logger[F].info(s"Received transaction ${transaction.asJson.noSpaces}")
    } yield ().asRight
  }

  val routes: List[ServerEndpoint[Any, F]] = List(
    createTransaction
  )

}

object Transactions {

  case class Transaction(source: String, destination: String, amount: BigDecimal) // TODO add proper types and missing fields

  private val postTransaction: Endpoint[Unit, Transaction, Unit, Unit, Any] =
    endpoint.post
      .in("api" / "transactions")
      .in(
        jsonBody[Transaction]
//          .description() // TODO add doc
//          .examples()
      )
      .out(statusCode(StatusCode.NoContent)) // TODO change to proper code or remove and add a response body
//      .errorOut() // TODO handle errors
      .description("Create a new transaction")
      .tag("Transactions")

}
