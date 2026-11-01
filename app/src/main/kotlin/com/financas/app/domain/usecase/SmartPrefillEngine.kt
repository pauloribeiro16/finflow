package com.financas.app.domain.usecase

import com.financas.app.data.db.TransactionDao
import com.financas.app.data.entity.TransactionEntity

data class PrefillSuggestion(
    val categoryId: String,
    val contextTag: String,
    val paymentMethod: String,
    val isFixed: Boolean,
    val merchant: String
)

class SmartPrefillEngine(
    private val transactionDao: TransactionDao? = null
) {

    fun findPrefill(merchantQuery: String, history: List<TransactionEntity>): PrefillSuggestion? {
        val trimmed = merchantQuery.trim()
        if (trimmed.isEmpty()) return null

        val match = history
            .filter { it.merchant.trim().equals(trimmed, ignoreCase = true) }
            .maxWithOrNull(compareBy({ it.epochDay }, { it.id }))

        return match?.let {
            PrefillSuggestion(
                categoryId = it.categoryId,
                contextTag = it.contextTag,
                paymentMethod = it.paymentMethod,
                isFixed = it.isFixed,
                merchant = it.merchant
            )
        }
    }

    suspend fun findPrefill(merchantQuery: String): PrefillSuggestion? {
        val trimmed = merchantQuery.trim()
        if (trimmed.isEmpty() || transactionDao == null) return null

        val entity = transactionDao.getLatestByMerchant(trimmed)
        return entity?.let {
            PrefillSuggestion(
                categoryId = it.categoryId,
                contextTag = it.contextTag,
                paymentMethod = it.paymentMethod,
                isFixed = it.isFixed,
                merchant = it.merchant
            )
        }
    }

    fun getRecentMerchants(history: List<TransactionEntity>, limit: Int = 10): List<String> {
        return history
            .filter { it.merchant.isNotBlank() }
            .sortedWith(compareByDescending<TransactionEntity> { it.epochDay }.thenByDescending { it.id })
            .map { it.merchant.trim() }
            .distinct()
            .take(limit)
    }
}
