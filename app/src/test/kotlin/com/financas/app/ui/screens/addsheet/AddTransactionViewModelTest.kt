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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var smartPrefillEngine: SmartPrefillEngine

    private val historyTransactions = listOf(
        TransactionEntity(
            id = 1,
            type = "EXPENSE",
            amountCents = 2450L,
            yearMonth = 202610,
            epochDay = LocalDate.of(2026, 10, 10).toEpochDay(),
            categoryId = "exp_food_supermarket",
            contextTag = "CASA",
            merchant = "Continente",
            paymentMethod = "MBWAY",
            isFixed = false
        ),
        TransactionEntity(
            id = 2,
            type = "EXPENSE",
            amountCents = 55000L,
            yearMonth = 202610,
            epochDay = LocalDate.of(2026, 10, 1).toEpochDay(),
            categoryId = "exp_housing_rent",
            contextTag = "CASA",
            merchant = "Renda Senhorio",
            paymentMethod = "DIRECT_DEBIT",
            isFixed = true
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        categoryDao = FakeCategoryDao(DefaultSeed.getDefaultCategories())
        transactionDao = FakeTransactionDao(historyTransactions)
        smartPrefillEngine = SmartPrefillEngine(transactionDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun criterion2_autoPrefillsAllDimensions_whenMerchantChipSelected() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        // Verify recent merchants contains "Continente" and "Renda Senhorio"
        val stateInitial = viewModel.uiState.value
        assertTrue(stateInitial.recentMerchants.contains("Continente"))
        assertTrue(stateInitial.recentMerchants.contains("Renda Senhorio"))

        // Select "Continente" chip
        viewModel.onMerchantChipSelected("Continente")

        val stateAfterContinente = viewModel.uiState.value
        assertEquals("Continente", stateAfterContinente.merchant)
        assertEquals("exp_food_supermarket", stateAfterContinente.selectedCategoryId)
        assertEquals("CASA", stateAfterContinente.selectedContextTag)
        assertEquals("MBWAY", stateAfterContinente.selectedPaymentMethod)
        assertFalse(stateAfterContinente.isFixed)

        // Now select "Renda Senhorio" chip
        viewModel.onMerchantChipSelected("Renda Senhorio")

        val stateAfterRenda = viewModel.uiState.value
        assertEquals("Renda Senhorio", stateAfterRenda.merchant)
        assertEquals("exp_housing_rent", stateAfterRenda.selectedCategoryId)
        assertEquals("CASA", stateAfterRenda.selectedContextTag)
        assertEquals("DIRECT_DEBIT", stateAfterRenda.selectedPaymentMethod)
        assertTrue(stateAfterRenda.isFixed)
    }

    @Test
    fun calculatorNumpad_updatesAmountStateCorrectly() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        assertEquals("0", viewModel.uiState.value.rawAmount)
        assertEquals(0L, viewModel.uiState.value.amountCents)

        // Type "12,50+3,50" -> 1600L
        viewModel.setRawAmount("12,50+3,50")
        assertEquals("12,50+3,50", viewModel.uiState.value.rawAmount)
        assertEquals(1600L, viewModel.uiState.value.amountCents)
    }

    @Test
    fun saveTransaction_insertsRecordInDao() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        // Prepare transaction
        viewModel.onMerchantChipSelected("Continente")
        viewModel.onDigit('2')
        viewModel.onDigit('5')
        viewModel.onComma()
        viewModel.onDigit('0')
        viewModel.onDigit('0') // 25,00 € = 2500L

        var callbackCalled = false
        viewModel.saveTransaction {
            callbackCalled = true
        }

        assertTrue(callbackCalled)
        assertTrue(viewModel.uiState.value.isSaved)

        val allTxs = transactionDao.getAllSync()
        assertEquals(3, allTxs.size)
        val inserted = allTxs.find { it.amountCents == 2500L }
        assertTrue(inserted != null)
        assertEquals("Continente", inserted?.merchant)
        assertEquals("exp_food_supermarket", inserted?.categoryId)
        assertEquals("CASA", inserted?.contextTag)
        assertEquals("MBWAY", inserted?.paymentMethod)
        assertEquals(false, inserted?.isFixed)
    }

    @Test
    fun defaults_expenseTypeComercioContextMbwayMethodTodayDateAndFirstCategory() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        val state = viewModel.uiState.value
        assertEquals("EXPENSE", state.type)
        assertEquals("COMERCIO", state.selectedContextTag)
        assertEquals("MBWAY", state.selectedPaymentMethod)
        assertFalse(state.isFixed)
        assertEquals(LocalDate.now().toEpochDay(), state.epochDay)
        assertEquals("0", state.rawAmount)
        assertEquals(0L, state.amountCents)
        assertEquals("", state.merchant)

        assertEquals(8, state.parentCategories.size)
        assertEquals("exp_housing", state.selectedParentCategoryId)
        assertEquals(5, state.subcategories.size)
        assertEquals("exp_housing_rent", state.selectedCategoryId)
    }

    @Test
    fun typeSwitchToIncome_selectsIncomeCategoryAndSavesIncomeRecord() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        viewModel.onTypeChanged("INCOME")

        val state = viewModel.uiState.value
        assertEquals("INCOME", state.type)
        assertEquals(listOf("inc_fixed", "inc_extra"), state.parentCategories.map { it.id })
        assertEquals("inc_fixed", state.selectedParentCategoryId)
        assertEquals(listOf("inc_base_salary", "inc_meal_allowance"), state.subcategories.map { it.id })
        assertEquals("inc_base_salary", state.selectedCategoryId)

        viewModel.onDigit('1')
        viewModel.onDigit('0')
        viewModel.onDigit('0')
        viewModel.saveTransaction()

        val inserted = transactionDao.getAllSync().first { it.type == "INCOME" }
        assertEquals(10000L, inserted.amountCents)
        assertEquals("inc_base_salary", inserted.categoryId)
        val savedDate = LocalDate.ofEpochDay(inserted.epochDay)
        assertEquals(savedDate.year * 100 + savedDate.monthValue, inserted.yearMonth)
    }

    @Test
    fun saveWithZeroAmount_isNoOpAndDaoUnchanged() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        viewModel.onDigit('0')
        assertEquals(0L, viewModel.uiState.value.amountCents)

        var callbackCalled = false
        viewModel.saveTransaction { callbackCalled = true }

        assertFalse(callbackCalled)
        assertFalse(viewModel.uiState.value.isSaved)
        assertEquals(2, transactionDao.count())
    }

    @Test
    fun expressionAddition_12_5plus3_evaluatesTo1550Cents() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        viewModel.setRawAmount("12,5+3")

        assertEquals("12,5+3", viewModel.uiState.value.rawAmount)
        assertEquals(1550L, viewModel.uiState.value.amountCents)
    }

    @Test
    fun merchantWithSurroundingSpaces_isTrimmedOnlyOnSave() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        viewModel.onMerchantChanged("  Continente  ")
        assertEquals("  Continente  ", viewModel.uiState.value.merchant)

        viewModel.onDigit('2')
        viewModel.onDigit('5')
        viewModel.saveTransaction()

        val inserted = transactionDao.getAllSync().first { it.amountCents == 2500L }
        assertEquals("Continente", inserted.merchant)
    }

    @Test
    fun saveWithoutCategories_fallsBackToExpFinancialOther() = runTest {
        val emptyCategoryDao = FakeCategoryDao()
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = emptyCategoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        assertEquals("", viewModel.uiState.value.selectedCategoryId)

        viewModel.onDigit('5')
        viewModel.saveTransaction()

        val inserted = transactionDao.getAllSync().first { it.amountCents == 500L }
        assertEquals("exp_financial_other", inserted.categoryId)
        assertEquals("COMERCIO", inserted.contextTag)
    }

    @Test
    fun fixedToggle_isPersistedOnSavedTransaction() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        viewModel.onFixedToggled(true)
        assertTrue(viewModel.uiState.value.isFixed)

        viewModel.onDigit('3')
        viewModel.saveTransaction()

        val inserted = transactionDao.getAllSync().first { it.amountCents == 300L }
        assertTrue(inserted.isFixed)

        viewModel.onFixedToggled(false)
        assertFalse(viewModel.uiState.value.isFixed)
    }

    @Test
    fun unknownMerchantChip_setsMerchantOnlyWithoutTouchingPickers() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        viewModel.onMerchantChipSelected("Loja Desconhecida")

        val state = viewModel.uiState.value
        assertEquals("Loja Desconhecida", state.merchant)
        assertEquals("exp_housing_rent", state.selectedCategoryId)
        assertEquals("exp_housing", state.selectedParentCategoryId)
        assertEquals("COMERCIO", state.selectedContextTag)
        assertEquals("MBWAY", state.selectedPaymentMethod)
        assertFalse(state.isFixed)
    }

    @Test
    fun recentMerchantsChips_distinctLimitTenMostRecentFirstNoBlanks() = runTest {
        val manyTx = (1L..12L).map { i ->
            val merchant = when {
                i == 5L -> ""
                i == 3L -> "Café Central"
                i == 8L -> "Café Central"
                else -> "M$i"
            }
            TransactionEntity(
                id = i,
                type = "EXPENSE",
                amountCents = 100L * i,
                yearMonth = 202610,
                epochDay = LocalDate.of(2026, 10, i.toInt()).toEpochDay(),
                categoryId = "exp_food_cafe",
                contextTag = "SAIDA",
                merchant = merchant,
                paymentMethod = "MBWAY"
            )
        }
        val manyDao = FakeTransactionDao(manyTx)

        val viewModel = AddTransactionViewModel(
            transactionDao = manyDao,
            categoryDao = categoryDao,
            smartPrefillEngine = SmartPrefillEngine(manyDao),
            contextTagsStore = FakeContextTagsStore()
        )

        val recent = viewModel.uiState.value.recentMerchants
        assertEquals(10, recent.size)
        assertEquals(
            listOf("M12", "M11", "M10", "M9", "Café Central", "M7", "M6", "M4", "M2", "M1"),
            recent
        )
        assertEquals(recent.size, recent.distinct().size)
        assertTrue(recent.none { it.isBlank() })
    }

    @Test
    fun displayFollowsTyping_stepByStepRawAndCents() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        viewModel.onDigit('1')
        assertEquals("1", viewModel.uiState.value.rawAmount)
        assertEquals(100L, viewModel.uiState.value.amountCents)

        viewModel.onDigit('2')
        assertEquals("12", viewModel.uiState.value.rawAmount)
        assertEquals(1200L, viewModel.uiState.value.amountCents)

        viewModel.onComma()
        assertEquals("12,", viewModel.uiState.value.rawAmount)
        assertEquals(1200L, viewModel.uiState.value.amountCents)

        viewModel.onDigit('5')
        assertEquals("12,5", viewModel.uiState.value.rawAmount)
        assertEquals(1250L, viewModel.uiState.value.amountCents)

        viewModel.onPlus()
        assertEquals("12,5+", viewModel.uiState.value.rawAmount)
        assertEquals(1250L, viewModel.uiState.value.amountCents)

        viewModel.onDigit('3')
        assertEquals("12,5+3", viewModel.uiState.value.rawAmount)
        assertEquals(1550L, viewModel.uiState.value.amountCents)

        viewModel.onBackspace()
        assertEquals("12,5+", viewModel.uiState.value.rawAmount)
        assertEquals(1250L, viewModel.uiState.value.amountCents)

        viewModel.onClear()
        assertEquals("0", viewModel.uiState.value.rawAmount)
        assertEquals(0L, viewModel.uiState.value.amountCents)
    }

    @Test
    fun startEdit_fillsAllFieldsIncludingEditingTransactionId() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        val target = historyTransactions.first { it.merchant == "Continente" }
        viewModel.startEdit(target)

        val state = viewModel.uiState.value
        assertEquals(target.id, state.editingTransactionId)
        assertEquals("EXPENSE", state.type)
        assertEquals("24,50", state.rawAmount)
        assertEquals(2450L, state.amountCents)
        assertEquals("Continente", state.merchant)
        assertEquals("exp_food_supermarket", state.selectedCategoryId)
        assertEquals("exp_food", state.selectedParentCategoryId)
        assertEquals("CASA", state.selectedContextTag)
        assertEquals("MBWAY", state.selectedPaymentMethod)
        assertFalse(state.isFixed)
        assertEquals(target.epochDay, state.epochDay)
        assertTrue(state.subcategories.any { it.id == "exp_food_supermarket" })
        assertTrue(state.parentCategories.any { it.id == "exp_food" })
    }

    @Test
    fun saveInEdit_callsUpdatePreservingIdAndCount() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        val target = historyTransactions.first { it.merchant == "Continente" }
        viewModel.startEdit(target)

        viewModel.setRawAmount("30,25")
        viewModel.onMerchantChanged("Continente Bom")

        viewModel.saveTransaction()

        assertEquals(2, transactionDao.count())
        val updated = transactionDao.getAllSync().first { it.id == target.id }
        assertEquals(3025L, updated.amountCents)
        assertEquals("Continente Bom", updated.merchant)
        assertEquals("CASA", updated.contextTag)
        assertEquals("MBWAY", updated.paymentMethod)
        assertEquals("EXPENSE", updated.type)
        assertEquals(202610, updated.yearMonth)
        assertFalse(updated.isFixed)
    }

    @Test
    fun saveWithoutEdit_stillInsertsNewRecord() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        assertNull(viewModel.uiState.value.editingTransactionId)

        viewModel.onDigit('9')
        viewModel.saveTransaction()

        assertEquals(3, transactionDao.count())
        val inserted = transactionDao.getAllSync().first { it.amountCents == 900L }
        assertTrue(inserted.id !in historyTransactions.map { it.id })
    }

    @Test
    fun startNewEntry_resetsStateToDefaultsAfterEdit() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        viewModel.startEdit(historyTransactions[1])
        viewModel.onTypeChanged("INCOME")

        viewModel.startNewEntry()

        val state = viewModel.uiState.value
        assertNull(state.editingTransactionId)
        assertEquals("EXPENSE", state.type)
        assertEquals("0", state.rawAmount)
        assertEquals(0L, state.amountCents)
        assertEquals("", state.merchant)
        assertFalse(state.isFixed)
        assertEquals(LocalDate.now().toEpochDay(), state.epochDay)
        assertEquals("COMERCIO", state.selectedContextTag)
        assertEquals("MBWAY", state.selectedPaymentMethod)
        assertEquals("exp_housing", state.selectedParentCategoryId)
        assertEquals("exp_housing_rent", state.selectedCategoryId)
        assertTrue(state.recentMerchants.contains("Continente"))

        viewModel.onDigit('1')
        viewModel.onDigit('0')
        viewModel.saveTransaction()

        assertEquals(3, transactionDao.count())
        assertTrue(transactionDao.getAllSync().none { it.id == historyTransactions[1].id && it.amountCents == 1000L })
    }

    @Test
    fun deleteTransaction_removesEditedTransactionAndResetsState() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        val target = historyTransactions.first { it.merchant == "Continente" }
        viewModel.startEdit(target)
        assertEquals(target.id, viewModel.uiState.value.editingTransactionId)

        viewModel.deleteTransaction()

        assertEquals(1, transactionDao.count())
        assertTrue(transactionDao.getAllSync().none { it.id == target.id })
        assertNull(viewModel.uiState.value.editingTransactionId)
        assertEquals("EXPENSE", viewModel.uiState.value.type)
        assertEquals("0", viewModel.uiState.value.rawAmount)
        assertEquals(0L, viewModel.uiState.value.amountCents)
    }

    @Test
    fun deleteTransaction_expenseWithLinkedRefund_unlinksRefundAndKeepsIncome() = runTest {
        val expense = TransactionEntity(
            id = 10L,
            type = "EXPENSE",
            amountCents = 2000L,
            yearMonth = 202610,
            epochDay = LocalDate.of(2026, 10, 5).toEpochDay(),
            categoryId = "exp_food_supermarket",
            contextTag = "CASA",
            merchant = "Continente",
            paymentMethod = "MBWAY"
        )
        val refund = TransactionEntity(
            id = 11L,
            type = "INCOME",
            amountCents = 500L,
            yearMonth = 202610,
            epochDay = LocalDate.of(2026, 10, 6).toEpochDay(),
            categoryId = "inc_other",
            contextTag = "SAIDA",
            merchant = "Reembolso: Continente",
            paymentMethod = "MBWAY",
            linkedTransactionId = 10L
        )
        val dao = FakeTransactionDao(listOf(expense, refund))
        val viewModel = buildAddViewModelWith(dao)

        viewModel.startEdit(expense)
        assertTrue(viewModel.uiState.value.hasLinkedRefunds)

        viewModel.deleteTransaction()

        assertEquals(1, dao.count())
        val surviving = dao.getAllSync().single()
        assertEquals(11L, surviving.id)
        assertEquals("INCOME", surviving.type)
        assertNull(surviving.linkedTransactionId)
        assertNull(viewModel.uiState.value.editingTransactionId)
    }

    @Test
    fun deleteTransaction_expenseWithoutRefunds_deletesOnlyTheTransaction() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        val target = historyTransactions.first { it.merchant == "Renda Senhorio" }
        viewModel.startEdit(target)
        assertFalse(viewModel.uiState.value.hasLinkedRefunds)

        viewModel.deleteTransaction()

        assertEquals(1, transactionDao.count())
        val remaining = transactionDao.getAllSync().single()
        assertEquals(historyTransactions.first { it.merchant == "Continente" }.id, remaining.id)
    }

    @Test
    fun dateSelection_setsEpochDayAndSavedEntityDerivesYearMonth() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        val chosenDate = LocalDate.of(2026, 9, 15)
        viewModel.onDateSelected(chosenDate.toEpochDay())

        assertEquals(chosenDate.toEpochDay(), viewModel.uiState.value.epochDay)

        viewModel.onDigit('1')
        viewModel.onDigit('0')
        viewModel.saveTransaction()

        val inserted = transactionDao.getAllSync().first { it.amountCents == 1000L }
        assertEquals(chosenDate.toEpochDay(), inserted.epochDay)
        assertEquals(202609, inserted.yearMonth)
    }

    @Test
    fun startEditRawAmount_roundTripsExactThroughEvaluator() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        viewModel.startEdit(historyTransactions[0].copy(id = 99L, amountCents = 1550L))
        assertEquals("15,50", viewModel.uiState.value.rawAmount)
        assertEquals(1550L, viewModel.uiState.value.amountCents)

        viewModel.startEdit(historyTransactions[0].copy(id = 99L, amountCents = 2500L))
        assertEquals("25", viewModel.uiState.value.rawAmount)
        assertEquals(2500L, viewModel.uiState.value.amountCents)

        viewModel.startEdit(historyTransactions[0].copy(id = 99L, amountCents = 5L))
        assertEquals("0,05", viewModel.uiState.value.rawAmount)
        assertEquals(5L, viewModel.uiState.value.amountCents)

        viewModel.startEdit(historyTransactions[0].copy(id = 99L, amountCents = 1550L))
        viewModel.onBackspace()
        assertEquals("15,5", viewModel.uiState.value.rawAmount)
        assertEquals(1550L, viewModel.uiState.value.amountCents)
    }

    @Test
    fun customContextTags_areLoadedFromStoreIntoState() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore(initialTags = setOf("PRAIA"))
        )

        assertTrue(viewModel.uiState.value.customContextTags.contains("PRAIA"))
    }

    @Test
    fun saveTransaction_asRefund_persistsLinkedTransactionId() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        viewModel.onTypeChanged("INCOME")
        viewModel.onRefundToggled(true)
        viewModel.onLinkedTransactionSelected(1L)
        viewModel.setRawAmount("40")

        var saved = false
        viewModel.saveTransaction { saved = true }

        assertTrue(saved)
        val all = transactionDao.getAllSync()
        val savedRefund = all.find { it.type == "INCOME" && it.amountCents == 4000L }
        assertTrue(savedRefund != null)
        assertEquals(1L, savedRefund?.linkedTransactionId)
    }

    @Test
    fun startEdit_withRefund_populatesIsRefundAndLinkedTransactionId() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        val refundTx = TransactionEntity(
            id = 55L,
            type = "INCOME",
            amountCents = 1500L,
            yearMonth = 202610,
            epochDay = LocalDate.now().toEpochDay(),
            categoryId = "inc_other",
            contextTag = "SAIDA",
            merchant = "Amigo MB WAY",
            paymentMethod = "MBWAY",
            linkedTransactionId = 1L
        )

        viewModel.startEdit(refundTx)
        assertTrue(viewModel.uiState.value.isRefund)
        assertEquals(1L, viewModel.uiState.value.linkedTransactionId)
        assertEquals(55L, viewModel.uiState.value.editingTransactionId)
    }

    @Test
    fun onTypeChanged_toExpense_resetsIsRefundAndLinkedTransactionId() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        viewModel.onTypeChanged("INCOME")
        viewModel.onRefundToggled(true)
        viewModel.onLinkedTransactionSelected(1L)
        assertTrue(viewModel.uiState.value.isRefund)
        assertEquals(1L, viewModel.uiState.value.linkedTransactionId)

        viewModel.onTypeChanged("EXPENSE")
        assertFalse(viewModel.uiState.value.isRefund)
        assertNull(viewModel.uiState.value.linkedTransactionId)
    }

    @Test
    fun createCategory_asSubcategory_insertsAndSelectsIt() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        val parentId = "exp_housing"
        viewModel.createCategory(
            name = "Condomínio",
            parentId = parentId,
            type = "EXPENSE"
        )

        val state = viewModel.uiState.value
        assertEquals(parentId, state.selectedParentCategoryId)
        val inserted = categoryDao.getAllSync().find { it.name == "Condomínio" }
        assertTrue(inserted != null)
        assertEquals(inserted?.id, state.selectedCategoryId)
        assertEquals(parentId, inserted?.parentId)
    }

    @Test
    fun createCategory_asParent_insertsAndSelectsIt() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        viewModel.createCategory(
            name = "Animais de Estimação",
            parentId = null,
            type = "EXPENSE"
        )

        val state = viewModel.uiState.value
        val inserted = categoryDao.getAllSync().find { it.name == "Animais de Estimação" }
        assertTrue(inserted != null)
        assertEquals(inserted?.id, state.selectedParentCategoryId)
        assertEquals(inserted?.id, state.selectedCategoryId)
    }

    @Test
    fun onParentCategorySelected_switchesSubcategories() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        viewModel.onParentCategorySelected("exp_transport")
        val state = viewModel.uiState.value
        assertEquals("exp_transport", state.selectedParentCategoryId)
        assertTrue(state.subcategories.isNotEmpty())
        assertTrue(state.subcategories.all { it.parentId == "exp_transport" })
    }

    @Test
    fun onPaymentMethodSelected_cashSetsCashPaymentMethodAndSavesCorrectly() = runTest {
        val viewModel = AddTransactionViewModel(
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            smartPrefillEngine = smartPrefillEngine,
            contextTagsStore = FakeContextTagsStore()
        )

        viewModel.onPaymentMethodSelected("CASH")
        assertEquals("CASH", viewModel.uiState.value.selectedPaymentMethod)

        viewModel.setRawAmount("1,50")
        viewModel.onMerchantChanged("Café Central")
        viewModel.onParentCategorySelected("exp_food")
        viewModel.onSubcategorySelected("exp_food_cafe")

        var saved = false
        viewModel.saveTransaction(onSuccess = { saved = true })

        assertTrue(saved)
        val allTxs = transactionDao.getAllSync()
        val savedTx = allTxs.find { it.merchant == "Café Central" }
        assertTrue(savedTx != null)
        assertEquals("CASH", savedTx?.paymentMethod)
        assertEquals(150L, savedTx?.amountCents)
    }

    private fun buildAddViewModelWith(dao: FakeTransactionDao): AddTransactionViewModel =
        AddTransactionViewModel(
            transactionDao = dao,
            categoryDao = categoryDao,
            smartPrefillEngine = SmartPrefillEngine(dao),
            contextTagsStore = FakeContextTagsStore()
        )
}
