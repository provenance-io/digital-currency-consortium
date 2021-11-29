package io.provenance.digitalcurrency.consortium.domain

import io.provenance.digitalcurrency.consortium.extension.toUSDAmount
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import java.time.OffsetDateTime
import java.util.UUID

typealias CRT = CoinRedemptionTable

object CoinRedemptionTable : BaseRequestTable(name = "coin_redemption") {
    val coinAmount = long("coin_amount")
    val fiatAmount = decimal("fiat_amount", 12, 2)
    val addressRegistration = reference("address_registration_uuid", AddressRegistrationTable)
}

open class CoinRedemptionEntityClass : BaseRequestEntityClass<CRT, CoinRedemptionRecord>(CRT) {
    fun insert(
        addressRegistration: AddressRegistrationRecord,
        coinAmount: Long
    ) = super.insert(UUID.randomUUID()).apply {
        this.addressRegistration = addressRegistration
        this.coinAmount = coinAmount
        fiatAmount = coinAmount.toBigInteger().toUSDAmount()
    }

    fun updateStatus(uuid: UUID, newStatus: TxStatus) =
        findById(uuid)!!.let {
            it.status = newStatus
            it.updated = OffsetDateTime.now()
        }

    fun findByTxHash(txHash: String) = find { CRT.txHash eq txHash }.toList()

    fun findPending() =
        find { CRT.status inList listOf(TxStatus.QUEUED, TxStatus.TXN_COMPLETE) }

    fun findPendingForUpdate(uuid: UUID) =
        find { (CRT.id eq uuid) and (CRT.status inList listOf(TxStatus.QUEUED, TxStatus.TXN_COMPLETE)) }.forUpdate()

    fun findExpiredForUpdate() = find {
        (CRT.created lessEq OffsetDateTime.now().minusSeconds(30))
            .and(CRT.status eq TxStatus.PENDING)
            .and(CRT.txHash.isNotNull())
    }.forUpdate().toList()
}

class CoinRedemptionRecord(uuid: EntityID<UUID>) : BaseRequestRecord(CRT, uuid) {
    companion object : CoinRedemptionEntityClass()

    var coinAmount by CRT.coinAmount
    var fiatAmount by CRT.fiatAmount
    var addressRegistration by AddressRegistrationRecord referencedOn CRT.addressRegistration
}
