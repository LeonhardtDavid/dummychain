package com.github.leonhardtdavid.dummychain.service.api

import cats.effect.Sync
import cats.implicits._
import Transactions._
import com.github.leonhardtdavid.dummychain.service.BlockchainStore
import com.github.leonhardtdavid.dummychain.service.model.Transaction
import com.github.leonhardtdavid.dummychain.service.model.Transaction.{ Amount, DestinationAddress, SourceAddress, TransactionSignature }
import io.circe.refined._
import io.circe.generic.auto._
import io.circe.syntax._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.ServerEndpoint

class Transactions[F[_]: Sync](store: BlockchainStore[F]) {

  private implicit val logger: Logger[F] = Slf4jLogger.getLogger[F]

  private val createTransaction = postTransaction.serverLogic[F] { request =>
    for {
      _ <- Logger[F].debug(s"Received transaction ${request.asJson.noSpaces}")
      _ <- store.addTransaction(
        Transaction(
          source = request.source,
          destination = request.destination,
          amount = request.amount,
          signature = request.signature
        )
      )
      bc <- store.store.get
      _  <- Logger[F].debug(s"Current store $bc")
    } yield ().asRight
  }

  val routes: List[ServerEndpoint[Any, F]] = List(
    createTransaction
  )

}

object Transactions {

  case class TransactionRequest(
    source: SourceAddress,
    destination: DestinationAddress,
    amount: Amount,
    signature: TransactionSignature
  ) // TODO add proper types and missing fields

  private val postTransaction: Endpoint[Unit, TransactionRequest, Unit, Unit, Any] =
    endpoint.post
      .in("api" / "transactions")
      .in(
        jsonBody[TransactionRequest]
//          .description() // TODO add doc
//          .examples()
      )
      .out(statusCode(StatusCode.NoContent)) // TODO change to proper code or remove and add a response body
//      .errorOut() // TODO handle errors
      .description("Create a new transaction")
      .tag("Transactions")

}
