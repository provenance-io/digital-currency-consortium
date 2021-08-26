package io.provenance.digitalcurrency.consortium.frameworks

import io.provenance.digitalcurrency.consortium.config.BankClientProperties
import io.provenance.digitalcurrency.consortium.config.CoroutineProperties
import io.provenance.digitalcurrency.consortium.config.logger
import io.provenance.digitalcurrency.consortium.domain.AddressRegistrationRecord
import io.provenance.digitalcurrency.consortium.domain.BalanceEntryRecord
import io.provenance.digitalcurrency.consortium.domain.BalanceReportRecord
import io.provenance.digitalcurrency.consortium.service.PbcService
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.UUID

class BalanceReportDirective(
    override val id: UUID
) : Directive()

class BalanceReportOutcome(
    override val id: UUID
) : Outcome()

@Component
class BalanceReportQueue(
    bankClientProperties: BankClientProperties,
    coroutineProperties: CoroutineProperties,
    private val pbcService: PbcService,
) : ActorModel<BalanceReportDirective, BalanceReportOutcome> {

    private val log = logger()
    private val denom = bankClientProperties.denom

    @EventListener(DataSourceConnectedEvent::class)
    fun startProcessing() {
        log.info("start balance report framework")
        start()
    }

    override val numWorkers: Int = coroutineProperties.numWorkers.toInt()
    override val pollingDelayMillis: Long = coroutineProperties.pollingDelayMs.toLong()

    override suspend fun loadMessages(): List<BalanceReportDirective> =
        transaction {
            BalanceReportRecord.findPending().map { BalanceReportDirective(it.id.value) }
        }

    override fun processMessage(message: BalanceReportDirective): BalanceReportOutcome {
        transaction {
            val balanceReport = BalanceReportRecord.findForUpdate(message.id).first()
            if (balanceReport.completed != null)
                return@transaction // already processed

            // TODO - paginate addresses query
            val addresses = AddressRegistrationRecord.all().map { it.address }

            addresses.forEach { address ->
                BalanceEntryRecord.insert(
                    report = balanceReport,
                    address = address,
                    denom = denom,
                    // TODO - retryable?
                    amount = pbcService.getCoinBalance(address)
                )
            }

            balanceReport.markCompleted()
        }

        return BalanceReportOutcome(message.id)
    }

    override fun onMessageSuccess(result: BalanceReportOutcome) {
        log.info("balance report queue successfully processed uuid ${result.id}.")
    }

    override fun onMessageFailure(message: BalanceReportDirective, e: Exception) {
        log.error("balance report queue got error for uuid ${message.id}", e)
    }
}
