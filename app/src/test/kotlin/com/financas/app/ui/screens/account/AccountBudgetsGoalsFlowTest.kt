package com.financas.app.ui.screens.account

import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.repository.BackupSerializer
import com.financas.app.ui.fakes.FakeBudgetDao
import com.financas.app.ui.fakes.FakeCategoryDao
import com.financas.app.ui.fakes.FakeContextTagsStore
import com.financas.app.ui.fakes.FakeGoalDao
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AccountBudgetsGoalsFlowTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var budgetDao: FakeBudgetDao
    private lateinit var goalDao: FakeGoalDao
    private lateinit var contextTagsStore: FakeContextTagsStore

    private val parentCategory = CategoryEntity(
        id = "exp_housing",
        parentId = null,
        name = "Habitação",
        icon = "home",
        colorHex = 0xFF1976D2,
        type = "EXPENSE",
        isDefaultFixed = true,
        sortOrder = 1
    )

    private val subCategory = CategoryEntity(
        id = "exp_housing_rent",
        parentId = "exp_housing",
        name = "Renda / Prestação",
        icon = "real_estate_agent",
        colorHex = 0xFF1976D2,
        type = "EXPENSE",
        isDefaultFixed = true,
        sortOrder = 2
    )

    private val todayYearMonth = 202610

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        categoryDao = FakeCategoryDao(listOf(parentCategory, subCategory))
        transactionDao = FakeTransactionDao()
        budgetDao = FakeBudgetDao()
        goalDao = FakeGoalDao()
        contextTagsStore = FakeContextTagsStore()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): AccountViewModel = AccountViewModel(
        categoryDao = categoryDao,
        transactionDao = transactionDao,
        budgetDao = budgetDao,
        goalDao = goalDao,
        biometricLockGate = BiometricLockGate(FakeBiometricPreferenceStore(enabled = false)),
        contextTagsStore = contextTagsStore,
        backupSerializer = BackupSerializer(),
        todayYearMonth = { todayYearMonth }
    )

    @Test
    fun test1_createBudget_usesDeterministicIdAndExactMessage() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.addBudget("exp_housing", 50000L)
        advanceUntilIdle()

        val budget = budgetDao.getById("budget_exp_housing")
        assertNotNull(budget)
        assertEquals("exp_housing", budget?.categoryId)
        assertEquals(50000L, budget?.limitCents)
        assertTrue(budget?.isActive == true)
        assertEquals(
            "Orçamento para \"Habitação\" definido: 500,00 €.",
            viewModel.uiState.value.message
        )
        assertEquals(1, viewModel.uiState.value.budgets.size)
        assertTrue(viewModel.uiState.value.budgetedCategoryIds.contains("exp_housing"))
    }

    @Test
    fun test2_createBudgetForSubcategory_usesTheSubcategoryNameInTheMessage() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.addBudget("exp_housing_rent", 30000L)
        advanceUntilIdle()

        assertEquals(30000L, budgetDao.getById("budget_exp_housing_rent")?.limitCents)
        assertEquals(
            "Orçamento para \"Renda / Prestação\" definido: 300,00 €.",
            viewModel.uiState.value.message
        )
    }

    @Test
    fun test3_secondBudgetForTheSameCategory_ReplacesTheFirst() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.addBudget("exp_housing", 50000L)
        advanceUntilIdle()
        viewModel.addBudget("exp_housing", 75000L)
        advanceUntilIdle()

        assertEquals(1, budgetDao.count())
        assertEquals(1, viewModel.uiState.value.budgets.size)
        assertEquals(75000L, budgetDao.getById("budget_exp_housing")?.limitCents)
        assertEquals(
            "Orçamento para \"Habitação\" definido: 750,00 €.",
            viewModel.uiState.value.message
        )
    }

    @Test
    fun test4_editBudgetLimit_updatesTheStoredValueAndKeepsTheId() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.addBudget("exp_housing", 50000L)
        advanceUntilIdle()

        val budget = budgetDao.getById("budget_exp_housing")!!
        viewModel.updateBudgetLimit(budget, 120000L)
        advanceUntilIdle()

        assertEquals(1, budgetDao.count())
        assertEquals("budget_exp_housing", budgetDao.getById("budget_exp_housing")?.id)
        assertEquals(120000L, budgetDao.getById("budget_exp_housing")?.limitCents)
        assertEquals(
            "Orçamento para \"Habitação\" definido: 1.200,00 €.",
            viewModel.uiState.value.message
        )
    }

    @Test
    fun test5_deleteBudget_removesItFromTheDaoAndFromTheBudgetedCategories() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.addBudget("exp_housing", 50000L)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.budgetedCategoryIds.contains("exp_housing"))

        viewModel.deleteBudget(budgetDao.getById("budget_exp_housing")!!)
        advanceUntilIdle()

        assertEquals(0, budgetDao.count())
        assertTrue(viewModel.uiState.value.budgets.isEmpty())
        assertFalse(viewModel.uiState.value.budgetedCategoryIds.contains("exp_housing"))
        assertEquals("Orçamento eliminado.", viewModel.uiState.value.message)
    }

    @Test
    fun test6_createGoalWithoutDeadline_keepsTheGoalWithoutDeadline() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.createGoal("Fundo de Emergência", 200000L)
        advanceUntilIdle()

        val goal = goalDao.getAllSync().single()
        assertEquals("Fundo de Emergência", goal.name)
        assertEquals(200000L, goal.targetCents)
        assertEquals(todayYearMonth, goal.startYearMonth)
        assertNull(goal.deadlineYearMonth)
        assertFalse(goal.archived)
        assertEquals("Meta \"Fundo de Emergência\" criada.", viewModel.uiState.value.message)
    }

    @Test
    fun test7_createGoalWithDeadline_keepsTheDeadline() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.createGoal("Viagem ao Japão", 300000L, 202712)
        advanceUntilIdle()

        val goal = goalDao.getAllSync().single()
        assertEquals(202712, goal.deadlineYearMonth)
        assertEquals("Meta \"Viagem ao Japão\" criada.", viewModel.uiState.value.message)
    }

    @Test
    fun test8_createGoalWithDeadlineOnTheStartingMonth_isAccepted() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.createGoal("Casa", 500000L, todayYearMonth)
        advanceUntilIdle()

        assertEquals(1, goalDao.count())
        assertEquals(todayYearMonth, goalDao.getAllSync().single().deadlineYearMonth)
    }

    @Test
    fun test9_creatingTheSameGoalTwice_replacesTheStoredTarget() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.createGoal("Fundo de Emergência", 200000L)
        advanceUntilIdle()
        viewModel.createGoal("Fundo de Emergência", 250000L)
        advanceUntilIdle()

        assertEquals(1, goalDao.count())
        val goal = goalDao.getAllSync().single()
        assertEquals(250000L, goal.targetCents)
        assertEquals(GoalEntity.deterministicId("Fundo de Emergência", todayYearMonth), goal.id)
    }

    @Test
    fun test10_archiveGoal_marksItArchivedWithTheExactMessage() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.createGoal("Fundo de Emergência", 200000L)
        advanceUntilIdle()

        val goal = goalDao.getAllSync().single()
        viewModel.archiveGoal(goal)
        advanceUntilIdle()

        assertTrue(goalDao.getById(goal.id)?.archived == true)
        assertEquals("Meta \"Fundo de Emergência\" arquivada.", viewModel.uiState.value.message)
    }

    @Test
    fun test11_deleteGoal_removesItWithTheExactMessage() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.createGoal("Fundo de Emergência", 200000L)
        advanceUntilIdle()

        val goal = goalDao.getAllSync().single()
        viewModel.deleteGoal(goal)
        advanceUntilIdle()

        assertEquals(0, goalDao.count())
        assertTrue(viewModel.uiState.value.goalItems.isEmpty())
        assertEquals("Meta eliminada.", viewModel.uiState.value.message)
    }

    @Test
    fun test12_goalItems_exposeTheAccumulatedSavingsOfTheCurrentPeriod() = runTest {
        transactionDao.insertAll(
            listOf(
                TransactionEntity(
                    id = 1L, type = "INCOME", amountCents = 150000L, yearMonth = 202610,
                    epochDay = LocalDate.of(2026, 10, 1).toEpochDay(),
                    categoryId = "inc_base_salary", contextTag = "TRABALHO",
                    merchant = "Salário", paymentMethod = "TRANSFER", isFixed = true
                ),
                TransactionEntity(
                    id = 2L, type = "EXPENSE", amountCents = 100000L, yearMonth = 202610,
                    epochDay = LocalDate.of(2026, 10, 5).toEpochDay(),
                    categoryId = "exp_housing_rent", contextTag = "CASA",
                    merchant = "Senhorio", paymentMethod = "TRANSFER", isFixed = true
                )
            )
        )

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.createGoal("Fundo de Emergência", 200000L)
        advanceUntilIdle()

        val items = viewModel.uiState.value.goalItems
        assertEquals(1, items.size)
        assertEquals("Fundo de Emergência", items[0].goal.name)
        assertEquals(50000L, items[0].progress.accumulatedSavingsCents)
        assertEquals(25.0, items[0].progress.percentComplete, 0.001)
        assertEquals(50000L, items[0].progress.averageMonthlySavingsCents)
        assertEquals(3L, items[0].progress.projectedMonthsToComplete)
        assertNull(items[0].progress.monthsRemaining)
        assertNull(items[0].progress.onTrack)
    }

    @Test
    fun test13_budgetsAndGoalsSurviveAnExportImportCycle() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.addBudget("exp_housing", 50000L)
        viewModel.createGoal("Fundo de Emergência", 200000L, 202710)
        advanceUntilIdle()

        val exported = viewModel.exportJson()
        assertTrue(exported.contains("\"budgets\": ["))
        assertTrue(exported.contains("\"goals\": ["))

        budgetDao.deleteById("budget_exp_housing")
        goalDao.deleteById(goalDao.getAllSync().single().id)
        assertEquals(0, budgetDao.count())
        assertEquals(0, goalDao.count())

        val result = viewModel.importJson(exported)
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().budgetsImported)
        assertEquals(1, result.getOrThrow().goalsImported)
        assertEquals(50000L, budgetDao.getById("budget_exp_housing")?.limitCents)
        assertEquals(200000L, goalDao.getAllSync().single().targetCents)
    }
}
