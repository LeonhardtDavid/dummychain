package com.github.leonhardtdavid.dummychain.service.store

import cats.data.{ EitherNel, ValidatedNel }
import cats.effect.Sync
import cats.effect.kernel.Resource
import cats.implicits._
import com.github.leonhardtdavid.dummychain.service.model.Transaction
import com.github.leonhardtdavid.dummychain.service.store.TransactionValidator._
import com.github.leonhardtdavid.dummychain.shared.TransactionSigner

trait TransactionValidator[F[_]] {
  def validate(transaction: Transaction): F[EitherNel[TransactionValidationError, Unit]]
}

class TransactionValidatorImpl[F[_]: Sync](signer: TransactionSigner[F], store: BlockchainStore[F]) extends TransactionValidator[F] {

  private def validateTransactionSign(transaction: Transaction): F[ValidatedNel[TransactionValidationError, Unit]] =
    signer
      .validate(
        transaction.source.value,
        transaction.destination.value,
        transaction.amount.value,
        transaction.signature.value,
        transaction.source.value
      )
      .attempt
      .map {
        case Left(_) | Right(false) => InvalidSign.invalidNel
        case _                      => ().validNel
      }

  private def validateSufficientFunds(transaction: Transaction): F[ValidatedNel[TransactionValidationError, Unit]] =
    store.hasSufficientFunds(transaction).map { result =>
      if (result) ().validNel
      else InsufficientFunds.invalidNel
    }

  override def validate(transaction: Transaction): F[EitherNel[TransactionValidationError, Unit]] =
    for {
      v1 <- validateTransactionSign(transaction)
      v2 <- validateSufficientFunds(transaction)
    } yield (v1 |+| v2).toEither // TODO it doesn't make sense to check funds if the signature is invalid

}

object TransactionValidator {

  sealed trait TransactionValidationError
  case object InvalidSign       extends TransactionValidationError
  case object InsufficientFunds extends TransactionValidationError

  def resource[F[_]: Sync](signer: TransactionSigner[F], store: BlockchainStore[F]): Resource[F, TransactionValidator[F]] =
    Resource.pure(new TransactionValidatorImpl[F](signer, store))

}
