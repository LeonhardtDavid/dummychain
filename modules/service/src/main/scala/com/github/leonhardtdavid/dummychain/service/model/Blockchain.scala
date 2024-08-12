package com.github.leonhardtdavid.dummychain.service.model

import cats.data.NonEmptyChain
import com.github.leonhardtdavid.dummychain.service.model.Block.{ BlockHash, Nonce, Sequence }

case class Blockchain(blocks: NonEmptyChain[Block])

object Blockchain {

  def genesis(transaction: Transaction, nonce: Nonce, hash: BlockHash): Blockchain =
    Blockchain(
      NonEmptyChain.one(
        Block(
          sequence = Sequence.unsafeFrom(0L),
          transactions = NonEmptyChain.one(transaction),
          nonce = nonce,
          hash = hash
        )
      )
    )

}
