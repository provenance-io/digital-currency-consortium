package io.provenance.digitalcurrency.consortium.service

import io.provenance.digitalcurrency.consortium.config.logger
import io.provenance.digitalcurrency.consortium.domain.AddressDeregistrationRecord
import io.provenance.digitalcurrency.consortium.domain.AddressRegistrationRecord
import io.provenance.digitalcurrency.consortium.domain.BaseRequestRecord
import io.provenance.digitalcurrency.consortium.domain.CoinBurnRecord
import io.provenance.digitalcurrency.consortium.domain.CoinMintRecord
import io.provenance.digitalcurrency.consortium.domain.CoinTransferRecord
import io.provenance.digitalcurrency.consortium.domain.TxRequestType
import io.provenance.digitalcurrency.consortium.domain.TxRequestViewRecord
import io.provenance.digitalcurrency.consortium.domain.TxStatus
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TxRequestService {
    private val log = logger()

    fun getBaseRequest(uuid: UUID, type: TxRequestType): BaseRequestRecord = when (type) {
        TxRequestType.TRANSFER -> CoinTransferRecord.findById(uuid)!!
        TxRequestType.MINT -> CoinMintRecord.findById(uuid)!!
        TxRequestType.BURN -> CoinBurnRecord.findById(uuid)!!
        TxRequestType.TAG -> AddressRegistrationRecord.findById(uuid)!!
        TxRequestType.DETAG -> AddressDeregistrationRecord.findById(uuid)!!
    }

    fun completeTxns(txHash: String) {
        transaction {
            TxRequestViewRecord.findByTxHash(txHash).forEach {
                when (it.status) {
                    TxStatus.PENDING -> getBaseRequest(it.id.value, it.type).updateToTxnComplete()
                    else -> log.error("Txn ${it.id} has invalid status ${it.status} to complete the txn")
                }
            }
        }
    }

    fun resetTxns(txHash: String) {
        transaction {
            TxRequestViewRecord.findByTxHash(txHash).forEach {
                when (it.status) {
                    TxStatus.PENDING -> getBaseRequest(it.id.value, it.type).resetForRetry()
                    else -> log.error("Txn ${it.id} has invalid status ${it.status} for a txn error")
                }
            }
        }
    }
}
