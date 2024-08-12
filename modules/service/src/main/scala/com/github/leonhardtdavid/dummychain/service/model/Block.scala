package com.github.leonhardtdavid.dummychain.service.model

import cats.data.NonEmptyChain
import com.github.leonhardtdavid.dummychain.service.model.Block.{ BlockHash, Nonce, Sequence }
import eu.timepit.refined.api._
import eu.timepit.refined.numeric._
import eu.timepit.refined.string._

case class Block(
  sequence: Sequence,
  transactions: NonEmptyChain[Transaction],
  nonce: Nonce,
  hash: BlockHash,
  previousBlock: Option[BlockHash] = None
)

object Block {

  type Sequence = Long Refined NonNegative
  object Sequence extends RefinedTypeOps[Sequence, Long]

  type Nonce = Long Refined NonNegative
  object Nonce extends RefinedTypeOps[Nonce, Long]

  type BlockHash = String Refined HexStringSpec
  object BlockHash extends RefinedTypeOps[BlockHash, String]

}
