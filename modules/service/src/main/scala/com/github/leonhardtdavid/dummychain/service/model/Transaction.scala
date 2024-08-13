package com.github.leonhardtdavid.dummychain.service.model

import com.github.leonhardtdavid.dummychain.service.model.Transaction.{ Amount, DestinationAddress, SourceAddress, TransactionSignature }
import eu.timepit.refined.api._
import eu.timepit.refined.collection._
import eu.timepit.refined.numeric._

import java.time.Instant

case class Transaction(
  source: SourceAddress,
  destination: DestinationAddress,
  amount: Amount,
  signature: TransactionSignature,
  timestamp: Instant
)

object Transaction {

  type SourceAddress = String Refined NonEmpty
  object SourceAddress extends RefinedTypeOps[SourceAddress, String]

  type DestinationAddress = String Refined NonEmpty
  object DestinationAddress extends RefinedTypeOps[DestinationAddress, String]

  type Amount = BigDecimal Refined Positive
  object Amount extends RefinedTypeOps[Amount, BigDecimal]

  type TransactionSignature = String Refined NonEmpty
  object TransactionSignature extends RefinedTypeOps[TransactionSignature, String]

}
