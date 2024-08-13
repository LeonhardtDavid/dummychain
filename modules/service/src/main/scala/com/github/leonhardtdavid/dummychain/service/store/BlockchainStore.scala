package com.github.leonhardtdavid.dummychain.service.store

import cats.data.{ EitherT, NonEmptyList }
import cats.effect.kernel.Ref.Make
import cats.effect.kernel.Resource
import cats.effect.{ Ref, Sync }
import cats.implicits._
import com.github.leonhardtdavid.dummychain.service.model.Block.{ BlockHash, Nonce, Sequence }
import com.github.leonhardtdavid.dummychain.service.model.Transaction.{ Amount, DestinationAddress, SourceAddress, TransactionSignature }
import com.github.leonhardtdavid.dummychain.service.model.{ Block, Blockchain, Transaction }
import com.github.leonhardtdavid.dummychain.service.store.BlockchainStore.StoreError
import com.github.leonhardtdavid.dummychain.shared.BlockHashGenerator.GeneratedHash
import com.github.leonhardtdavid.dummychain.shared.{ BlockHashGenerator, TransactionSigner }

import java.time.Instant
import scala.annotation.tailrec

class BlockchainStore[F[_]: Sync](maxTransactionsPerBlock: Int)(store: Ref[F, Blockchain], generator: BlockHashGenerator[F]) {

  private def updateBlockchain(
    f: GeneratedHash => Blockchain
  ): PartialFunction[Either[Throwable, GeneratedHash], Either[StoreError, Blockchain]] = {
    case Right(generatedHash) => f(generatedHash).asRight
    case Left(e)              => StoreError(e).asLeft
  }

  private def add(transaction: Transaction, blockchain: Blockchain) = {
    val currentBlock = blockchain.blocks.head
    val transactions = currentBlock.transactions

    if (transactions.size <= maxTransactionsPerBlock) {
      val ts = transactions.append(transaction)
      generator
        .generate(
          currentBlock.sequence.value,
          ts.map(t => t.signature.value -> t.timestamp).toList,
          currentBlock.previousBlock.map(_.value)
        )
        .attempt
        .map(updateBlockchain { generatedHash =>
          val updatedBlock = currentBlock.copy(
            transactions = ts,
            nonce = Nonce.unsafeFrom(generatedHash.nonce),
            hash = BlockHash.unsafeFrom(generatedHash.hash)
          )
          Blockchain(NonEmptyList(updatedBlock, blockchain.blocks.tail))
        })
    } else {
      val newSequence = currentBlock.sequence.value + 1
      generator
        .generate(
          newSequence,
          (transaction.signature.value -> transaction.timestamp) :: Nil,
          currentBlock.hash.value.some
        )
        .attempt
        .map(updateBlockchain { generatedHash =>
          val updatedBlock = Block(
            sequence = Sequence.unsafeFrom(newSequence),
            transactions = NonEmptyList.one(transaction),
            nonce = Nonce.unsafeFrom(generatedHash.nonce),
            hash = BlockHash.unsafeFrom(generatedHash.hash)
          )
          Blockchain(NonEmptyList(updatedBlock, blockchain.blocks.toList))
        })
    }
  }

  def addTransaction(transaction: Transaction): F[Either[StoreError, Unit]] = {
    val eitherT = for {
      blockchain <- EitherT.right[StoreError](store.get)
      updated    <- EitherT(add(transaction, blockchain))
      _          <- EitherT.right[StoreError](store.set(updated))
    } yield ()

    eitherT.value
  }

  def hasSufficientFunds(transaction: Transaction): F[Boolean] = {
    @tailrec
    def validate(blocks: List[Block], acc: BigDecimal): Boolean =
      blocks match {
        case Nil => acc >= transaction.amount.value
        case b :: bs =>
          val newAcc = b.transactions.collect {
            case t if t.destination.value == transaction.source.value => t.amount.value
            case t if t.source.value == transaction.source.value      => -t.amount.value
          }
            .foldLeft(acc)(_ + _)
          validate(bs, newAcc)
      }

    store.get.map(blockchain => validate(blockchain.blocks.toList, BigDecimal(0)))
  }

}

object BlockchainStore {

  case class StoreError(error: Throwable)

  def resource[F[_]: Sync: Make](maxTransactionsPerBlock: Int)(source: String, initialAmount: BigDecimal, privateKey: String)(
    signer: TransactionSigner[F],
    generator: BlockHashGenerator[F]
  ): Resource[F, BlockchainStore[F]] =
    Resource.eval {
      for {
        signature <- signer.sign(source, source, initialAmount, privateKey)
        transaction = Transaction(
          source = SourceAddress.unsafeFrom(source),
          destination = DestinationAddress.unsafeFrom(source),
          amount = Amount.unsafeFrom(initialAmount),
          signature = TransactionSignature.unsafeFrom(signature),
          timestamp = Instant.now()
        )
        generated <- generator.generate(0L, (transaction.signature.value, transaction.timestamp) :: Nil, None)
        blockchain = Blockchain.genesis(
          transaction,
          Nonce.unsafeFrom(generated.nonce),
          BlockHash.unsafeFrom(generated.hash)
        )
        ref <- Ref.of[F, Blockchain](blockchain)
      } yield new BlockchainStore[F](maxTransactionsPerBlock)(ref, generator)
    }

}
