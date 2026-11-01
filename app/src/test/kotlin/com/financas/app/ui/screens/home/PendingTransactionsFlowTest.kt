package com.financas.app.ui.screens.home

import com.financas.app.data.entity.PendingTransactionEntity
import com.financas.app.data.seed.DefaultSeed
import com.financas.app.domain.usecase.MonthlyAnalyticsCalculator
import com.financas.app.ui.fakes.FakeBudgetDao
import com.financas.app.ui.fakes.FakeCategoryDao
import com.financas.app.ui.fakes.FakeGoalDao
import com.financas.app.ui.fakes.FakePendingTransactionDao
import com.financas.app.ui.fakes.FakeTransactionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class PendingTransactionsFlowTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var budgetDao: FakeBudgetDao
    private lateinit var goalDao: FakeGoalDao
    private lateinit var pendingDao: FakePendingTransactionDao
    private lateinit var viewModel: HomeViewModel

    private val testEpochDay = LocalDate.of(2026, 10, 15).toEpochDay()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        categoryDao = FakeCategoryDao(DefaultSeed.getDefaultCategories())
        transactionDao = FakeTransactionDao(emptyList())
        budgetDao = FakeBudgetDao()
        goalDao = FakeGoalDao()
        pendingDao = FakePendingTransactionDao(
            listOf(
                PendingTransactionEntity(
                    id = 1,
                    amountCents = 1250L,
                    rawMerchant = "Continente Bom Dia",
                    suggestedCategoryId = "sub_groceries",
                    suggestedContextTag = "CASA",
                    suggestedPaymentMethod = "MBWAY",
                    isFixed = false,
                    sourceApp = "MB WAY",
                    detectedEpochDay = testEpochDay,
                    receivedTimestamp = 1000_000L
                ),
                PendingTransactionEntity(
                    id = 2,
                    amountCents = 350L,
                    rawMerchant = "Café Central",
                    suggestedCategoryId = "sub_coffee",
                    suggestedContextTag = "TRABALHO",
                    suggestedPaymentMethod = "CARD",
                    isFixed = false,
                    sourceApp = "Moey",
                    detectedEpochDay = testEpochDay,
                    receivedTimestamp = 1001_000L
                )
            )
        )

        viewModel = HomeViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            budgetDao = budgetDao,
            goalDao = goalDao,
            pendingTransactionDao = pendingDao,
            monthlyAnalyticsCalculator = MonthlyAnalyticsCalculator(),
            initialYearMonth = 202610
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialPendingTransactions_loadedIntoUiState() = runTest {
        val state = viewModel.uiState.value
        assertEquals(2, state.pendingCount)
        assertEquals(2, state.pendingTransactions.size)
        // Most recent first (timestamp 1001_000L > 1000_000L)
        assertEquals("Café Central", state.pendingTransactions[0].rawMerchant)
        assertEquals("Continente Bom Dia", state.pendingTransactions[1].rawMerchant)
    }

    @Test
    fun confirmPending_createsTransactionAndRemovesFromPending() = runTest {
        val pendingItem = viewModel.uiState.value.pendingTransactions.first { it.id == 1L }
        viewModel.confirmPending(pendingItem)

        // Verifies transaction was created
        val txs = transactionDao.getAllSync()
        assertEquals(1, txs.size)
        val created = txs.first()
        assertEquals(1250L, created.amountCents)
        assertEquals("Continente Bom Dia", created.merchant)
        assertEquals("sub_groceries", created.categoryId)
        assertEquals("CASA", created.contextTag)
        assertEquals("MBWAY", created.paymentMethod)
        assertEquals(202610, created.yearMonth)

        // Verifies item was removed from pending
        val remainingPending = pendingDao.getAllSync()
        assertEquals(1, remainingPending.size)
        assertEquals(2L, remainingPending.first().id)
        assertEquals(1, viewModel.uiState.value.pendingCount)
    }

    @Test
    fun confirmPending_withModifiedCategoryAndContext_savesUserChanges() = runTest {
        val pendingItem = viewModel.uiState.value.pendingTransactions.first { it.id == 1L }
        // User changes category to restaurant and context to social
        viewModel.confirmPending(
            pending = pendingItem,
            chosenCategoryId = "sub_restaurants",
            chosenContextTag = "SAIDA"
        )

        val txs = transactionDao.getAllSync()
        assertEquals(1, txs.size)
        val created = txs.first()
        assertEquals("sub_restaurants", created.categoryId)
        assertEquals("SAIDA", created.contextTag)
    }

    @Test
    fun discardPending_deletesFromPendingWithoutSaving() = runTest {
        viewModel.discardPending(1L)

        // No transaction should be created
        assertTrue(transactionDao.getAllSync().isEmpty())

        // Pending count decremented
        val remaining = pendingDao.getAllSync()
        assertEquals(1, remaining.size)
        assertEquals(2L, remaining.first().id)
        assertEquals(1, viewModel.uiState.value.pendingCount)
    }

    @Test
    fun confirmAllPending_confirmsAllAndClearsQueue() = runTest {
        viewModel.confirmAllPending()

        val txs = transactionDao.getAllSync()
        assertEquals(2, txs.size)

        val remainingPending = pendingDao.getAllSync()
        assertTrue(remainingPending.isEmpty())
        assertEquals(0, viewModel.uiState.value.pendingCount)
    }

    @Test
    fun confirmPending_incomeSuggestedType_createsIncomeTransaction() = runTest {
        pendingDao.insert(
            PendingTransactionEntity(
                amountCents = 8000L,
                rawMerchant = "Maria",
                suggestedType = "INCOME",
                suggestedCategoryId = "inc_meal_allowance",
                suggestedContextTag = "SAIDA",
                suggestedPaymentMethod = "MBWAY",
                isFixed = false,
                sourceApp = "MB WAY",
                detectedEpochDay = testEpochDay,
                receivedTimestamp = 1002_000L
            )
        )

        val incomePending = pendingDao.getAllSync().first { it.suggestedType == "INCOME" }
        viewModel.confirmPending(incomePending)

        val txs = transactionDao.getAllSync()
        assertEquals(1, txs.size)
        val created = txs.first()
        assertEquals("INCOME", created.type)
        assertEquals(8000L, created.amountCents)
        assertEquals("Maria", created.merchant)
        assertEquals("inc_meal_allowance", created.categoryId)
        assertEquals(202610, created.yearMonth)

        assertTrue(pendingDao.getAllSync().none { it.id == incomePending.id })
    }

    @Test
    fun confirmAllPending_respectsEachPendingSuggestedType() = runTest {
        pendingDao.insert(
            PendingTransactionEntity(
                amountCents = 8000L,
                rawMerchant = "Maria",
                suggestedType = "INCOME",
                suggestedCategoryId = "inc_meal_allowance",
                suggestedContextTag = "SAIDA",
                suggestedPaymentMethod = "MBWAY",
                isFixed = false,
                sourceApp = "MB WAY",
                detectedEpochDay = testEpochDay,
                receivedTimestamp = 1002_000L
            )
        )

        viewModel.confirmAllPending()

        val txs = transactionDao.getAllSync()
        assertEquals(3, txs.size)
        assertEquals(2, txs.count { it.type == "EXPENSE" })
        assertEquals(1, txs.count { it.type == "INCOME" })
        assertEquals(8000L, txs.first { it.type == "INCOME" }.amountCents)

        assertTrue(pendingDao.getAllSync().isEmpty())
    }

    @Test
    fun simulateNotification_addsPendingTransaction() = runTest {
        viewModel.simulatePendingNotification(
            amountCents = 4999L,
            merchant = "Zara Chiado",
            sourceApp = "Moey"
        )

        val allPending = pendingDao.getAllSync()
        assertEquals(3, allPending.size)
        val simulated = allPending.first { it.rawMerchant == "Zara Chiado" }
        assertEquals(4999L, simulated.amountCents)
        assertEquals("Moey", simulated.sourceApp)
        assertEquals(3, viewModel.uiState.value.pendingCount)
    }
}
