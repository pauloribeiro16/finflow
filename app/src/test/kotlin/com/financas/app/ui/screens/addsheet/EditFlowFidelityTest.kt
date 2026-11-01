package com.financas.app.ui.screens.addsheet

import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.seed.DefaultSeed
import com.financas.app.domain.usecase.SmartPrefillEngine
import com.financas.app.ui.fakes.FakeCategoryDao
import com.financas.app.ui.fakes.FakeContextTagsStore
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class EditFlowFidelityTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var transactionDao: FakeTransactionDao

    private val expenseTx = TransactionEntity(
        id = 7L,
        type = "EXPENSE",
        amountCents = 2450L,
        yearMonth = 202610,
        epochDay = LocalDate.of(2026, 10, 10).toEpochDay(),
        categoryId = "exp_food_supermarket",
        contextTag = "CASA",
        merchant = "Continente",
        paymentMethod = "MBWAY",
        isFixed = false
    )

    private val incomeTx = TransactionEntity(
        id = 8L,
        type = "INCOME",
        amountCents = 250000L,
        yearMonth = 202610,
        epochDay = LocalDate.of(2026, 10, 1).toEpochDay(),
        categoryId = "inc_base_salary",
        contextTag = "TRABALHO",
        merchant = "Empresa",
        paymentMethod = "TRANSFER",
        isFixed = true
    )

    private val fixedExpenseTx = TransactionEntity(
        id = 9L,
        type = "EXPENSE",
        amountCents = 55000L,
        yearMonth = 202611,
        epochDay = LocalDate.of(2026, 11, 3).toEpochDay(),
        categoryId = "exp_housing_rent",
        contextTag = "CASA",
        merchant = "Senhorio",
        paymentMethod = "DIRECT_DEBIT",
        isFixed = true
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        categoryDao = FakeCategoryDao(DefaultSeed.getDefaultCategories())
        transactionDao = FakeTransactionDao(listOf(expenseTx, incomeTx, fixedExpenseTx))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun txById(id: Long): TransactionEntity =
        transactionDao.getAllSync().first { it.id == id }

    private fun buildViewModel(store: FakeContextTagsStore = FakeContextTagsStore()) = AddTransactionViewModel(
        transactionDao = transactionDao,
        categoryDao = categoryDao,
        smartPrefillEngine = SmartPrefillEngine(transactionDao),
        contextTagsStore = store
    )

    @Test
    fun test1_startEdit_preservesEveryNonEditedField() = runTest {
        val viewModel = buildViewModel()

        viewModel.startEdit(expenseTx)

        val state = viewModel.uiState.value
        assertEquals(7L, state.editingTransactionId)
        assertEquals("EXPENSE", state.type)
        assertEquals("24,50", state.rawAmount)
        assertEquals(2450L, state.amountCents)
        assertEquals("Continente", state.merchant)
        assertEquals("exp_food_supermarket", state.selectedCategoryId)
        assertEquals("exp_food", state.selectedParentCategoryId)
        assertEquals("CASA", state.selectedContextTag)
        assertEquals("MBWAY", state.selectedPaymentMethod)
        assertFalse(state.isFixed)
        assertEquals(expenseTx.epochDay, state.epochDay)
    }

    @Test
    fun test2_editingOnlyTheAmount_KeepsCategoryContextMethodAndDate() = runTest {
        val viewModel = buildViewModel()

        viewModel.startEdit(expenseTx)
        viewModel.setRawAmount("30,25")
        viewModel.saveTransaction()

        val updated = transactionDao.getAllSync().first { it.id == expenseTx.id }
        assertEquals(3025L, updated.amountCents)
        assertEquals("exp_food_supermarket", updated.categoryId)
        assertEquals("CASA", updated.contextTag)
        assertEquals("MBWAY", updated.paymentMethod)
        assertEquals(expenseTx.epochDay, updated.epochDay)
        assertEquals(202610, updated.yearMonth)
        assertEquals("Continente", updated.merchant)
        assertEquals("EXPENSE", updated.type)
        assertFalse(updated.isFixed)
    }

    @Test
    fun test3_editingAndSaving_DoesNotChangeTheDaoCount() = runTest {
        val viewModel = buildViewModel()

        viewModel.startEdit(expenseTx)
        viewModel.setRawAmount("99,99")
        viewModel.saveTransaction()

        assertEquals(3, transactionDao.count())
        assertEquals(9999L, txById(7L).amountCents)
    }

    @Test
    fun test4_zeroAmountInEditMode_BlocksTheSaveAndKeepsTheOriginalRecord() = runTest {
        val viewModel = buildViewModel()

        viewModel.startEdit(expenseTx)
        viewModel.onClear()
        assertEquals(0L, viewModel.uiState.value.amountCents)

        var callbackCalled = false
        viewModel.saveTransaction { callbackCalled = true }

        assertFalse(callbackCalled)
        assertFalse(viewModel.uiState.value.isSaved)
        assertEquals(3, transactionDao.count())
        assertEquals(2450L, txById(7L).amountCents)
        assertEquals("Continente", txById(7L).merchant)
    }

    @Test
    fun test5_startNewEntry_ClearsTheEditingIdAndResetsTheForm() = runTest {
        val viewModel = buildViewModel()

        viewModel.startEdit(fixedExpenseTx)
        assertEquals(9L, viewModel.uiState.value.editingTransactionId)

        viewModel.startNewEntry()

        val state = viewModel.uiState.value
        assertNull(state.editingTransactionId)
        assertEquals("EXPENSE", state.type)
        assertEquals("0", state.rawAmount)
        assertEquals(0L, state.amountCents)
        assertEquals("", state.merchant)
        assertFalse(state.isFixed)
        assertEquals("COMERCIO", state.selectedContextTag)
        assertEquals("MBWAY", state.selectedPaymentMethod)
        assertEquals("exp_housing", state.selectedParentCategoryId)
        assertEquals("exp_housing_rent", state.selectedCategoryId)
    }

    @Test
    fun test6_startNewEntry_ThenSavingCreatesANewRecordWithoutTouchingTheEditedOne() = runTest {
        val viewModel = buildViewModel()

        viewModel.startEdit(fixedExpenseTx)
        viewModel.startNewEntry()
        viewModel.onDigit('1')
        viewModel.onDigit('2')
        viewModel.saveTransaction()

        assertEquals(4, transactionDao.count())
        assertEquals(55000L, txById(9L).amountCents)

        val created = transactionDao.getAllSync().first { it.id != 9L && it.id != 7L && it.id != 8L }
        assertEquals(1200L, created.amountCents)
        assertNotNull(created.id)
    }

    @Test
    fun test7_editingAnIncomeTransaction_KeepsIncomeTypeAndCategoryGroup() = runTest {
        val viewModel = buildViewModel()

        viewModel.startEdit(incomeTx)

        val state = viewModel.uiState.value
        assertEquals("INCOME", state.type)
        assertEquals("inc_base_salary", state.selectedCategoryId)
        assertEquals("inc_fixed", state.selectedParentCategoryId)
        assertEquals(listOf("inc_fixed", "inc_extra"), state.parentCategories.map { it.id })
        assertEquals(listOf("inc_base_salary", "inc_meal_allowance"), state.subcategories.map { it.id })
        assertTrue(state.isFixed)

        viewModel.setRawAmount("2600,00")
        viewModel.saveTransaction()

        val updated = transactionDao.getAllSync().first { it.id == incomeTx.id }
        assertEquals("INCOME", updated.type)
        assertEquals(260000L, updated.amountCents)
        assertEquals("inc_base_salary", updated.categoryId)
        assertTrue(updated.isFixed)
        assertEquals("TRANSFER", updated.paymentMethod)
    }

    @Test
    fun test8_editingAFixedExpense_KeepsTheFixedFlagAndTheOriginalDate() = runTest {
        val viewModel = buildViewModel()

        viewModel.startEdit(fixedExpenseTx)
        viewModel.onMerchantChanged("Senhorio Novo")
        viewModel.saveTransaction()

        val updated = transactionDao.getAllSync().first { it.id == fixedExpenseTx.id }
        assertEquals("Senhorio Novo", updated.merchant)
        assertTrue(updated.isFixed)
        assertEquals(fixedExpenseTx.epochDay, updated.epochDay)
        assertEquals(202611, updated.yearMonth)
        assertEquals("DIRECT_DEBIT", updated.paymentMethod)
        assertEquals("exp_housing_rent", updated.categoryId)
    }

    @Test
    fun test9_editingTwiceInARow_UsesTheLatestTransactionAsTheBase() = runTest {
        val viewModel = buildViewModel()

        viewModel.startEdit(expenseTx)
        viewModel.startEdit(incomeTx)

        val state = viewModel.uiState.value
        assertEquals(8L, state.editingTransactionId)
        assertEquals("INCOME", state.type)
        assertEquals(250000L, state.amountCents)
        assertEquals("Empresa", state.merchant)
        assertEquals("2500", state.rawAmount)
    }

    @Test
    fun test10_editingDoesNotCreateADuplicateRecord() = runTest {
        val viewModel = buildViewModel()

        viewModel.startEdit(expenseTx)
        viewModel.saveTransaction()
        viewModel.saveTransaction()

        assertEquals(3, transactionDao.count())
        assertEquals(1, transactionDao.getAllSync().count { it.id == expenseTx.id })
    }

    @Test
    fun test11_editingWithACustomContextTag_KeepsTheCustomContext() = runTest {
        val customTx = expenseTx.copy(id = 12L, contextTag = "GIMNASIO")
        transactionDao = FakeTransactionDao(listOf(customTx, incomeTx))
        val viewModel = buildViewModel()

        viewModel.startEdit(customTx)
        assertEquals("GIMNASIO", viewModel.uiState.value.selectedContextTag)

        viewModel.setRawAmount("30")
        viewModel.saveTransaction()

        assertEquals("GIMNASIO", transactionDao.getAllSync().first { it.id == 12L }.contextTag)
    }

    @Test
    fun test12_startNewEntry_KeepsTheCustomContextTagsAndRecentMerchants() = runTest {
        val store = FakeContextTagsStore(initialTags = setOf("GIMNASIO"))
        val viewModel = buildViewModel(store)

        viewModel.startEdit(expenseTx)
        viewModel.startNewEntry()

        val state = viewModel.uiState.value
        assertTrue(state.customContextTags.contains("GIMNASIO"))
        assertTrue(state.recentMerchants.contains("Continente"))
    }
}
