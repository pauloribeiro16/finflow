package com.financas.app.domain.usecase

import com.financas.app.data.entity.PendingTransactionEntity
import com.financas.app.data.entity.TransactionEntity

sealed class DeduplicationResult {
    data class DuplicateDrop(val reason: String) : DeduplicationResult()
    data class MatchesExistingFixed(
        val matchedFixedTransaction: TransactionEntity,
        val suggestedCategoryId: String,
        val suggestedContextTag: String
    ) : DeduplicationResult()
    data class NewUnique(
        val isPotentialDuplicate: Boolean = false,
        val note: String = ""
    ) : DeduplicationResult()
}

class TransactionDeduplicator(
    private val debounceWindowMs: Long = 180_000L // 3 minutes
) {

    fun evaluate(
        incomingAmountCents: Long,
        incomingMerchant: String,
        incomingSourceApp: String,
        incomingTimestamp: Long,
        todayEpochDay: Long,
        currentYearMonth: Int,
        existingPending: List<PendingTransactionEntity>,
        monthFixedTransactions: List<TransactionEntity>,
        todayConfirmedTransactions: List<TransactionEntity>
    ): DeduplicationResult {
        // 1. Debounce check against pending queue (e.g. MB WAY + Moey duplicates)
        val duplicatePending = existingPending.firstOrNull { pending ->
            pending.amountCents == incomingAmountCents &&
                kotlin.math.abs(incomingTimestamp - pending.receivedTimestamp) <= debounceWindowMs
        }
        if (duplicatePending != null) {
            return DeduplicationResult.DuplicateDrop(
                "Duplicado descartado: o mesmo valor foi capturado via ${duplicatePending.sourceApp} há menos de 3 minutos."
            )
        }

        // 2. Check if a transaction with the exact amount & merchant was already confirmed today
        val confirmedDuplicate = todayConfirmedTransactions.firstOrNull { tx ->
            tx.epochDay == todayEpochDay &&
                tx.amountCents == incomingAmountCents &&
                isMerchantMatch(incomingMerchant, tx.merchant)
        }
        if (confirmedDuplicate != null) {
            return DeduplicationResult.DuplicateDrop(
                "Já registado: despesa de ${confirmedDuplicate.merchant} já foi confirmada hoje."
            )
        }

        // 3. Match against current month's fixed expenses (protection against duplicating recurring expenses)
        val matchingFixed = monthFixedTransactions.firstOrNull { fixed ->
            fixed.isFixed && (
                (fixed.amountCents == incomingAmountCents && isMerchantMatch(incomingMerchant, fixed.merchant)) ||
                (fixed.amountCents == incomingAmountCents && isGenericOrUnknown(incomingMerchant))
            )
        }
        if (matchingFixed != null) {
            return DeduplicationResult.MatchesExistingFixed(
                matchedFixedTransaction = matchingFixed,
                suggestedCategoryId = matchingFixed.categoryId,
                suggestedContextTag = matchingFixed.contextTag
            )
        }

        return DeduplicationResult.NewUnique()
    }

    private fun isMerchantMatch(a: String, b: String): Boolean {
        val cleanA = a.trim().lowercase()
        val cleanB = b.trim().lowercase()
        if (cleanA.isBlank() || cleanB.isBlank()) return false
        return cleanA == cleanB || cleanA.contains(cleanB) || cleanB.contains(cleanA)
    }

    private fun isGenericOrUnknown(merchant: String): Boolean {
        val clean = merchant.trim().lowercase()
        return clean.isBlank() ||
            clean.contains("desconhecido") ||
            clean.contains("comerciante") ||
            clean.contains("pagamento") ||
            clean.startsWith("recebimento ")
    }
}
