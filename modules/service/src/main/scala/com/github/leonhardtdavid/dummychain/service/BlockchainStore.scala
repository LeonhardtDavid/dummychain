package com.github.leonhardtdavid.dummychain.service

import cats.data.NonEmptyChain
import cats.effect.kernel.Ref.Make
import cats.effect.{ Ref, Sync }
import cats.effect.kernel.Resource
import cats.implicits._
import com.github.leonhardtdavid.dummychain.service.model.Block.{ BlockHash, Nonce, Sequence }
import com.github.leonhardtdavid.dummychain.service.model.Transaction.{ Amount, DestinationAddress, SourceAddress, TransactionSignature }
import com.github.leonhardtdavid.dummychain.service.model.{ Block, Blockchain, Transaction }
import com.github.leonhardtdavid.dummychain.shared.{ BlockHashGenerator, TransactionSigner }

// TODO make store private
class BlockchainStore[F[_]: Sync](maxTransactionsPerBlock: Int)(val store: Ref[F, Blockchain], generator: BlockHashGenerator[F]) {

  def addTransaction(transaction: Transaction): F[Unit] =
    store.get.flatMap { blockchain =>
      val currentBlock = blockchain.blocks.head
      val transactions = currentBlock.transactions

      if (transactions.size <= maxTransactionsPerBlock) {
        val ts = transactions.append(transaction)
        generator
          .generate(
            currentBlock.sequence.value,
            ts.map(_.signature.value).toList,
            currentBlock.previousBlock.map(_.value)
          )
          .map { generatedHash =>
            val updatedBlock = currentBlock.copy(
              transactions = ts,
              nonce = Nonce.unsafeFrom(generatedHash.nonce),
              hash = BlockHash.unsafeFrom(generatedHash.hash)
            )
            Blockchain(NonEmptyChain.fromChainPrepend(updatedBlock, blockchain.blocks.tail))
          }
      } else {
        val newSequence = currentBlock.sequence.value + 1
        generator
          .generate(
            newSequence,
            transaction.signature.value :: Nil,
            currentBlock.hash.value.some
          )
          .map { generatedHash =>
            val updatedBlock = Block(
              sequence = Sequence.unsafeFrom(newSequence),
              transactions = NonEmptyChain.one(transaction),
              nonce = Nonce.unsafeFrom(generatedHash.nonce),
              hash = BlockHash.unsafeFrom(generatedHash.hash)
            )
            Blockchain(NonEmptyChain.fromChainPrepend(updatedBlock, blockchain.blocks.toChain))
          }
      }
    }.flatMap { blockchain =>
      store.set(blockchain)
    }

}

object BlockchainStore {

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
          signature = TransactionSignature.unsafeFrom(signature)
        )
        generated <- generator.generate(0L, transaction.signature.value :: Nil, None)
        blockchain = Blockchain.genesis(
          transaction,
          Nonce.unsafeFrom(generated.nonce),
          BlockHash.unsafeFrom(generated.hash)
        )
        ref <- Ref.of[F, Blockchain](blockchain)
      } yield new BlockchainStore[F](maxTransactionsPerBlock)(ref, generator)
    }

}
