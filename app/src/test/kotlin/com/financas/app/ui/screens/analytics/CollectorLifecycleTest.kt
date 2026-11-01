package com.financas.app.ui.screens.analytics

import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.seed.DefaultSeed
import com.financas.app.domain.usecase.MonthlyAnalyticsCalculator
import com.financas.app.ui.fakes.FakeBudgetDao
import com.financas.app.ui.fakes.FakeCategoryDao
import com.financas.app.ui.fakes.FakeGoalDao
import com.financas.app.ui.fakes.CountingTransactionDao
import com.financas.app.ui.screens.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CollectorLifecycleTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var transactionDao: CountingTransactionDao

    private val yearMonth = 202610

    private val transactions = listOf(
        TransactionEntity(
            id = 1L,
            type = "INCOME",
            amountCents = 200000L,
            yearMonth = yearMonth,
            epochDay = LocalDate.of(2026, 10, 1).toEpochDay(),
            categoryId = "inc_base_salary",
            contextTag = "TRABALHO",
            merchant = "Empresa",
            paymentMethod = "TRANSFER",
            isFixed = true
        ),
        TransactionEntity(
            id = 2L,
            type = "EXPENSE",
            amountCents = 50000L,
            yearMonth = yearMonth,
            epochDay = LocalDate.of(2026, 10, 12).toEpochDay(),
            categoryId = "exp_food_supermarket",
            contextTag = "CASA",
            merchant = "Continente",
            paymentMethod = "CARD",
            isFixed = false
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        categoryDao = FakeCategoryDao(DefaultSeed.getDefaultCategories())
        transactionDao = CountingTransactionDao(transactions)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildAnalyticsViewModel() = AnalyticsViewModel(
        transactionDao = transactionDao,
        categoryDao = categoryDao,
        monthlyAnalyticsCalculator = MonthlyAnalyticsCalculator(),
        initialYearMonth = yearMonth
    )

    private fun buildHomeViewModel() = HomeViewModel(
        transactionDao = transactionDao,
        categoryDao = categoryDao,
        budgetDao = FakeBudgetDao(),
        goalDao = FakeGoalDao(),
        monthlyAnalyticsCalculator = MonthlyAnalyticsCalculator(),
        initialYearMonth = yearMonth
    )

    @Test
    fun test1_analyticsViewModel_KeepsASingleMonthCollectorAfterCreation() = runTest {
        val viewModel = buildAnalyticsViewModel()
        advanceUntilIdle()

        assertEquals(1, transactionDao.activeSubscriptions)
        assertEquals(1, transactionDao.totalSubscriptions)
        assertEquals(50000L, viewModel.uiState.value.totalExpenseCents)
    }

    @Test
    fun test2_analyticsViewModel_MonthChangesDoNotAccumulateCollectors() = runTest {
        val viewModel = buildAnalyticsViewModel()
        advanceUntilIdle()

        val activeAfterInit = transactionDao.activeSubscriptions
        val totalAfterInit = transactionDao.totalSubscriptions
        assertEquals(1, activeAfterInit)

        val months = listOf(202609, 202608, 202607, 202606, 202605)
        months.forEach { targetMonth ->
            viewModel.setYearMonth(targetMonth)
            advanceUntilIdle()
            assertEquals(
                "Mês $targetMonth não pode acumular coletores",
                activeAfterInit,
                transactionDao.activeSubscriptions
            )
        }

        assertEquals(totalAfterInit + months.size, transactionDao.totalSubscriptions)
    }

    @Test
    fun test3_analyticsViewModel_MonthChangesStillDeliverTheNewMonthData() = runTest {
        val viewModel = buildAnalyticsViewModel()
        advanceUntilIdle()

        viewModel.setYearMonth(202609)
        advanceUntilIdle()

        assertEquals(202609, viewModel.uiState.value.selectedYearMonth)
        assertEquals(0L, viewModel.uiState.value.totalExpenseCents)
        assertEquals(1, transactionDao.activeSubscriptions)

        viewModel.setYearMonth(202610)
        advanceUntilIdle()

        assertEquals(50000L, viewModel.uiState.value.totalExpenseCents)
        assertEquals(1, transactionDao.activeSubscriptions)
    }

    @Test
    fun test4_analyticsViewModel_SelectPreviousAndNextMonthDoesNotLeakCollectors() = runTest {
        val viewModel = buildAnalyticsViewModel()
        advanceUntilIdle()

        repeat(4) {
            viewModel.selectPreviousMonth()
            advanceUntilIdle()
            viewModel.selectNextMonth()
            advanceUntilIdle()
        }

        assertEquals(1, transactionDao.activeSubscriptions)
        assertEquals(1 + 8, transactionDao.totalSubscriptions)
    }

    @Test
    fun test5_homeViewModel_KeepsASingleMonthCollectorAfterCreation() = runTest {
        val viewModel = buildHomeViewModel()
        advanceUntilIdle()

        assertEquals(1, transactionDao.activeSubscriptions)
        assertEquals(50000L, viewModel.uiState.value.monthlySummary.totalExpenseCents)
    }

    @Test
    fun test6_homeViewModel_MonthChangesDoNotAccumulateCollectors() = runTest {
        val viewModel = buildHomeViewModel()
        advanceUntilIdle()

        val activeAfterInit = transactionDao.activeSubscriptions
        val totalAfterInit = transactionDao.totalSubscriptions
        assertEquals(1, activeAfterInit)

        val months = listOf(202609, 202608, 202607, 202606, 202605)
        months.forEach { targetMonth ->
            viewModel.setYearMonth(targetMonth)
            advanceUntilIdle()
            assertEquals(
                "Mês $targetMonth não pode acumular coletores",
                activeAfterInit,
                transactionDao.activeSubscriptions
            )
        }

        assertEquals(totalAfterInit + months.size, transactionDao.totalSubscriptions)
    }

    @Test
    fun test7_homeViewModel_MonthChangesStillDeliverTheNewMonthData() = runTest {
        val viewModel = buildHomeViewModel()
        advanceUntilIdle()

        viewModel.selectNextMonth()
        advanceUntilIdle()

        assertEquals(202611, viewModel.uiState.value.selectedYearMonth)
        assertEquals(0L, viewModel.uiState.value.monthlySummary.totalExpenseCents)
        assertEquals(1, transactionDao.activeSubscriptions)

        viewModel.selectPreviousMonth()
        advanceUntilIdle()

        assertEquals(50000L, viewModel.uiState.value.monthlySummary.totalExpenseCents)
        assertEquals(1, transactionDao.activeSubscriptions)
    }

    @Test
    fun test8_bothViewModels_KeepExactlyOneCollectorWhileTheMonthKeepsChanging() = runTest {
        val analytics = buildAnalyticsViewModel()
        val home = buildHomeViewModel()
        advanceUntilIdle()

        assertEquals(2, transactionDao.activeSubscriptions)

        repeat(5) {
            analytics.setYearMonth(202609)
            home.setYearMonth(202609)
            advanceUntilIdle()
        }

        assertEquals(2, transactionDao.activeSubscriptions)
        assertTrue(transactionDao.totalSubscriptions > 2)
    }
}
