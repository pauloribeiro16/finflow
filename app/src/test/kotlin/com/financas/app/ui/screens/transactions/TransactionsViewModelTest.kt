package com.financas.app.ui.screens.transactions

import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.seed.DefaultSeed
import com.financas.app.ui.fakes.FakeCategoryDao
import com.financas.app.ui.fakes.FakeTransactionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class TransactionsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var transactionDao: FakeTransactionDao

    private val sampleYearMonth = 202610
    private val sampleEpoch = LocalDate.of(2026, 10, 10).toEpochDay()

    private val sampleTransactions = listOf(
        // Income
        TransactionEntity(
            id = 1,
            type = "INCOME",
            amountCents = 200000L, // 2.000 €
            yearMonth = sampleYearMonth,
            epochDay = sampleEpoch,
            categoryId = "inc_base_salary",
            contextTag = "TRABALHO",
            merchant = "Salário Tech",
            paymentMethod = "TRANSFER",
            isFixed = true
        ),
        // Expense 1: Continente (Casa, MBWay, Variável)
        TransactionEntity(
            id = 2,
            type = "EXPENSE",
            amountCents = 15000L, // 150 €
            yearMonth = sampleYearMonth,
            epochDay = sampleEpoch,
            categoryId = "exp_food_supermarket",
            contextTag = "CASA",
            merchant = "Continente Colombo",
            paymentMethod = "MBWAY",
            isFixed = false
        ),
        // Expense 2: Pingo Doce (Casa, Card, Variável)
        TransactionEntity(
            id = 3,
            type = "EXPENSE",
            amountCents = 5000L, // 50 €
            yearMonth = sampleYearMonth,
            epochDay = sampleEpoch,
            categoryId = "exp_food_supermarket",
            contextTag = "CASA",
            merchant = "Pingo Doce",
            paymentMethod = "CARD",
            isFixed = false
        ),
        // Expense 3: Renda (Casa, Transfer, Fixo)
        TransactionEntity(
            id = 4,
            type = "EXPENSE",
            amountCents = 60000L, // 600 €
            yearMonth = sampleYearMonth,
            epochDay = sampleEpoch,
            categoryId = "exp_housing_rent",
            contextTag = "CASA",
            merchant = "Senhorio",
            paymentMethod = "TRANSFER",
            isFixed = true
        ),
        // Expense 4: Almoço Trabalho (Trabalho, MBWay, Variável)
        TransactionEntity(
            id = 5,
            type = "EXPENSE",
            amountCents = 1200L, // 12 €
            yearMonth = sampleYearMonth,
            epochDay = sampleEpoch,
            categoryId = "exp_food_lunch",
            contextTag = "TRABALHO",
            merchant = "Cervejaria Central",
            paymentMethod = "MBWAY",
            isFixed = false
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        categoryDao = FakeCategoryDao(DefaultSeed.getDefaultCategories())
        transactionDao = FakeTransactionDao(sampleTransactions)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun criterion4_filterBySearchQueryAndContextAndMethod_recalculatesDynamicSummary() = runTest {
        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        // Initial state: all 5 transactions in October 2026
        // Total Income in period = 200000L (2000 €)
        // Total filtered = 200000 + 15000 + 5000 + 60000 + 1200 = 281200L
        assertEquals(5, viewModel.uiState.value.filteredCount)
        assertEquals(281200L, viewModel.uiState.value.filteredTotalCents)
        assertEquals(200000L, viewModel.uiState.value.periodIncomeCents)

        // Filter by Context "CASA"
        viewModel.onContextFilterChanged("CASA")
        // Transactions with CASA: 2 (Continente 150€), 3 (Pingo Doce 50€), 4 (Renda 600€) = 3 items, 800€ (80000L)
        // 800 € / 2000 € = 40.0% do recebido
        assertEquals(3, viewModel.uiState.value.filteredCount)
        assertEquals(80000L, viewModel.uiState.value.filteredTotalCents)
        assertEquals(40.0, viewModel.uiState.value.filteredIncomeSharePercent, 0.001)

        // Now also filter by Method "MBWAY"
        viewModel.onMethodFilterChanged("MBWAY")
        // Transactions with CASA + MBWAY: only Continente (150€ = 15000L)
        // 150 € / 2000 € = 7.5% do recebido
        assertEquals(1, viewModel.uiState.value.filteredCount)
        assertEquals(15000L, viewModel.uiState.value.filteredTotalCents)
        assertEquals(7.5, viewModel.uiState.value.filteredIncomeSharePercent, 0.001)
        assertEquals("Continente Colombo", viewModel.uiState.value.filteredTransactions.first().merchant)

        // Now search query "colombo" (matches Continente Colombo)
        viewModel.onSearchQueryChanged("colombo")
        assertEquals(1, viewModel.uiState.value.filteredCount)
        assertEquals(15000L, viewModel.uiState.value.filteredTotalCents)

        // Search query "inexistente"
        viewModel.onSearchQueryChanged("inexistente")
        assertEquals(0, viewModel.uiState.value.filteredCount)
        assertEquals(0L, viewModel.uiState.value.filteredTotalCents)
        assertEquals(0.0, viewModel.uiState.value.filteredIncomeSharePercent, 0.001)
    }

    @Test
    fun flowFilter_separatesFixedAndVariableAndIncomes() = runTest {
        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        // Filter FIXED: Salário Tech (1) + Renda (4)
        viewModel.onFlowFilterChanged(FlowFilter.FIXED)
        assertEquals(2, viewModel.uiState.value.filteredCount)
        assertTrue(viewModel.uiState.value.filteredTransactions.all { it.isFixed })

        // Filter VARIABLE: Continente (2) + Pingo Doce (3) + Almoço (5) = 3 items
        viewModel.onFlowFilterChanged(FlowFilter.VARIABLE)
        assertEquals(3, viewModel.uiState.value.filteredCount)
        assertTrue(viewModel.uiState.value.filteredTransactions.all { !it.isFixed && it.type == "EXPENSE" })

        // Filter INCOMES: Salário Tech = 1 item
        viewModel.onFlowFilterChanged(FlowFilter.INCOMES)
        assertEquals(1, viewModel.uiState.value.filteredCount)
        assertEquals("INCOME", viewModel.uiState.value.filteredTransactions.first().type)
    }

    @Test
    fun searchBySubcategoryName() = runTest {
        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        // Search "Supermercado" (should match category name of Continente and Pingo Doce)
        viewModel.onSearchQueryChanged("Supermercado")
        assertEquals(2, viewModel.uiState.value.filteredCount)
    }

    @Test
    fun periodFilter_currentMonthYearAndAll_adjustsCountsAndIncome() = runTest {
        val txs = listOf(
            TransactionEntity(
                id = 1, type = "INCOME", amountCents = 200000L, yearMonth = 202610,
                epochDay = sampleEpoch, categoryId = "inc_base_salary", contextTag = "TRABALHO",
                merchant = "Salário", paymentMethod = "TRANSFER", isFixed = true
            ),
            TransactionEntity(
                id = 2, type = "EXPENSE", amountCents = 5000L, yearMonth = 202609,
                epochDay = sampleEpoch, categoryId = "exp_food_supermarket", contextTag = "CASA",
                merchant = "Continente", paymentMethod = "CARD", isFixed = false
            ),
            TransactionEntity(
                id = 3, type = "INCOME", amountCents = 100000L, yearMonth = 202501,
                epochDay = sampleEpoch, categoryId = "inc_base_salary", contextTag = "TRABALHO",
                merchant = "Salário Antigo", paymentMethod = "TRANSFER", isFixed = true
            )
        )
        transactionDao = FakeTransactionDao(txs)

        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = 202610
        )

        assertEquals(PeriodFilter.CURRENT_MONTH, viewModel.uiState.value.periodFilter)
        assertEquals(1, viewModel.uiState.value.filteredCount)
        assertEquals(200000L, viewModel.uiState.value.filteredTotalCents)
        assertEquals(200000L, viewModel.uiState.value.periodIncomeCents)

        viewModel.onPeriodFilterChanged(PeriodFilter.YEAR)
        assertEquals(2, viewModel.uiState.value.filteredCount)
        assertEquals(205000L, viewModel.uiState.value.filteredTotalCents)
        assertEquals(200000L, viewModel.uiState.value.periodIncomeCents)

        viewModel.onPeriodFilterChanged(PeriodFilter.ALL)
        assertEquals(3, viewModel.uiState.value.filteredCount)
        assertEquals(305000L, viewModel.uiState.value.filteredTotalCents)
        assertEquals(300000L, viewModel.uiState.value.periodIncomeCents)
    }

    @Test
    fun contextAndMethodFilters_toggleOnOffAndSwitch() = runTest {
        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        viewModel.onContextFilterChanged("CASA")
        assertEquals("CASA", viewModel.uiState.value.contextFilter)
        assertEquals(3, viewModel.uiState.value.filteredCount)

        viewModel.onContextFilterChanged("CASA")
        assertEquals(null, viewModel.uiState.value.contextFilter)
        assertEquals(5, viewModel.uiState.value.filteredCount)

        viewModel.onMethodFilterChanged("MBWAY")
        assertEquals("MBWAY", viewModel.uiState.value.methodFilter)
        assertEquals(2, viewModel.uiState.value.filteredCount)

        viewModel.onMethodFilterChanged("CARD")
        assertEquals("CARD", viewModel.uiState.value.methodFilter)
        assertEquals(1, viewModel.uiState.value.filteredCount)
        assertEquals("Pingo Doce", viewModel.uiState.value.filteredTransactions.first().merchant)

        viewModel.onMethodFilterChanged("CARD")
        assertEquals(null, viewModel.uiState.value.methodFilter)
        assertEquals(5, viewModel.uiState.value.filteredCount)
    }

    @Test
    fun searchQuery_isCaseInsensitiveAcrossMerchantCategoryContextAndMethod() = runTest {
        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        viewModel.onSearchQueryChanged("PINGO DOCE")
        assertEquals(1, viewModel.uiState.value.filteredCount)
        assertEquals("Pingo Doce", viewModel.uiState.value.filteredTransactions.first().merchant)

        viewModel.onSearchQueryChanged("pingo")
        assertEquals(1, viewModel.uiState.value.filteredCount)

        viewModel.onSearchQueryChanged("casa")
        assertEquals(3, viewModel.uiState.value.filteredCount)

        viewModel.onSearchQueryChanged("transfer")
        assertEquals(2, viewModel.uiState.value.filteredCount)
        assertEquals(
            listOf("Senhorio", "Salário Tech"),
            viewModel.uiState.value.filteredTransactions.map { it.merchant }
        )

        viewModel.onSearchQueryChanged("renda / prestação")
        assertEquals(1, viewModel.uiState.value.filteredCount)

        viewModel.onSearchQueryChanged("")
        assertEquals(5, viewModel.uiState.value.filteredCount)
    }

    @Test
    fun combinedFilters_narrowStepByStep() = runTest {
        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        viewModel.onPeriodFilterChanged(PeriodFilter.YEAR)
        assertEquals(5, viewModel.uiState.value.filteredCount)

        viewModel.onFlowFilterChanged(FlowFilter.FIXED)
        assertEquals(2, viewModel.uiState.value.filteredCount)

        viewModel.onContextFilterChanged("CASA")
        assertEquals(1, viewModel.uiState.value.filteredCount)
        assertEquals("Senhorio", viewModel.uiState.value.filteredTransactions.first().merchant)

        viewModel.onMethodFilterChanged("TRANSFER")
        assertEquals(1, viewModel.uiState.value.filteredCount)

        viewModel.onSearchQueryChanged("senhorio")
        assertEquals(1, viewModel.uiState.value.filteredCount)

        viewModel.onSearchQueryChanged("inexistente")
        assertEquals(0, viewModel.uiState.value.filteredCount)
    }

    @Test
    fun summaryCard_zeroIncomeInPeriod_shareIsZeroWhileTotalIsShown() = runTest {
        val expenseOnly = listOf(
            TransactionEntity(
                id = 1, type = "EXPENSE", amountCents = 5000L, yearMonth = sampleYearMonth,
                epochDay = sampleEpoch, categoryId = "exp_food_supermarket", contextTag = "CASA",
                merchant = "Continente", paymentMethod = "CARD", isFixed = false
            ),
            TransactionEntity(
                id = 2, type = "EXPENSE", amountCents = 3000L, yearMonth = sampleYearMonth,
                epochDay = sampleEpoch, categoryId = "exp_food_lunch", contextTag = "TRABALHO",
                merchant = "Café Central", paymentMethod = "MBWAY", isFixed = false
            )
        )
        transactionDao = FakeTransactionDao(expenseOnly)

        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        assertEquals(2, viewModel.uiState.value.filteredCount)
        assertEquals(8000L, viewModel.uiState.value.filteredTotalCents)
        assertEquals(0L, viewModel.uiState.value.periodIncomeCents)
        assertEquals(0.0, viewModel.uiState.value.filteredIncomeSharePercent, 0.0)
    }

    @Test
    fun ordering_mostRecentFirstWithIdTieBreak() = runTest {
        val txs = listOf(
            TransactionEntity(
                id = 1, type = "EXPENSE", amountCents = 1000L, yearMonth = sampleYearMonth,
                epochDay = LocalDate.of(2026, 10, 5).toEpochDay(), categoryId = "exp_food_lunch",
                contextTag = "TRABALHO", merchant = "Antigo", paymentMethod = "CASH", isFixed = false
            ),
            TransactionEntity(
                id = 2, type = "EXPENSE", amountCents = 2000L, yearMonth = sampleYearMonth,
                epochDay = LocalDate.of(2026, 10, 10).toEpochDay(), categoryId = "exp_food_lunch",
                contextTag = "TRABALHO", merchant = "MesmoDiaA", paymentMethod = "CASH", isFixed = false
            ),
            TransactionEntity(
                id = 3, type = "EXPENSE", amountCents = 3000L, yearMonth = sampleYearMonth,
                epochDay = LocalDate.of(2026, 10, 10).toEpochDay(), categoryId = "exp_food_lunch",
                contextTag = "TRABALHO", merchant = "MesmoDiaB", paymentMethod = "CASH", isFixed = false
            ),
            TransactionEntity(
                id = 4, type = "EXPENSE", amountCents = 4000L, yearMonth = sampleYearMonth,
                epochDay = LocalDate.of(2026, 10, 1).toEpochDay(), categoryId = "exp_food_lunch",
                contextTag = "TRABALHO", merchant = "MaisAntigo", paymentMethod = "CASH", isFixed = false
            )
        )
        transactionDao = FakeTransactionDao(txs)

        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        assertEquals(
            listOf("MesmoDiaB", "MesmoDiaA", "Antigo", "MaisAntigo"),
            viewModel.uiState.value.filteredTransactions.map { it.merchant }
        )
    }

    @Test
    fun deleteTransaction_removesRecordFromDao() = runTest {
        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        viewModel.deleteTransaction(2L)

        assertEquals(4, transactionDao.count())
        assertTrue(transactionDao.getAllSync().none { it.id == 2L })
    }

    @Test
    fun filteredExpenseAndIncomeSplits_exactAcrossAllFlows() = runTest {
        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        viewModel.onFlowFilterChanged(FlowFilter.ALL)
        assertEquals(81200L, viewModel.uiState.value.filteredExpenseCents)
        assertEquals(200000L, viewModel.uiState.value.filteredIncomeCents)
        assertEquals(40.6, viewModel.uiState.value.filteredIncomeSharePercent, 0.001)

        viewModel.onFlowFilterChanged(FlowFilter.FIXED)
        assertEquals(60000L, viewModel.uiState.value.filteredExpenseCents)
        assertEquals(200000L, viewModel.uiState.value.filteredIncomeCents)
        assertEquals(260000L, viewModel.uiState.value.filteredTotalCents)

        viewModel.onFlowFilterChanged(FlowFilter.VARIABLE)
        assertEquals(21200L, viewModel.uiState.value.filteredExpenseCents)
        assertEquals(0L, viewModel.uiState.value.filteredIncomeCents)
        assertEquals(10.6, viewModel.uiState.value.filteredIncomeSharePercent, 0.001)

        viewModel.onFlowFilterChanged(FlowFilter.INCOMES)
        assertEquals(0L, viewModel.uiState.value.filteredExpenseCents)
        assertEquals(200000L, viewModel.uiState.value.filteredIncomeCents)
        assertEquals(100.0, viewModel.uiState.value.filteredIncomeSharePercent, 0.001)
    }

    @Test
    fun filteredSplits_respectContextAndSearchFilters() = runTest {
        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        viewModel.onContextFilterChanged("CASA")
        assertEquals(80000L, viewModel.uiState.value.filteredExpenseCents)
        assertEquals(0L, viewModel.uiState.value.filteredIncomeCents)

        viewModel.onContextFilterChanged("CASA")
        viewModel.onSearchQueryChanged("senhorio")
        assertEquals(60000L, viewModel.uiState.value.filteredExpenseCents)
        assertEquals(0L, viewModel.uiState.value.filteredIncomeCents)

        viewModel.onSearchQueryChanged("")
        viewModel.onContextFilterChanged("TRABALHO")
        assertEquals(200000L, viewModel.uiState.value.filteredIncomeCents)
        assertEquals(1200L, viewModel.uiState.value.filteredExpenseCents)
    }

    private val amountTransactions = listOf(
        TransactionEntity(
            id = 1, type = "INCOME", amountCents = 200000L, yearMonth = sampleYearMonth,
            epochDay = sampleEpoch, categoryId = "inc_base_salary", contextTag = "TRABALHO",
            merchant = "Salário", paymentMethod = "TRANSFER", isFixed = true
        ),
        TransactionEntity(
            id = 2, type = "EXPENSE", amountCents = 500L, yearMonth = sampleYearMonth,
            epochDay = sampleEpoch, categoryId = "exp_food_cafe", contextTag = "CASA",
            merchant = "Café", paymentMethod = "CASH", isFixed = false
        ),
        TransactionEntity(
            id = 3, type = "EXPENSE", amountCents = 1000L, yearMonth = sampleYearMonth,
            epochDay = sampleEpoch, categoryId = "exp_food_lunch", contextTag = "CASA",
            merchant = "Pão", paymentMethod = "CASH", isFixed = false
        ),
        TransactionEntity(
            id = 4, type = "EXPENSE", amountCents = 2500L, yearMonth = sampleYearMonth,
            epochDay = sampleEpoch, categoryId = "exp_food_lunch", contextTag = "TRABALHO",
            merchant = "Almoço", paymentMethod = "MBWAY", isFixed = false
        ),
        TransactionEntity(
            id = 5, type = "EXPENSE", amountCents = 4999L, yearMonth = sampleYearMonth,
            epochDay = sampleEpoch, categoryId = "exp_food_restaurant", contextTag = "SAIDA",
            merchant = "Cinema", paymentMethod = "CARD", isFixed = false
        ),
        TransactionEntity(
            id = 6, type = "EXPENSE", amountCents = 5000L, yearMonth = sampleYearMonth,
            epochDay = sampleEpoch, categoryId = "exp_food_restaurant", contextTag = "SAIDA",
            merchant = "Livro", paymentMethod = "CARD", isFixed = false
        ),
        TransactionEntity(
            id = 7, type = "EXPENSE", amountCents = 60000L, yearMonth = sampleYearMonth,
            epochDay = sampleEpoch, categoryId = "exp_housing_rent", contextTag = "CASA",
            merchant = "Renda", paymentMethod = "TRANSFER", isFixed = true
        ),
        TransactionEntity(
            id = 8, type = "INCOME", amountCents = 800L, yearMonth = sampleYearMonth,
            epochDay = sampleEpoch, categoryId = "inc_refunds", contextTag = "TRABALHO",
            merchant = "Reembolso", paymentMethod = "MBWAY", isFixed = false
        )
    )

    @Test
    fun amountFilter_selectsExactlyTheExpectedBand() = runTest {
        transactionDao = FakeTransactionDao(amountTransactions)

        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        assertEquals(AmountFilter.ANY, viewModel.uiState.value.amountFilter)
        assertEquals(8, viewModel.uiState.value.filteredCount)

        viewModel.onAmountFilterChanged(AmountFilter.UP_TO_10)
        assertEquals(AmountFilter.UP_TO_10, viewModel.uiState.value.amountFilter)
        assertEquals(3, viewModel.uiState.value.filteredCount)
        assertEquals(
            listOf("Café", "Pão", "Reembolso"),
            viewModel.uiState.value.filteredTransactions.map { it.merchant }.sorted()
        )
        assertEquals(2300L, viewModel.uiState.value.filteredTotalCents)
        assertEquals(1500L, viewModel.uiState.value.filteredExpenseCents)
        assertEquals(800L, viewModel.uiState.value.filteredIncomeCents)
        assertEquals(200800L, viewModel.uiState.value.periodIncomeCents)

        viewModel.onAmountFilterChanged(AmountFilter.BETWEEN_10_50)
        assertEquals(2, viewModel.uiState.value.filteredCount)
        assertEquals(
            listOf("Almoço", "Cinema"),
            viewModel.uiState.value.filteredTransactions.map { it.merchant }.sorted()
        )
        assertEquals(7499L, viewModel.uiState.value.filteredTotalCents)

        viewModel.onAmountFilterChanged(AmountFilter.OVER_50)
        assertEquals(3, viewModel.uiState.value.filteredCount)
        assertEquals(
            listOf("Livro", "Renda", "Salário"),
            viewModel.uiState.value.filteredTransactions.map { it.merchant }.sorted()
        )
        assertEquals(265000L, viewModel.uiState.value.filteredTotalCents)
    }

    @Test
    fun amountFilter_toggleReturnsToAny() = runTest {
        transactionDao = FakeTransactionDao(amountTransactions)

        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        viewModel.onAmountFilterChanged(AmountFilter.UP_TO_10)
        assertEquals(3, viewModel.uiState.value.filteredCount)

        viewModel.onAmountFilterChanged(AmountFilter.UP_TO_10)
        assertEquals(AmountFilter.ANY, viewModel.uiState.value.amountFilter)
        assertEquals(8, viewModel.uiState.value.filteredCount)

        viewModel.onAmountFilterChanged(AmountFilter.OVER_50)
        assertEquals(AmountFilter.OVER_50, viewModel.uiState.value.amountFilter)
        viewModel.onAmountFilterChanged(AmountFilter.ANY)
        assertEquals(AmountFilter.ANY, viewModel.uiState.value.amountFilter)
        assertEquals(8, viewModel.uiState.value.filteredCount)
    }

    @Test
    fun amountFilter_combinesWithContextFlowAndSearchFilters() = runTest {
        transactionDao = FakeTransactionDao(amountTransactions)

        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        viewModel.onContextFilterChanged("CASA")
        assertEquals(3, viewModel.uiState.value.filteredCount)

        viewModel.onAmountFilterChanged(AmountFilter.UP_TO_10)
        assertEquals(2, viewModel.uiState.value.filteredCount)
        assertEquals(
            listOf("Café", "Pão"),
            viewModel.uiState.value.filteredTransactions.map { it.merchant }.sorted()
        )

        viewModel.onAmountFilterChanged(AmountFilter.OVER_50)
        assertEquals(1, viewModel.uiState.value.filteredCount)
        assertEquals("Renda", viewModel.uiState.value.filteredTransactions.first().merchant)

        viewModel.onAmountFilterChanged(AmountFilter.ANY)
        viewModel.onContextFilterChanged("CASA")
        assertEquals(null, viewModel.uiState.value.contextFilter)
        viewModel.onFlowFilterChanged(FlowFilter.INCOMES)
        viewModel.onAmountFilterChanged(AmountFilter.UP_TO_10)
        assertEquals(1, viewModel.uiState.value.filteredCount)
        assertEquals("Reembolso", viewModel.uiState.value.filteredTransactions.first().merchant)

        viewModel.onAmountFilterChanged(AmountFilter.ANY)
        viewModel.onFlowFilterChanged(FlowFilter.ALL)
        viewModel.onContextFilterChanged("SAIDA")
        viewModel.onAmountFilterChanged(AmountFilter.BETWEEN_10_50)
        assertEquals(1, viewModel.uiState.value.filteredCount)
        assertEquals("Cinema", viewModel.uiState.value.filteredTransactions.first().merchant)

        viewModel.onSearchQueryChanged("inexistente")
        assertEquals(0, viewModel.uiState.value.filteredCount)
    }

    @Test
    fun amountFilter_matchesTheExactBandBoundaries() {
        assertTrue(AmountFilter.ANY.matches(0L))
        assertTrue(AmountFilter.ANY.matches(1000000L))

        assertTrue(AmountFilter.UP_TO_10.matches(0L))
        assertTrue(AmountFilter.UP_TO_10.matches(999L))
        assertTrue(AmountFilter.UP_TO_10.matches(1000L))
        assertFalse(AmountFilter.UP_TO_10.matches(1001L))

        assertFalse(AmountFilter.BETWEEN_10_50.matches(1000L))
        assertTrue(AmountFilter.BETWEEN_10_50.matches(1001L))
        assertTrue(AmountFilter.BETWEEN_10_50.matches(4999L))
        assertFalse(AmountFilter.BETWEEN_10_50.matches(5000L))

        assertFalse(AmountFilter.OVER_50.matches(4999L))
        assertTrue(AmountFilter.OVER_50.matches(5000L))
        assertTrue(AmountFilter.OVER_50.matches(5001L))
    }

    @Test
    fun amountFilter_boundariesAreInclusiveAtTenAndFiftyEuros() = runTest {
        transactionDao = FakeTransactionDao(
            listOf(
                TransactionEntity(
                    id = 1, type = "EXPENSE", amountCents = 999L, yearMonth = sampleYearMonth,
                    epochDay = sampleEpoch, categoryId = "exp_food_cafe", contextTag = "CASA",
                    merchant = "A999", paymentMethod = "CASH", isFixed = false
                ),
                TransactionEntity(
                    id = 2, type = "EXPENSE", amountCents = 1000L, yearMonth = sampleYearMonth,
                    epochDay = sampleEpoch, categoryId = "exp_food_cafe", contextTag = "CASA",
                    merchant = "B1000", paymentMethod = "CASH", isFixed = false
                ),
                TransactionEntity(
                    id = 3, type = "EXPENSE", amountCents = 1001L, yearMonth = sampleYearMonth,
                    epochDay = sampleEpoch, categoryId = "exp_food_cafe", contextTag = "CASA",
                    merchant = "C1001", paymentMethod = "CASH", isFixed = false
                ),
                TransactionEntity(
                    id = 4, type = "EXPENSE", amountCents = 4999L, yearMonth = sampleYearMonth,
                    epochDay = sampleEpoch, categoryId = "exp_food_cafe", contextTag = "CASA",
                    merchant = "D4999", paymentMethod = "CASH", isFixed = false
                ),
                TransactionEntity(
                    id = 5, type = "EXPENSE", amountCents = 5000L, yearMonth = sampleYearMonth,
                    epochDay = sampleEpoch, categoryId = "exp_food_cafe", contextTag = "CASA",
                    merchant = "E5000", paymentMethod = "CASH", isFixed = false
                ),
                TransactionEntity(
                    id = 6, type = "INCOME", amountCents = 5000L, yearMonth = sampleYearMonth,
                    epochDay = sampleEpoch, categoryId = "inc_base_salary", contextTag = "TRABALHO",
                    merchant = "F5000", paymentMethod = "TRANSFER", isFixed = true
                ),
                TransactionEntity(
                    id = 7, type = "INCOME", amountCents = 1000L, yearMonth = sampleYearMonth,
                    epochDay = sampleEpoch, categoryId = "inc_base_salary", contextTag = "TRABALHO",
                    merchant = "G1000", paymentMethod = "TRANSFER", isFixed = true
                )
            )
        )

        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        viewModel.onAmountFilterChanged(AmountFilter.UP_TO_10)
        assertEquals(3, viewModel.uiState.value.filteredCount)
        assertEquals(
            listOf("A999", "B1000", "G1000"),
            viewModel.uiState.value.filteredTransactions.map { it.merchant }.sorted()
        )
        assertEquals(2999L, viewModel.uiState.value.filteredTotalCents)
        assertEquals(1999L, viewModel.uiState.value.filteredExpenseCents)
        assertEquals(1000L, viewModel.uiState.value.filteredIncomeCents)

        viewModel.onAmountFilterChanged(AmountFilter.BETWEEN_10_50)
        assertEquals(2, viewModel.uiState.value.filteredCount)
        assertEquals(
            listOf("C1001", "D4999"),
            viewModel.uiState.value.filteredTransactions.map { it.merchant }.sorted()
        )
        assertEquals(6000L, viewModel.uiState.value.filteredTotalCents)

        viewModel.onAmountFilterChanged(AmountFilter.OVER_50)
        assertEquals(2, viewModel.uiState.value.filteredCount)
        assertEquals(
            listOf("E5000", "F5000"),
            viewModel.uiState.value.filteredTransactions.map { it.merchant }.sorted()
        )
        assertEquals(10000L, viewModel.uiState.value.filteredTotalCents)
        assertEquals(5000L, viewModel.uiState.value.filteredExpenseCents)
        assertEquals(5000L, viewModel.uiState.value.filteredIncomeCents)
    }

    @Test
    fun amountFilter_combinesWithThePeriodFilter() = runTest {
        transactionDao = FakeTransactionDao(
            listOf(
                TransactionEntity(
                    id = 1, type = "EXPENSE", amountCents = 500L, yearMonth = 202610,
                    epochDay = sampleEpoch, categoryId = "exp_food_cafe", contextTag = "CASA",
                    merchant = "Outubro Pequeno", paymentMethod = "CASH", isFixed = false
                ),
                TransactionEntity(
                    id = 2, type = "EXPENSE", amountCents = 60000L, yearMonth = 202609,
                    epochDay = sampleEpoch, categoryId = "exp_housing_rent", contextTag = "CASA",
                    merchant = "Setembro Grande", paymentMethod = "TRANSFER", isFixed = true
                )
            )
        )

        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        assertEquals(1, viewModel.uiState.value.filteredCount)

        viewModel.onPeriodFilterChanged(PeriodFilter.YEAR)
        assertEquals(2, viewModel.uiState.value.filteredCount)

        viewModel.onAmountFilterChanged(AmountFilter.OVER_50)
        assertEquals(1, viewModel.uiState.value.filteredCount)
        assertEquals("Setembro Grande", viewModel.uiState.value.filteredTransactions.first().merchant)

        viewModel.onAmountFilterChanged(AmountFilter.UP_TO_10)
        assertEquals(1, viewModel.uiState.value.filteredCount)
        assertEquals("Outubro Pequeno", viewModel.uiState.value.filteredTransactions.first().merchant)
    }

    @Test
    fun amountFilter_combinesWithThePaymentMethodFilter() = runTest {
        transactionDao = FakeTransactionDao(
            listOf(
                TransactionEntity(
                    id = 1, type = "EXPENSE", amountCents = 60000L, yearMonth = sampleYearMonth,
                    epochDay = sampleEpoch, categoryId = "exp_housing_rent", contextTag = "CASA",
                    merchant = "Renda", paymentMethod = "TRANSFER", isFixed = true
                ),
                TransactionEntity(
                    id = 2, type = "EXPENSE", amountCents = 60000L, yearMonth = sampleYearMonth,
                    epochDay = sampleEpoch, categoryId = "exp_housing_rent", contextTag = "CASA",
                    merchant = "Loja", paymentMethod = "CARD", isFixed = false
                )
            )
        )

        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        viewModel.onAmountFilterChanged(AmountFilter.OVER_50)
        assertEquals(2, viewModel.uiState.value.filteredCount)

        viewModel.onMethodFilterChanged("CARD")
        assertEquals(1, viewModel.uiState.value.filteredCount)
        assertEquals("Loja", viewModel.uiState.value.filteredTransactions.first().merchant)

        viewModel.onMethodFilterChanged("CARD")
        assertEquals(null, viewModel.uiState.value.methodFilter)
        assertEquals(2, viewModel.uiState.value.filteredCount)
    }

    @Test
    fun amountFilter_keepsIncomeAndExpensesInTheSameSelection() = runTest {
        transactionDao = FakeTransactionDao(
            listOf(
                TransactionEntity(
                    id = 1, type = "INCOME", amountCents = 200000L, yearMonth = sampleYearMonth,
                    epochDay = sampleEpoch, categoryId = "inc_base_salary", contextTag = "TRABALHO",
                    merchant = "Salário", paymentMethod = "TRANSFER", isFixed = true
                ),
                TransactionEntity(
                    id = 2, type = "EXPENSE", amountCents = 60000L, yearMonth = sampleYearMonth,
                    epochDay = sampleEpoch, categoryId = "exp_housing_rent", contextTag = "CASA",
                    merchant = "Renda", paymentMethod = "TRANSFER", isFixed = true
                )
            )
        )

        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        viewModel.onAmountFilterChanged(AmountFilter.OVER_50)

        assertEquals(2, viewModel.uiState.value.filteredCount)
        assertEquals(260000L, viewModel.uiState.value.filteredTotalCents)
        assertEquals(60000L, viewModel.uiState.value.filteredExpenseCents)
        assertEquals(200000L, viewModel.uiState.value.filteredIncomeCents)
        assertEquals(200000L, viewModel.uiState.value.periodIncomeCents)
        assertEquals(30.0, viewModel.uiState.value.filteredIncomeSharePercent, 0.001)
    }

    @Test
    fun amountFilter_selectingAnotherBandReplacesThePreviousSelection() = runTest {
        transactionDao = FakeTransactionDao(amountTransactions)

        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        viewModel.onAmountFilterChanged(AmountFilter.UP_TO_10)
        assertEquals(3, viewModel.uiState.value.filteredCount)

        viewModel.onAmountFilterChanged(AmountFilter.OVER_50)
        assertEquals(AmountFilter.OVER_50, viewModel.uiState.value.amountFilter)
        assertEquals(3, viewModel.uiState.value.filteredCount)
        assertTrue(viewModel.uiState.value.filteredTransactions.none { it.merchant == "Café" })

        viewModel.onAmountFilterChanged(AmountFilter.ANY)
        assertEquals(AmountFilter.ANY, viewModel.uiState.value.amountFilter)
        assertEquals(8, viewModel.uiState.value.filteredCount)
    }

    @Test
    fun refundsMap_correctlyAggregatesRefundAmountsForParentTransactions() = runTest {
        val expense1 = TransactionEntity(
            id = 10L, type = "EXPENSE", amountCents = 6000L, yearMonth = sampleYearMonth,
            epochDay = sampleEpoch, categoryId = "exp_food_lunch", contextTag = "SAIDA",
            merchant = "Jantar", paymentMethod = "CARD"
        )
        val refund1 = TransactionEntity(
            id = 11L, type = "INCOME", amountCents = 2000L, yearMonth = sampleYearMonth,
            epochDay = sampleEpoch, categoryId = "inc_other", contextTag = "SAIDA",
            merchant = "Amigo 1", paymentMethod = "MBWAY", linkedTransactionId = 10L
        )
        val refund2 = TransactionEntity(
            id = 12L, type = "INCOME", amountCents = 2000L, yearMonth = sampleYearMonth,
            epochDay = sampleEpoch, categoryId = "inc_other", contextTag = "SAIDA",
            merchant = "Amigo 2", paymentMethod = "MBWAY", linkedTransactionId = 10L
        )

        transactionDao = FakeTransactionDao(listOf(expense1, refund1, refund2))
        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = sampleYearMonth
        )

        assertEquals(4000L, viewModel.uiState.value.refundsMap[10L])
        // Refunds should not be counted in periodIncome (denominator)
        assertEquals(0L, viewModel.uiState.value.periodIncomeCents)
    }

    @Test
    fun setYearMonth_updatesCurrentYearMonth_monthTitle_andFiltersTransactions() = runTest {
        val txOct = TransactionEntity(
            id = 1L, type = "EXPENSE", amountCents = 1000L, yearMonth = 202610,
            epochDay = LocalDate.of(2026, 10, 5).toEpochDay(), categoryId = "exp_food_lunch",
            contextTag = "CASA", merchant = "Outubro Café", paymentMethod = "CARD"
        )
        val txSep = TransactionEntity(
            id = 2L, type = "EXPENSE", amountCents = 2500L, yearMonth = 202609,
            epochDay = LocalDate.of(2026, 9, 20).toEpochDay(), categoryId = "exp_food_lunch",
            contextTag = "CASA", merchant = "Setembro Restaurante", paymentMethod = "CARD"
        )

        transactionDao = FakeTransactionDao(listOf(txOct, txSep))
        val viewModel = TransactionsViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            initialYearMonth = 202610
        )

        assertEquals(202610, viewModel.uiState.value.currentYearMonth)
        assertEquals("Outubro 2026", viewModel.uiState.value.monthTitle)
        assertEquals(1, viewModel.uiState.value.filteredCount)
        assertEquals("Outubro Café", viewModel.uiState.value.filteredTransactions.first().merchant)

        viewModel.setYearMonth(202609)
        assertEquals(202609, viewModel.uiState.value.currentYearMonth)
        assertEquals("Setembro 2026", viewModel.uiState.value.monthTitle)
        assertEquals(1, viewModel.uiState.value.filteredCount)
        assertEquals("Setembro Restaurante", viewModel.uiState.value.filteredTransactions.first().merchant)
        assertEquals(PeriodFilter.CURRENT_MONTH, viewModel.uiState.value.periodFilter)
    }

    @Test
    fun selectPreviousMonth_and_selectNextMonth_navigateCorrectlyAcrossYears() = runTest {
        val viewModel = TransactionsViewModel(
            transactionDao = FakeTransactionDao(emptyList()),
            categoryDao = categoryDao,
            initialYearMonth = 202601
        )

        assertEquals(202601, viewModel.uiState.value.currentYearMonth)
        assertEquals("Janeiro 2026", viewModel.uiState.value.monthTitle)

        viewModel.selectPreviousMonth()
        assertEquals(202512, viewModel.uiState.value.currentYearMonth)
        assertEquals("Dezembro 2025", viewModel.uiState.value.monthTitle)

        viewModel.selectNextMonth()
        assertEquals(202601, viewModel.uiState.value.currentYearMonth)
        assertEquals("Janeiro 2026", viewModel.uiState.value.monthTitle)
    }
}
