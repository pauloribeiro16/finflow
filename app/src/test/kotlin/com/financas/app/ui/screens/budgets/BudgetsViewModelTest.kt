package com.financas.app.ui.screens.budgets

import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.domain.model.InvestmentType
import com.financas.app.domain.usecase.BudgetProgressCalculator
import com.financas.app.domain.usecase.GoalProgressCalculator
import com.financas.app.ui.fakes.FakeBudgetDao
import com.financas.app.ui.fakes.FakeCategoryDao
import com.financas.app.ui.fakes.FakeGoalDao
import com.financas.app.ui.fakes.FakeInvestmentAssetDao
import com.financas.app.ui.fakes.FakeTransactionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var budgetDao: FakeBudgetDao
    private lateinit var goalDao: FakeGoalDao
    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var investmentAssetDao: FakeInvestmentAssetDao

    private val categoryGroceries = CategoryEntity(
        id = "exp_groceries",
        parentId = null,
        name = "Supermercado",
        icon = "shopping_cart",
        colorHex = 0xFF4CAF50,
        type = "EXPENSE",
        isDefaultFixed = false,
        sortOrder = 1
    )

    private val categoryLeisure = CategoryEntity(
        id = "exp_leisure",
        parentId = null,
        name = "Lazer",
        icon = "sports_esports",
        colorHex = 0xFFFF9800,
        type = "EXPENSE",
        isDefaultFixed = false,
        sortOrder = 2
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        budgetDao = FakeBudgetDao()
        goalDao = FakeGoalDao()
        categoryDao = FakeCategoryDao(listOf(categoryGroceries, categoryLeisure))
        transactionDao = FakeTransactionDao()
        investmentAssetDao = FakeInvestmentAssetDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): BudgetsViewModel {
        return BudgetsViewModel(
            budgetDao = budgetDao,
            goalDao = goalDao,
            categoryDao = categoryDao,
            transactionDao = transactionDao,
            investmentAssetDao = investmentAssetDao,
            budgetProgressCalculator = BudgetProgressCalculator(),
            goalProgressCalculator = GoalProgressCalculator()
        )
    }

    @Test
    fun initialUiStateLoadsBudgetsAndGoals() = runTest {
        val now = LocalDate.now()
        val currentYm = now.year * 100 + now.monthValue

        budgetDao.insert(
            BudgetEntity(
                id = "b1",
                categoryId = "exp_groceries",
                limitCents = 40000L,
                isActive = true
            )
        )
        transactionDao.insert(
            TransactionEntity(
                id = 1L,
                amountCents = 15000L,
                type = "EXPENSE",
                categoryId = "exp_groceries",
                contextTag = "daily",
                merchant = "Continente",
                paymentMethod = "DEBIT",
                epochDay = now.toEpochDay(),
                yearMonth = currentYm,
                isFixed = false
            )
        )
        goalDao.insert(
            GoalEntity(
                id = "g1",
                name = "Fundo de Emergência",
                targetCents = 100000L,
                startYearMonth = currentYm,
                deadlineYearMonth = null,
                archived = false
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(currentYm, state.selectedYearMonth)
        assertEquals(40000L, state.totalLimitCents)
        assertEquals(15000L, state.totalSpentCents)
        assertEquals(25000L, state.totalRemainingCents)
        assertEquals(1, state.categoryBudgets.size)
        assertEquals(1, state.goals.size)
        assertEquals("Fundo de Emergência", state.goals[0].name)
    }

    @Test
    fun monthNavigationUpdatesState() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val startYm = viewModel.uiState.value.selectedYearMonth

        viewModel.selectNextMonth()
        advanceUntilIdle()
        val nextYm = viewModel.uiState.value.selectedYearMonth
        assertTrue(nextYm > startYm)

        viewModel.selectPreviousMonth()
        advanceUntilIdle()
        assertEquals(startYm, viewModel.uiState.value.selectedYearMonth)
    }

    @Test
    fun saveBudgetAndGoalMutatesDaos() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.saveBudget("exp_leisure", 20000L)
        viewModel.saveGoal("Viagem Japão", 300000L, null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.categoryBudgets.any { it.categoryId == "exp_leisure" })
        assertTrue(state.goals.any { it.name == "Viagem Japão" })
    }

    @Test
    fun toggleRolloverChangesFlag() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.rolloverEnabled)
        viewModel.toggleRollover()
        assertTrue(viewModel.uiState.value.rolloverEnabled)
    }

    @Test
    fun selectTab_switchesBetweenInvestmentsGoalsAndBudgets() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(WealthTab.INVESTMENTS, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(WealthTab.GOALS)
        advanceUntilIdle()
        assertEquals(WealthTab.GOALS, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(WealthTab.BUDGETS)
        advanceUntilIdle()
        assertEquals(WealthTab.BUDGETS, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun saveInvestmentAsset_insertsAssetAndCalculatesPortfolio() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.saveInvestmentAsset(
            id = "ppr_1",
            name = "PPR Alves Ribeiro",
            type = "PPR",
            institution = "Banco Invest",
            investedCents = 200000L,
            currentValueCents = 250000L,
            monthlyContributionCents = 15000L,
            notes = "Benefício fiscal"
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.investmentAssets.size)
        assertEquals("PPR Alves Ribeiro", state.investmentAssets[0].name)
        assertEquals(200000L, state.portfolioSummary.totalInvestedCents)
        assertEquals(250000L, state.portfolioSummary.totalCurrentValueCents)
        assertEquals(50000L, state.portfolioSummary.totalProfitLossCents)
        assertEquals(25.0, state.portfolioSummary.totalProfitLossPercentage, 0.001)
        assertEquals(15000L, state.portfolioSummary.totalMonthlyContributionCents)
    }

    @Test
    fun updateAssetValue_updatesPortfolioCurrentValueAndProfit() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.saveInvestmentAsset(
            id = "etf_1",
            name = "VWCE",
            type = "ETF",
            institution = "Degiro",
            investedCents = 100000L,
            currentValueCents = 110000L
        )
        advanceUntilIdle()

        viewModel.updateAssetValue("etf_1", 130000L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(130000L, state.portfolioSummary.totalCurrentValueCents)
        assertEquals(30000L, state.portfolioSummary.totalProfitLossCents)
        assertEquals(30.0, state.portfolioSummary.totalProfitLossPercentage, 0.001)
    }

    @Test
    fun selectAssetTypeFilter_filtersDisplayedAssets() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.saveInvestmentAsset(
            id = "ppr_1",
            name = "PPR Stoik",
            type = "PPR",
            institution = "Banco BiG",
            investedCents = 100000L,
            currentValueCents = 120000L
        )
        viewModel.saveInvestmentAsset(
            id = "etf_1",
            name = "VWCE",
            type = "ETF",
            institution = "Degiro",
            investedCents = 100000L,
            currentValueCents = 110000L
        )
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.filteredInvestmentAssets.size)

        viewModel.selectAssetTypeFilter(InvestmentType.PPR)
        advanceUntilIdle()

        val pprFiltered = viewModel.uiState.value.filteredInvestmentAssets
        assertEquals(1, pprFiltered.size)
        assertEquals("PPR Stoik", pprFiltered[0].name)

        viewModel.selectAssetTypeFilter(null)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.filteredInvestmentAssets.size)
    }

    @Test
    fun deleteInvestmentAsset_removesAssetFromPortfolio() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.saveInvestmentAsset(
            id = "dep_1",
            name = "DP 3.5%",
            type = "DEPOSITO_PRAZO",
            institution = "CGD",
            investedCents = 500000L,
            currentValueCents = 500000L
        )
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.investmentAssets.size)

        viewModel.deleteInvestmentAsset("dep_1")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.investmentAssets.isEmpty())
        assertEquals(0L, viewModel.uiState.value.portfolioSummary.totalCurrentValueCents)
    }
}
