package com.financas.app.data.seed

import com.financas.app.domain.model.InvestmentType
import com.financas.app.ui.fakes.FakeBudgetDao
import com.financas.app.ui.fakes.FakeCategoryDao
import com.financas.app.ui.fakes.FakeGoalDao
import com.financas.app.ui.fakes.FakeInvestmentAssetDao
import com.financas.app.ui.fakes.FakePendingTransactionDao
import com.financas.app.ui.fakes.FakeTransactionDao
import com.financas.app.ui.fakes.FakeUserProfileStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DemoDataSeederTest {

    @Test
    fun testSeedDemoDataPopulatesAllTables() = runTest {
        val categoryDao = FakeCategoryDao()
        val transactionDao = FakeTransactionDao()
        val budgetDao = FakeBudgetDao()
        val goalDao = FakeGoalDao()
        val investmentAssetDao = FakeInvestmentAssetDao()
        val pendingTransactionDao = FakePendingTransactionDao()
        val userProfileStore = FakeUserProfileStore()

        DemoDataSeeder.seedDemoData(
            categoryDao = categoryDao,
            transactionDao = transactionDao,
            budgetDao = budgetDao,
            goalDao = goalDao,
            investmentAssetDao = investmentAssetDao,
            userProfileStore = userProfileStore,
            pendingTransactionDao = pendingTransactionDao,
            clearExisting = true
        )

        // 1. Verify categories
        val categories = categoryDao.getAll().first()
        assertTrue("Categories should not be empty", categories.isNotEmpty())

        // 2. Verify transactions volume and span across years
        val transactions = transactionDao.getAll().first()
        assertTrue("Transactions should have over 500 items for multi-year analytics", transactions.size > 500)
        assertTrue("All transactions must have positive amounts", transactions.all { it.amountCents > 0L })

        // Verify multi-year span (2024, 2025, 2026)
        val years = transactions.map { it.yearMonth / 100 }.distinct()
        assertTrue("Should contain 2024", years.contains(2024))
        assertTrue("Should contain 2025", years.contains(2025))
        assertTrue("Should contain 2026", years.contains(2026))

        // Verify weekly distribution in recent month
        val currentYearMonth = 202611
        val monthTxs = transactions.filter { it.yearMonth == currentYearMonth && it.type == "EXPENSE" }
        val week1 = monthTxs.any { LocalDate.ofEpochDay(it.epochDay).dayOfMonth in 1..7 }
        val week2 = monthTxs.any { LocalDate.ofEpochDay(it.epochDay).dayOfMonth in 8..14 }
        val week3 = monthTxs.any { LocalDate.ofEpochDay(it.epochDay).dayOfMonth in 15..21 }
        val week4 = monthTxs.any { LocalDate.ofEpochDay(it.epochDay).dayOfMonth >= 22 }
        assertTrue("Should have week 1 expenses", week1)
        assertTrue("Should have week 2 expenses", week2)
        assertTrue("Should have week 3 expenses", week3)
        assertTrue("Should have week 4 expenses", week4)

        // 3. Verify budgets (6 budgets)
        val budgets = budgetDao.getAll().first()
        assertEquals(6, budgets.size)

        // 4. Verify goals (4 goals)
        val goals = goalDao.getAll().first()
        assertEquals(4, goals.size)

        // 5. Verify investment assets (8 assets covering all 6 types)
        val assets = investmentAssetDao.getAll().first()
        assertEquals(8, assets.size)
        val typesRepresented = assets.map { InvestmentType.fromKey(it.type) }.toSet()
        assertEquals("All 6 investment types must be represented in demo data", 6, typesRepresented.size)

        // 6. Verify pending transactions (2 items)
        val pending = pendingTransactionDao.getAll().first()
        assertEquals(2, pending.size)

        // 7. Verify user profile
        assertEquals("Paulo (Demonstração)", userProfileStore.getUserName())
        assertEquals("EUR (€)", userProfileStore.getDefaultCurrency())
    }
}
