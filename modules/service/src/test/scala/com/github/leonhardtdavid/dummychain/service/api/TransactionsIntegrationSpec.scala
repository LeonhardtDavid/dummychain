package com.github.leonhardtdavid.dummychain.service.api

import cats.effect.unsafe.implicits.global
import cats.effect.{ IO, Resource }
import cats.implicits._
import com.github.leonhardtdavid.dummychain.service.api.Transactions._
import com.github.leonhardtdavid.dummychain.service.model.Transaction.{ Amount, SourceAddress, TransactionSignature }
import com.github.leonhardtdavid.dummychain.service.store.BlockchainStore.StoreError
import com.github.leonhardtdavid.dummychain.service.store.{ BlockchainStore, TransactionValidator }
import com.github.leonhardtdavid.dummychain.shared.{ BlockHashGenerator, KeyPairGenerator, TransactionSigner }
import io.circe.refined._
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import org.http4s.server.Router
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sttp.tapir.server.http4s.Http4sServerInterpreter

import java.time.Instant
import scala.util.Random

class TransactionsIntegrationSpec extends AnyWordSpec with Matchers with MockFactory {

  "The create transaction endpoint" should {

    "fail on invalid signature" in {
      val (keyPairSource, keyPairDestination, store, validator, _) = resource.use(IO.pure).unsafeRunSync()

      val api     = new Transactions[IO](store, validator)
      val service = Router("/" -> Http4sServerInterpreter[IO]().toRoutes(api.createTransaction)).orNotFound
      val request = Request[IO](
        method = Method.POST,
        uri = uri"/api/transactions"
      ).withEntity(
        TransactionRequest(
          source = SourceAddress.unsafeFrom(keyPairSource.publicKey),
          destination = SourceAddress.unsafeFrom(keyPairDestination.publicKey),
          amount = Amount.unsafeFrom(10),
          signature = TransactionSignature.unsafeFrom("signature")
        ).asJson
      )

      val result = for {
        result   <- service.run(request)
        response <- result.decodeJson[ValidationErrorsResponse]
      } yield (result.status, response)

      val (status, response) = result.unsafeRunSync()

      status shouldBe Status.BadRequest
      response.errors.head shouldBe ValidationErrorResponse("Invalid Signature")
      response.errors.tail shouldBe Nil
    }

    "fail on insufficient funds" in {
      val (keyPairSource, keyPairDestination, store, validator, signer) = resource.use(IO.pure).unsafeRunSync()

      val source      = SourceAddress.unsafeFrom(keyPairSource.publicKey)
      val destination = SourceAddress.unsafeFrom(keyPairDestination.publicKey)
      val amount      = Amount.unsafeFrom(10000)

      val api     = new Transactions[IO](store, validator)
      val service = Router("/" -> Http4sServerInterpreter[IO]().toRoutes(api.createTransaction)).orNotFound
      val request = (signature: String) =>
        Request[IO](
          method = Method.POST,
          uri = uri"/api/transactions"
        ).withEntity(
          TransactionRequest(
            source = source,
            destination = destination,
            amount = amount,
            signature = TransactionSignature.unsafeFrom(signature)
          ).asJson
        )

      val result = for {
        signature <- signer.sign(keyPairSource.publicKey, keyPairDestination.publicKey, amount.value, keyPairSource.privateKey)
        result    <- service.run(request(signature))
        response  <- result.decodeJson[ValidationErrorsResponse]
      } yield (result.status, response)

      val (status, response) = result.unsafeRunSync()

      status shouldBe Status.BadRequest
      response.errors.head shouldBe ValidationErrorResponse("Insufficient Funds")
      response.errors.tail shouldBe Nil
    }

    "fail on same source and destination" in {
      val (keyPairSource, _, store, validator, signer) = resource.use(IO.pure).unsafeRunSync()

      val source      = SourceAddress.unsafeFrom(keyPairSource.publicKey)
      val destination = SourceAddress.unsafeFrom(keyPairSource.publicKey)
      val amount      = Amount.unsafeFrom(10)

      val api     = new Transactions[IO](store, validator)
      val service = Router("/" -> Http4sServerInterpreter[IO]().toRoutes(api.createTransaction)).orNotFound
      val request = (signature: String) =>
        Request[IO](
          method = Method.POST,
          uri = uri"/api/transactions"
        ).withEntity(
          TransactionRequest(
            source = source,
            destination = destination,
            amount = amount,
            signature = TransactionSignature.unsafeFrom(signature)
          ).asJson
        )

      val result = for {
        signature <- signer.sign(keyPairSource.publicKey, keyPairSource.publicKey, amount.value, keyPairSource.privateKey)
        result    <- service.run(request(signature))
        response  <- result.decodeJson[ValidationErrorsResponse]
      } yield (result.status, response)

      val (status, response) = result.unsafeRunSync()

      status shouldBe Status.BadRequest
      response.errors.head shouldBe ValidationErrorResponse("The destination can't be the same as the source")
      response.errors.tail shouldBe Nil
    }

    "fail on unexpected errors" in {
      trait TValidator extends TransactionValidator[IO] // Using traits for type erasure
      val validator = mock[TValidator]

      trait BStore extends BlockchainStore[IO]
      val store = mock[BStore]

      val errorMsg = Random.alphanumeric.take(10).mkString // TODO use scalacheck?

      (validator.validate _).expects(*).once().returns(IO.pure(().asRight))
      (store.addTransaction _).expects(*).once().returns(IO.pure(StoreError(new RuntimeException(errorMsg)).asLeft))

      val api     = new Transactions[IO](store, validator)
      val service = Router("/" -> Http4sServerInterpreter[IO]().toRoutes(api.createTransaction)).orNotFound
      val request = Request[IO](
        method = Method.POST,
        uri = uri"/api/transactions"
      ).withEntity(
        TransactionRequest(
          source = SourceAddress.unsafeFrom("source-address"),
          destination = SourceAddress.unsafeFrom("destination-address"),
          amount = Amount.unsafeFrom(10),
          signature = TransactionSignature.unsafeFrom("signature")
        ).asJson
      )

      val result = for {
        result   <- service.run(request)
        response <- result.decodeJson[UnexpectedErrorResponse]
      } yield (result.status, response)

      val (status, response) = result.unsafeRunSync()
      status shouldBe Status.InternalServerError
      response.message shouldBe errorMsg
    }

    "succeed on valid request" in {
      val (keyPairSource, keyPairDestination, store, validator, signer) = resource.use(IO.pure).unsafeRunSync()

      val start = Instant.now()

      val source      = SourceAddress.unsafeFrom(keyPairSource.publicKey)
      val destination = SourceAddress.unsafeFrom(keyPairDestination.publicKey)
      val amount      = Amount.unsafeFrom(10)

      val api     = new Transactions[IO](store, validator)
      val service = Router("/" -> Http4sServerInterpreter[IO]().toRoutes(api.createTransaction)).orNotFound
      val request = (signature: String) =>
        Request[IO](
          method = Method.POST,
          uri = uri"/api/transactions"
        ).withEntity(
          TransactionRequest(
            source = source,
            destination = destination,
            amount = amount,
            signature = TransactionSignature.unsafeFrom(signature)
          ).asJson
        )

      val result = for {
        signature <- signer.sign(keyPairSource.publicKey, keyPairDestination.publicKey, amount.value, keyPairSource.privateKey)
        result    <- service.run(request(signature))
        response  <- result.decodeJson[TransactionResponse]
      } yield (result.status, response)

      val (status, response) = result.unsafeRunSync()
      status shouldBe Status.Created
      response.source shouldBe source
      response.destination shouldBe destination
      response.amount shouldBe amount
      response.timestamp should be > start
      response.timestamp should be <= Instant.now()
    }

  }

  private def resource = for {
    keyPairGenerator   <- KeyPairGenerator.resource[IO]
    keyPairSource      <- Resource.eval(keyPairGenerator.generate)
    keyPairDestination <- Resource.eval(keyPairGenerator.generate)
    signer             <- TransactionSigner.resource[IO]
    generator          <- BlockHashGenerator.resource[IO](2)
    store              <- BlockchainStore.resource[IO](2)(keyPairSource.publicKey, 100, keyPairSource.privateKey)(signer, generator)
    validator          <- TransactionValidator.resource[IO](signer, store)
  } yield (keyPairSource, keyPairDestination, store, validator, signer)

}
