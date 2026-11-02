package com.financas.app.data.seed

import com.financas.app.ui.fakes.FakeBudgetDao
import com.financas.app.ui.fakes.FakeCategoryDao
import com.financas.app.ui.fakes.FakeGoalDao
import com.financas.app.ui.fakes.FakeInvestmentAssetDao
import com.financas.app.ui.fakes.FakeTransactionDao
import com.financas.app.ui.fakes.FakeUserProfileStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoDataSeederTest {

    @Test
    fun testSeedDemoDataPopulatesAllTables() = runTest {
        val categoryDao = FakeCategoryDao()
        val transactionDao = FakeTransactionDao()
        val budgetDao = FakeBudgetDao()
        val goalDao = FakeGoalDao()
        val investmentAssetDao = FakeInvestmentAssetDao()
        val userProfileStore = FakeUserProfileStore()

        DemoDataSeeder.seedDemoData(
            categoryDao = categoryDao,
            transactionDao = transactionDao,
            budgetDao = budgetDao,
            goalDao = goalDao,
            investmentAssetDao = investmentAssetDao,
            userProfileStore = userProfileStore,
            clearExisting = true
        )

        // Verify categories
        val categories = categoryDao.getAll().first()
        assertTrue("Categories should not be empty", categories.isNotEmpty())

        // Verify transactions
        val transactions = transactionDao.getAll().first()
        assertTrue("Transactions should have at least 20 items", transactions.size >= 20)
        assertTrue("All transactions must have positive amount", transactions.all { it.amountCents > 0L })

        // Verify fixed income and fixed expenses are present
        val hasSalary = transactions.any { it.categoryId == "inc_base_salary" && it.isFixed }
        assertTrue("Should have base salary", hasSalary)

        val hasRent = transactions.any { it.categoryId == "exp_housing_rent" && it.isFixed }
        assertTrue("Should have rent expense", hasRent)

        // Verify budgets
        val budgets = budgetDao.getAll().first()
        assertEquals(4, budgets.size)

        // Verify goals
        val goals = goalDao.getAll().first()
        assertEquals(2, goals.size)

        // Verify investment assets
        val assets = investmentAssetDao.getAll().first()
        assertEquals(3, assets.size)
        val hasPpr = assets.any { it.type == "PPR" }
        assertTrue("Should have a PPR asset", hasPpr)

        // Verify user profile
        assertEquals("Paulo (Demonstração)", userProfileStore.getUserName())
        assertEquals("EUR (€)", userProfileStore.getDefaultCurrency())
    }
}
