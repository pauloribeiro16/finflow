package com.financas.app.domain.usecase

import com.financas.app.data.entity.PendingTransactionEntity
import com.financas.app.data.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionDeduplicatorTest {

    private val deduplicator = TransactionDeduplicator(debounceWindowMs = 180_000L) // 3 mins

    @Test
    fun simultaneousNotifications_within3Minutes_droppedAsDuplicate() {
        val now = 1000_000L
        val existingPending = listOf(
            PendingTransactionEntity(
                id = 1,
                amountCents = 1250L,
                rawMerchant = "Continente",
                suggestedCategoryId = "sub_groceries",
                suggestedContextTag = "CASA",
                suggestedPaymentMethod = "CARD",
                isFixed = false,
                sourceApp = "Google Wallet",
                detectedEpochDay = 20000L,
                receivedTimestamp = now - 5000L // 5 seconds ago
            )
        )

        // Moey notification fires 5 seconds later for same 12,50 €
        val result = deduplicator.evaluate(
            incomingAmountCents = 1250L,
            incomingMerchant = "Continente Bom Dia",
            incomingSourceApp = "Moey",
            incomingTimestamp = now,
            todayEpochDay = 20000L,
            currentYearMonth = 202610,
            existingPending = existingPending,
            monthFixedTransactions = emptyList(),
            todayConfirmedTransactions = emptyList()
        )

        assertTrue(result is DeduplicationResult.DuplicateDrop)
    }

    @Test
    fun notifications_differentAmounts_notDropped() {
        val now = 1000_000L
        val existingPending = listOf(
            PendingTransactionEntity(
                id = 1,
                amountCents = 1250L,
                rawMerchant = "Continente",
                suggestedCategoryId = "sub_groceries",
                suggestedContextTag = "CASA",
                suggestedPaymentMethod = "CARD",
                isFixed = false,
                sourceApp = "Google Wallet",
                detectedEpochDay = 20000L,
                receivedTimestamp = now - 5000L
            )
        )

        val result = deduplicator.evaluate(
            incomingAmountCents = 350L,
            incomingMerchant = "Café Central",
            incomingSourceApp = "MB WAY",
            incomingTimestamp = now,
            todayEpochDay = 20000L,
            currentYearMonth = 202610,
            existingPending = existingPending,
            monthFixedTransactions = emptyList(),
            todayConfirmedTransactions = emptyList()
        )

        assertTrue(result is DeduplicationResult.NewUnique)
    }

    @Test
    fun notifications_afterDebounceWindow_allowed() {
        val now = 1000_000L
        val existingPending = listOf(
            PendingTransactionEntity(
                id = 1,
                amountCents = 1250L,
                rawMerchant = "Continente",
                suggestedCategoryId = "sub_groceries",
                suggestedContextTag = "CASA",
                suggestedPaymentMethod = "CARD",
                isFixed = false,
                sourceApp = "Google Wallet",
                detectedEpochDay = 20000L,
                receivedTimestamp = now - 300_000L // 5 minutes ago (outside 3m window)
            )
        )

        val result = deduplicator.evaluate(
            incomingAmountCents = 1250L,
            incomingMerchant = "Outra Loja",
            incomingSourceApp = "Moey",
            incomingTimestamp = now,
            todayEpochDay = 20000L,
            currentYearMonth = 202610,
            existingPending = existingPending,
            monthFixedTransactions = emptyList(),
            todayConfirmedTransactions = emptyList()
        )

        assertTrue(result is DeduplicationResult.NewUnique)
    }

    @Test
    fun matchesExistingFixedExpense_flagsAsFixed() {
        val fixedVodafone = TransactionEntity(
            id = 42,
            type = "EXPENSE",
            amountCents = 3490L,
            yearMonth = 202610,
            epochDay = 20001L,
            categoryId = "sub_telecom",
            contextTag = "CASA",
            merchant = "Vodafone",
            paymentMethod = "DIRECT_DEBIT",
            isFixed = true
        )

        val result = deduplicator.evaluate(
            incomingAmountCents = 3490L,
            incomingMerchant = "Vodafone Portugal",
            incomingSourceApp = "MB WAY",
            incomingTimestamp = 1000_000L,
            todayEpochDay = 20001L,
            currentYearMonth = 202610,
            existingPending = emptyList(),
            monthFixedTransactions = listOf(fixedVodafone),
            todayConfirmedTransactions = emptyList()
        )

        assertTrue(result is DeduplicationResult.MatchesExistingFixed)
        val matched = result as DeduplicationResult.MatchesExistingFixed
        assertEquals("sub_telecom", matched.suggestedCategoryId)
        assertEquals("CASA", matched.suggestedContextTag)
        assertEquals(42L, matched.matchedFixedTransaction.id)
    }

    @Test
    fun alreadyConfirmedToday_droppedAsDuplicate() {
        val confirmedToday = TransactionEntity(
            id = 99,
            type = "EXPENSE",
            amountCents = 450L,
            yearMonth = 202610,
            epochDay = 20000L,
            categoryId = "sub_coffee",
            contextTag = "TRABALHO",
            merchant = "Padaria Central",
            paymentMethod = "MBWAY",
            isFixed = false
        )

        val result = deduplicator.evaluate(
            incomingAmountCents = 450L,
            incomingMerchant = "Padaria Central",
            incomingSourceApp = "Moey",
            incomingTimestamp = 1000_000L,
            todayEpochDay = 20000L,
            currentYearMonth = 202610,
            existingPending = emptyList(),
            monthFixedTransactions = emptyList(),
            todayConfirmedTransactions = listOf(confirmedToday)
        )

        assertTrue(result is DeduplicationResult.DuplicateDrop)
    }
}
