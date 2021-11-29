package io.provenance.digitalcurrency.consortium.stream

import io.provenance.digitalcurrency.consortium.annotation.NotTest
import io.provenance.digitalcurrency.consortium.config.logger
import io.provenance.digitalcurrency.consortium.domain.CoinRedemptionRecord
import io.provenance.digitalcurrency.consortium.domain.TxRequestViewRecord
import io.provenance.digitalcurrency.consortium.extension.isFailed
import io.provenance.digitalcurrency.consortium.service.PbcService
import io.provenance.digitalcurrency.consortium.service.TxRequestService
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@NotTest
@Component
class ExpiredEventReaper(private val pbcService: PbcService, private val txRequestService: TxRequestService) {

    private val log = logger()

    @Scheduled(
        initialDelayString = "\${event_stream.expiration.initial_delay.ms}",
        fixedDelayString = "\${event_stream.expiration.delay.ms}"
    )
    fun pollExpiredTransactions() {
        transaction { TxRequestViewRecord.findExpired().groupBy { it.txHash } }.forEach { (txHash, uuids) ->
            log.info("Handling $txHash for ids $uuids from expired transaction reaper")

            val response = pbcService.getTransaction(txHash!!)
            when {
                response == null -> log.info("no tx response, wait?")
                response.txResponse.height > 0 -> txRequestService.completeTxns(txHash)
                response.txResponse.isFailed() -> txRequestService.resetTxns(txHash, response.txResponse.height)
            }
        }

        // these cannot be batched and therefore are not in the above view, must be handled separately
        transaction {
            CoinRedemptionRecord.findExpiredForUpdate().forEach { coinRedemption ->
                val response = pbcService.getTransaction(coinRedemption.txHash!!)
                when {
                    response == null -> log.info("no tx response, wait?")
                    response.txResponse.height > 0 -> coinRedemption.updateToTxnComplete()
                    response.txResponse.isFailed() -> coinRedemption.resetForRetry(response.txResponse.height)
                }
            }
        }
    }
}