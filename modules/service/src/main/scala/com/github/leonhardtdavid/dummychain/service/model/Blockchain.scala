package com.github.leonhardtdavid.dummychain.service.model

import cats.data.NonEmptyList
import com.github.leonhardtdavid.dummychain.service.model.Block.{ BlockHash, Nonce, Sequence }

case class Blockchain(blocks: NonEmptyList[Block])

object Blockchain {

  def genesis(transaction: Transaction, nonce: Nonce, hash: BlockHash): Blockchain =
    Blockchain(
      NonEmptyList.one(
        Block(
          sequence = Sequence.unsafeFrom(0L),
          transactions = NonEmptyList.one(transaction),
          nonce = nonce,
          hash = hash
        )
      )
    )

}
