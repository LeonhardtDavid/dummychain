package com.github.leonhardtdavid.dummychain.service.api

import cats.effect.Sync
import cats.implicits._
import cats.data.EitherT
import com.github.leonhardtdavid.dummychain.service.api.Transactions._
import com.github.leonhardtdavid.dummychain.service.model.Transaction
import com.github.leonhardtdavid.dummychain.service.model.Transaction.{ Amount, DestinationAddress, SourceAddress, TransactionSignature }
import com.github.leonhardtdavid.dummychain.service.store.BlockchainStore.StoreError
import com.github.leonhardtdavid.dummychain.service.store.{ BlockchainStore, TransactionValidator }
import com.github.leonhardtdavid.dummychain.service.store.TransactionValidator.{ InsufficientFunds, InvalidDestination, InvalidSign }
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

import java.time.Instant

class Transactions[F[_]: Sync](store: BlockchainStore[F], validator: TransactionValidator[F]) {

  private implicit val logger: Logger[F] = Slf4jLogger.getLogger[F]

  private[api] val createTransaction = postTransaction.serverLogic[F] { request =>
    def validateTransaction(transaction: Transaction): F[Either[TransactionError, Unit]] =
      validator.validate(transaction).map {
        case Right(_) => ().asRight
        case Left(errors) =>
          ValidationErrorsResponse(
            errors = errors.map {
              case InvalidDestination(message) => ValidationErrorResponse(message)
              case InvalidSign                 => ValidationErrorResponse("Invalid Signature")
              case InsufficientFunds           => ValidationErrorResponse("Insufficient Funds") // TODO Maybe it'd be better to return 402
            }.toList
          ).asLeft
      }

    def addTransaction(transaction: Transaction): F[Either[TransactionError, Unit]] =
      store
        .addTransaction(transaction)
        .map {
          case Right(_)            => ().asRight
          case Left(StoreError(e)) => UnexpectedErrorResponse(e.getMessage).asLeft
        }

    val eitherT: EitherT[F, TransactionError, TransactionResponse] =
      for {
        _ <- EitherT.rightT[F, TransactionError](Logger[F].debug(s"Received transaction ${request.asJson.noSpaces}"))
        transaction = Transaction(
          source = request.source,
          destination = request.destination,
          amount = request.amount,
          signature = request.signature,
          timestamp = Instant.now()
        )
        _ <- EitherT(validateTransaction(transaction))
        _ <- EitherT(addTransaction(transaction))
      } yield
        TransactionResponse(
          source = transaction.source,
          destination = transaction.destination,
          amount = transaction.amount,
          timestamp = transaction.timestamp
        )

    eitherT.value
  }

  val routes: List[ServerEndpoint[Any, F]] = List(
    createTransaction
  )

}

object Transactions {

  case class ValidationErrorResponse(message: String)

  sealed trait TransactionError
  case class ValidationErrorsResponse(errors: List[ValidationErrorResponse]) extends TransactionError
  case class UnexpectedErrorResponse(message: String)                        extends TransactionError

  case class TransactionRequest(
    source: SourceAddress,
    destination: DestinationAddress,
    amount: Amount,
    signature: TransactionSignature
  )

  case class TransactionResponse(
    source: SourceAddress,
    destination: DestinationAddress,
    amount: Amount,
    timestamp: Instant
  )

  private val postTransaction: Endpoint[Unit, TransactionRequest, TransactionError, TransactionResponse, Any] =
    endpoint.post
      .in("api" / "transactions")
      .in(
        jsonBody[TransactionRequest]
          .description("Request body of a transaction request")
          .example(
            TransactionRequest(
              source = SourceAddress.unsafeFrom(
                "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEZO9q9ii0zTypbVpCVZBRmxIARCMY-6KKcB3VRpaXDJydL3bibI5xNHvFo1-4Bk5W-IILf0p3aOtVYpfo06jveQ"
              ),
              destination = DestinationAddress.unsafeFrom(
                "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEAhTGwJlILrlxwWhg1LmCjSu4T098LB4xQbiVkZDvWcZPuDvf6LFW7h8JoKy94Ho-wTXXl7dQ6cC-Ot2TjClkhg"
              ),
              amount = Amount.unsafeFrom(BigDecimal("1")),
              signature = TransactionSignature.unsafeFrom(
                "MEUCIQDnzbmyGtFm4RAvI0WJN8wbglL6OEj9m6SjrYgq4A8R7gIgZjrCMH2XFm79Seud03ZhalpJBmThu5XMMoideIvy2g8"
              )
            )
          )
      )
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[TransactionResponse])
      .errorOut(
        oneOf[TransactionError](
          oneOfVariant(
            StatusCode.BadRequest,
            jsonBody[ValidationErrorsResponse].description("An error occurred during transaction validation")
          ),
          oneOfVariant(
            StatusCode.InternalServerError,
            jsonBody[UnexpectedErrorResponse].description(
              "Unexpected error - unlikely - trying to insert the transaction into the blockchain"
            )
          )
        )
      )
      .description("Create a new transaction")
      .tag("Transactions")

}
