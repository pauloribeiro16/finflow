package com.financas.app.ui.screens.home

import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.seed.DefaultSeed
import com.financas.app.domain.usecase.MonthlyAnalyticsCalculator
import com.financas.app.domain.usecase.MonthProjectionCalculator
import com.financas.app.ui.fakes.FakeBudgetDao
import com.financas.app.ui.fakes.FakeCategoryDao
import com.financas.app.ui.fakes.FakeGoalDao
import com.financas.app.ui.fakes.FakeTransactionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var budgetDao: FakeBudgetDao
    private lateinit var goalDao: FakeGoalDao
    private lateinit var analyticsCalculator: MonthlyAnalyticsCalculator

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        categoryDao = FakeCategoryDao(DefaultSeed.getDefaultCategories())
        budgetDao = FakeBudgetDao()
        goalDao = FakeGoalDao()
        analyticsCalculator = MonthlyAnalyticsCalculator()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun criterion3_effortBarShares_sumTo100WhenExpenseWithinIncome() = runTest {
        // Recebido: 1000 € (100_000L cêntimos)
        // Fixos: 400 € (40_000L cêntimos) -> 40,0%
        // Variáveis: 350 € (35_000L cêntimos) -> 35,0%
        // Poupança: 250 € (25_000L cêntimos) -> 25,0%
        val yearMonth = 202610
        val epochDay = LocalDate.of(2026, 10, 15).toEpochDay()

        val transactions = listOf(
            TransactionEntity(
                id = 1,
                type = "INCOME",
                amountCents = 100000L,
                yearMonth = yearMonth,
                epochDay = epochDay,
                categoryId = "inc_base_salary",
                contextTag = "TRABALHO",
                merchant = "Salário",
                paymentMethod = "TRANSFER",
                isFixed = true
            ),
            TransactionEntity(
                id = 2,
                type = "EXPENSE",
                amountCents = 40000L,
                yearMonth = yearMonth,
                epochDay = epochDay,
                categoryId = "exp_housing_rent",
                contextTag = "CASA",
                merchant = "Senhorio",
                paymentMethod = "TRANSFER",
                isFixed = true
            ),
            TransactionEntity(
                id = 3,
                type = "EXPENSE",
                amountCents = 35000L,
                yearMonth = yearMonth,
                epochDay = epochDay,
                categoryId = "exp_food_supermarket",
                contextTag = "COMERCIO",
                merchant = "Continente",
                paymentMethod = "MBWAY",
                isFixed = false
            )
        )

        transactionDao = FakeTransactionDao(transactions)

        val viewModel = HomeViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            budgetDao = budgetDao,
            goalDao = goalDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = yearMonth
        )

        val state = viewModel.uiState.value
        val summary = state.monthlySummary

        assertEquals(100000L, summary.totalIncomeCents)
        assertEquals(75000L, summary.totalExpenseCents)
        assertEquals(40000L, summary.fixedExpenseCents)
        assertEquals(35000L, summary.variableExpenseCents)
        assertEquals(25000L, summary.netSavingsCents)

        assertEquals(40.0, summary.fixedExpenseIncomeRatioPercent, 0.001)
        assertEquals(35.0, summary.variableExpenseIncomeRatioPercent, 0.001)
        assertEquals(25.0, summary.savingsRatePercent, 0.001)

        val sum = summary.fixedExpenseIncomeRatioPercent + summary.variableExpenseIncomeRatioPercent + summary.savingsRatePercent
        assertEquals(100.0, sum, 0.001)
    }

    @Test
    fun monthNavigation_updatesSelectedMonth() = runTest {
        transactionDao = FakeTransactionDao()

        val viewModel = HomeViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            budgetDao = budgetDao,
            goalDao = goalDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202610
        )

        assertEquals(202610, viewModel.uiState.value.selectedYearMonth)
        assertEquals("Outubro 2026", viewModel.uiState.value.monthTitle)

        viewModel.selectNextMonth()
        assertEquals(202611, viewModel.uiState.value.selectedYearMonth)
        assertEquals("Novembro 2026", viewModel.uiState.value.monthTitle)

        viewModel.selectPreviousMonth()
        assertEquals(202610, viewModel.uiState.value.selectedYearMonth)
        assertEquals("Outubro 2026", viewModel.uiState.value.monthTitle)
    }

    @Test
    fun monthNavigation_acrossYearBoundary() = runTest {
        transactionDao = FakeTransactionDao()

        val viewModel = HomeViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            budgetDao = budgetDao,
            goalDao = goalDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202601
        )

        assertEquals(202601, viewModel.uiState.value.selectedYearMonth)
        assertEquals("Janeiro 2026", viewModel.uiState.value.monthTitle)

        viewModel.selectPreviousMonth()
        assertEquals(202512, viewModel.uiState.value.selectedYearMonth)
        assertEquals("Dezembro 2025", viewModel.uiState.value.monthTitle)

        viewModel.selectNextMonth()
        assertEquals(202601, viewModel.uiState.value.selectedYearMonth)
        assertEquals("Janeiro 2026", viewModel.uiState.value.monthTitle)

        viewModel.selectNextMonth()
        assertEquals(202602, viewModel.uiState.value.selectedYearMonth)
        assertEquals("Fevereiro 2026", viewModel.uiState.value.monthTitle)
    }

    @Test
    fun deleteTransaction_removesRecordFromDao() = runTest {
        val txs = listOf(
            TransactionEntity(
                id = 1, type = "EXPENSE", amountCents = 5000L, yearMonth = 202610,
                epochDay = LocalDate.of(2026, 10, 1).toEpochDay(),
                categoryId = "exp_food_supermarket", contextTag = "CASA", merchant = "Continente",
                paymentMethod = "CARD", isFixed = false
            ),
            TransactionEntity(
                id = 2, type = "EXPENSE", amountCents = 8000L, yearMonth = 202610,
                epochDay = LocalDate.of(2026, 10, 2).toEpochDay(),
                categoryId = "exp_food_lunch", contextTag = "TRABALHO", merchant = "Café Central",
                paymentMethod = "MBWAY", isFixed = false
            )
        )
        transactionDao = FakeTransactionDao(txs)

        val viewModel = HomeViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            budgetDao = budgetDao,
            goalDao = goalDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202610
        )

        viewModel.deleteTransaction(1L)

        assertEquals(1, transactionDao.count())
        assertTrue(transactionDao.getAllSync().none { it.id == 1L })
    }

    @Test
    fun recentTransactions_isCappedAtFiveMostRecentFirst() = runTest {
        val txs = (1L..7L).map { i ->
            TransactionEntity(
                id = i, type = "EXPENSE", amountCents = 1000L * i, yearMonth = 202610,
                epochDay = LocalDate.of(2026, 10, i.toInt()).toEpochDay(),
                categoryId = "exp_food_supermarket", contextTag = "CASA", merchant = "Loja $i",
                paymentMethod = "CARD", isFixed = false
            )
        }
        transactionDao = FakeTransactionDao(txs)

        val viewModel = HomeViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            budgetDao = budgetDao,
            goalDao = goalDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202610
        )

        val recent = viewModel.uiState.value.recentTransactions
        assertEquals(5, recent.size)
        assertEquals(listOf("Loja 7", "Loja 6", "Loja 5", "Loja 4", "Loja 3"), recent.map { it.merchant })
    }

    @Test
    fun emptyMonth_zeroTotalsZeroRateAndNoBanner() = runTest {
        transactionDao = FakeTransactionDao()

        val viewModel = HomeViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            budgetDao = budgetDao,
            goalDao = goalDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202612
        )

        val summary = viewModel.uiState.value.monthlySummary
        assertEquals(0L, summary.totalIncomeCents)
        assertEquals(0L, summary.totalExpenseCents)
        assertEquals(0L, summary.netSavingsCents)
        assertEquals(0.0, summary.savingsRatePercent, 0.0)
        assertTrue(viewModel.uiState.value.recentTransactions.isEmpty())
    }

    @Test
    fun criterion8_budgetCard_rollsUpSubcategoriesIntoParentAndOrdersByPercentUsed() = runTest {
        val yearMonth = 202610
        val epochDay = LocalDate.of(2026, 10, 15).toEpochDay()

        val transactions = listOf(
            TransactionEntity(
                id = 1, type = "INCOME", amountCents = 100000L, yearMonth = yearMonth,
                epochDay = epochDay, categoryId = "inc_base_salary", contextTag = "TRABALHO",
                merchant = "Salário", paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 2, type = "EXPENSE", amountCents = 25000L, yearMonth = yearMonth,
                epochDay = epochDay, categoryId = "exp_housing_rent", contextTag = "CASA",
                merchant = "Senhorio", paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 3, type = "EXPENSE", amountCents = 10000L, yearMonth = yearMonth,
                epochDay = epochDay, categoryId = "exp_food_supermarket", contextTag = "COMERCIO",
                merchant = "Continente", paymentMethod = "MBWAY", isFixed = false
            ),
            TransactionEntity(
                id = 4, type = "EXPENSE", amountCents = 22000L, yearMonth = yearMonth,
                epochDay = epochDay, categoryId = "exp_food_lunch", contextTag = "TRABALHO",
                merchant = "Café Central", paymentMethod = "MBWAY", isFixed = false
            )
        )
        transactionDao = FakeTransactionDao(transactions)
        budgetDao = FakeBudgetDao(
            listOf(
                BudgetEntity(id = "budget_exp_food", categoryId = "exp_food", limitCents = 40000L),
                BudgetEntity(id = "budget_exp_housing_rent", categoryId = "exp_housing_rent", limitCents = 30000L)
            )
        )

        val viewModel = HomeViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            budgetDao = budgetDao,
            goalDao = goalDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = yearMonth
        )

        val report = viewModel.uiState.value.budgetReport
        assertEquals(2, report.items.size)

        val rentBudget = report.items[0]
        assertEquals("budget_exp_housing_rent", rentBudget.budgetId)
        assertEquals("Renda / Prestação", rentBudget.categoryName)
        assertEquals(25000L, rentBudget.spentCents)
        assertEquals(30000L, rentBudget.limitCents)
        assertEquals(5000L, rentBudget.remainingCents)
        assertEquals(83.333, rentBudget.percentUsed, 0.001)

        val foodBudget = report.items[1]
        assertEquals("budget_exp_food", foodBudget.budgetId)
        assertEquals("Alimentação", foodBudget.categoryName)
        assertEquals(32000L, foodBudget.spentCents)
        assertEquals(8000L, foodBudget.remainingCents)
        assertEquals(80.0, foodBudget.percentUsed, 0.001)

        assertEquals(70000L, report.totalLimitCents)
        assertEquals(57000L, report.totalSpentCents)
        assertEquals(13000L, report.totalRemainingCents)
    }

    @Test
    fun criterion8_budgetCard_hiddenWhenThereAreNoBudgets() = runTest {
        val yearMonth = 202610
        val epochDay = LocalDate.of(2026, 10, 15).toEpochDay()
        transactionDao = FakeTransactionDao(
            listOf(
                TransactionEntity(
                    id = 1, type = "EXPENSE", amountCents = 5000L, yearMonth = yearMonth,
                    epochDay = epochDay, categoryId = "exp_food_lunch", contextTag = "TRABALHO",
                    merchant = "Café Central", paymentMethod = "MBWAY", isFixed = false
                )
            )
        )

        val viewModel = HomeViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            budgetDao = budgetDao,
            goalDao = goalDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = yearMonth
        )

        assertTrue(viewModel.uiState.value.budgetReport.items.isEmpty())
        assertEquals(0L, viewModel.uiState.value.budgetReport.totalLimitCents)
    }

    @Test
    fun criterion8_paceCard_populatedForCurrentMonthWithExactProjection() = runTest {
        val now = LocalDate.now()
        val yearMonth = now.year * 100 + now.monthValue
        val epochDay = now.toEpochDay()

        val transactions = listOf(
            TransactionEntity(
                id = 1, type = "INCOME", amountCents = 100000L, yearMonth = yearMonth,
                epochDay = epochDay, categoryId = "inc_base_salary", contextTag = "TRABALHO",
                merchant = "Salário", paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 2, type = "EXPENSE", amountCents = 5000L, yearMonth = yearMonth,
                epochDay = epochDay, categoryId = "exp_food_lunch", contextTag = "TRABALHO",
                merchant = "Café Central", paymentMethod = "MBWAY", isFixed = false
            ),
            TransactionEntity(
                id = 3, type = "EXPENSE", amountCents = 4000L, yearMonth = yearMonth,
                epochDay = epochDay, categoryId = "exp_food_supermarket", contextTag = "COMERCIO",
                merchant = "Continente", paymentMethod = "MBWAY", isFixed = false
            )
        )
        transactionDao = FakeTransactionDao(transactions)

        val viewModel = HomeViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            budgetDao = budgetDao,
            goalDao = goalDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = yearMonth
        )

        val projection = viewModel.uiState.value.monthProjection
        val expected = MonthProjectionCalculator().calculate(
            transactions,
            yearMonth,
            LocalDate.now().toEpochDay()
        )

        assertEquals(expected, projection)
    }

    @Test
    fun criterion8_paceCard_hiddenForEmptyMonthOutsideCurrentMonth() = runTest {
        val futureYm = java.time.YearMonth.now().plusMonths(2)
        val yearMonth = futureYm.year * 100 + futureYm.monthValue

        transactionDao = FakeTransactionDao()

        val viewModel = HomeViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            budgetDao = budgetDao,
            goalDao = goalDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = yearMonth
        )

        assertNull(viewModel.uiState.value.monthProjection)
    }

    @Test
    fun criterion8_goalsCard_accumulatesNetSavingsIncludingNegativeMonths() = runTest {
        val goal = GoalEntity(
            id = "goal_1",
            name = "Fundo de Emergência",
            targetCents = 120000L,
            startYearMonth = 202601,
            deadlineYearMonth = null
        )
        goalDao = FakeGoalDao(listOf(goal))

        val transactions = listOf(
            TransactionEntity(
                id = 1, type = "INCOME", amountCents = 150000L, yearMonth = 202601,
                epochDay = LocalDate.of(2026, 1, 5).toEpochDay(),
                categoryId = "inc_base_salary", contextTag = "TRABALHO",
                merchant = "Salário", paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 2, type = "EXPENSE", amountCents = 100000L, yearMonth = 202601,
                epochDay = LocalDate.of(2026, 1, 10).toEpochDay(),
                categoryId = "exp_housing_rent", contextTag = "CASA",
                merchant = "Senhorio", paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 3, type = "INCOME", amountCents = 150000L, yearMonth = 202602,
                epochDay = LocalDate.of(2026, 2, 5).toEpochDay(),
                categoryId = "inc_base_salary", contextTag = "TRABALHO",
                merchant = "Salário", paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 4, type = "EXPENSE", amountCents = 170000L, yearMonth = 202602,
                epochDay = LocalDate.of(2026, 2, 10).toEpochDay(),
                categoryId = "exp_housing_rent", contextTag = "CASA",
                merchant = "Senhorio", paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 5, type = "INCOME", amountCents = 150000L, yearMonth = 202603,
                epochDay = LocalDate.of(2026, 3, 5).toEpochDay(),
                categoryId = "inc_base_salary", contextTag = "TRABALHO",
                merchant = "Salário", paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 6, type = "EXPENSE", amountCents = 100000L, yearMonth = 202603,
                epochDay = LocalDate.of(2026, 3, 10).toEpochDay(),
                categoryId = "exp_housing_rent", contextTag = "CASA",
                merchant = "Senhorio", paymentMethod = "TRANSFER", isFixed = true
            )
        )
        transactionDao = FakeTransactionDao(transactions)

        val viewModel = HomeViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            budgetDao = budgetDao,
            goalDao = goalDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202603
        )

        val goals = viewModel.uiState.value.goalProgress
        assertEquals(1, goals.size)

        val progress = goals[0]
        assertEquals(80000L, progress.accumulatedSavingsCents)
        assertEquals(66.666, progress.percentComplete, 0.001)
        assertEquals(26666L, progress.averageMonthlySavingsCents)
        assertEquals(2L, progress.projectedMonthsToComplete)
        assertNull(progress.monthsRemaining)
        assertNull(progress.onTrack)
    }

    @Test
    fun criterion8_goalsCard_hiddenWhenThereAreNoGoals() = runTest {
        transactionDao = FakeTransactionDao()

        val viewModel = HomeViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            budgetDao = budgetDao,
            goalDao = goalDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = 202610
        )

        assertTrue(viewModel.uiState.value.goalProgress.isEmpty())
    }

    @Test
    fun criterion8_goalsCard_exposesProjectionTextAndTrackBadge() = runTest {
        val goal = GoalEntity(
            id = "goal_fundo_202601",
            name = "Fundo de Emergência",
            targetCents = 120000L,
            startYearMonth = 202601,
            deadlineYearMonth = 202606
        )

        val viewModel = buildGoalViewModel(goal, savingsTransactions(), 202603)
        val progress = viewModel.uiState.value.goalProgress.single()

        assertEquals(2L, progress.projectedMonthsToComplete)
        assertEquals(true, progress.onTrack)
        assertEquals("Previsão: ~2 meses", progress.projectionText)
        assertEquals("No prazo", progress.trackBadgeText)
        assertEquals(3, progress.monthsRemaining)
    }

    @Test
    fun criterion8_goalsCard_marksGoalAsLateWhenProjectionExceedsDeadline() = runTest {
        val goal = GoalEntity(
            id = "goal_fundo_202601",
            name = "Fundo de Emergência",
            targetCents = 120000L,
            startYearMonth = 202601,
            deadlineYearMonth = 202604
        )

        val viewModel = buildGoalViewModel(goal, savingsTransactions(), 202603)
        val progress = viewModel.uiState.value.goalProgress.single()

        assertEquals(2L, progress.projectedMonthsToComplete)
        assertEquals(false, progress.onTrack)
        assertEquals("Previsão: ~2 meses", progress.projectionText)
        assertEquals("Atrasado", progress.trackBadgeText)
        assertEquals(1, progress.monthsRemaining)
    }

    @Test
    fun criterion8_goalsCard_withoutDeadlineHasNoBadgeAndWithoutDataHasNoProjection() = runTest {
        val goalWithoutDeadline = GoalEntity(
            id = "goal_fundo_202601",
            name = "Fundo de Emergência",
            targetCents = 120000L,
            startYearMonth = 202601,
            deadlineYearMonth = null
        )

        val withData = buildGoalViewModel(goalWithoutDeadline, savingsTransactions(), 202603)
            .uiState.value.goalProgress.single()

        assertEquals("Previsão: ~2 meses", withData.projectionText)
        assertNull(withData.trackBadgeText)
        assertNull(withData.onTrack)

        val withoutData = buildGoalViewModel(goalWithoutDeadline, emptyList(), 202603)
            .uiState.value.goalProgress.single()

        assertNull(withoutData.projectedMonthsToComplete)
        assertEquals("Sem dados para projetar", withoutData.projectionText)
        assertNull(withoutData.trackBadgeText)
    }

    @Test
    fun criterion8_goalsCard_withoutDataButWithDeadlineIsLate() = runTest {
        val goal = GoalEntity(
            id = "goal_fundo_202601",
            name = "Fundo de Emergência",
            targetCents = 120000L,
            startYearMonth = 202601,
            deadlineYearMonth = 202606
        )

        val progress = buildGoalViewModel(goal, emptyList(), 202603).uiState.value.goalProgress.single()

        assertNull(progress.projectedMonthsToComplete)
        assertEquals("Sem dados para projetar", progress.projectionText)
        assertEquals(false, progress.onTrack)
        assertEquals("Atrasado", progress.trackBadgeText)
    }

    private fun savingsTransactions(): List<TransactionEntity> = listOf(
        TransactionEntity(
            id = 1, type = "INCOME", amountCents = 150000L, yearMonth = 202601,
            epochDay = LocalDate.of(2026, 1, 5).toEpochDay(),
            categoryId = "inc_base_salary", contextTag = "TRABALHO",
            merchant = "Salário", paymentMethod = "TRANSFER", isFixed = true
        ),
        TransactionEntity(
            id = 2, type = "EXPENSE", amountCents = 100000L, yearMonth = 202601,
            epochDay = LocalDate.of(2026, 1, 10).toEpochDay(),
            categoryId = "exp_housing_rent", contextTag = "CASA",
            merchant = "Senhorio", paymentMethod = "TRANSFER", isFixed = true
        ),
        TransactionEntity(
            id = 3, type = "INCOME", amountCents = 150000L, yearMonth = 202602,
            epochDay = LocalDate.of(2026, 2, 5).toEpochDay(),
            categoryId = "inc_base_salary", contextTag = "TRABALHO",
            merchant = "Salário", paymentMethod = "TRANSFER", isFixed = true
        ),
        TransactionEntity(
            id = 4, type = "EXPENSE", amountCents = 170000L, yearMonth = 202602,
            epochDay = LocalDate.of(2026, 2, 10).toEpochDay(),
            categoryId = "exp_housing_rent", contextTag = "CASA",
            merchant = "Senhorio", paymentMethod = "TRANSFER", isFixed = true
        ),
        TransactionEntity(
            id = 5, type = "INCOME", amountCents = 150000L, yearMonth = 202603,
            epochDay = LocalDate.of(2026, 3, 5).toEpochDay(),
            categoryId = "inc_base_salary", contextTag = "TRABALHO",
            merchant = "Salário", paymentMethod = "TRANSFER", isFixed = true
        ),
        TransactionEntity(
            id = 6, type = "EXPENSE", amountCents = 100000L, yearMonth = 202603,
            epochDay = LocalDate.of(2026, 3, 10).toEpochDay(),
            categoryId = "exp_housing_rent", contextTag = "CASA",
            merchant = "Senhorio", paymentMethod = "TRANSFER", isFixed = true
        )
    )

    private fun buildGoalViewModel(
        goal: GoalEntity,
        transactions: List<TransactionEntity>,
        initialYearMonth: Int
    ): HomeViewModel {
        transactionDao = FakeTransactionDao(transactions)
        goalDao = FakeGoalDao(listOf(goal))
        return HomeViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            budgetDao = budgetDao,
            goalDao = goalDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = initialYearMonth
        )
    }

    private fun buildViewModel(
        transactions: List<TransactionEntity>,
        initialYearMonth: Int
    ): HomeViewModel {
        transactionDao = FakeTransactionDao(transactions)
        return HomeViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            budgetDao = budgetDao,
            goalDao = goalDao,
            monthlyAnalyticsCalculator = analyticsCalculator,
            initialYearMonth = initialYearMonth
        )
    }
}
