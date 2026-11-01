package com.financas.app.data.prefs

import com.financas.app.data.seed.DefaultSeed
import com.financas.app.domain.usecase.SmartPrefillEngine
import com.financas.app.ui.fakes.FakeBudgetDao
import com.financas.app.ui.fakes.FakeCategoryDao
import com.financas.app.ui.fakes.FakeContextTagsStore
import com.financas.app.ui.fakes.FakeGoalDao
import com.financas.app.ui.fakes.FakeTransactionDao
import com.financas.app.ui.screens.account.AccountViewModel
import com.financas.app.ui.screens.account.BiometricLockGate
import com.financas.app.ui.screens.account.FakeBiometricPreferenceStore
import com.financas.app.ui.screens.addsheet.AddTransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContextTagsPersistenceTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var transactionDao: FakeTransactionDao

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        categoryDao = FakeCategoryDao(DefaultSeed.getDefaultCategories())
        transactionDao = FakeTransactionDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildAccountViewModel(store: FakeContextTagsStore): AccountViewModel = AccountViewModel(
        categoryDao = categoryDao,
        transactionDao = transactionDao,
        budgetDao = FakeBudgetDao(),
        goalDao = FakeGoalDao(),
        biometricLockGate = BiometricLockGate(FakeBiometricPreferenceStore(enabled = false)),
        contextTagsStore = store,
        todayYearMonth = { 202610 }
    )

    private fun buildAddSheetViewModel(store: FakeContextTagsStore) = AddTransactionViewModel(
        transactionDao = transactionDao,
        categoryDao = categoryDao,
        smartPrefillEngine = SmartPrefillEngine(transactionDao),
        contextTagsStore = store
    )

    @Test
    fun test1_store_WritesAndReadsBackTheCustomTags() {
        val store = FakeContextTagsStore()

        assertTrue(store.getCustomTags().isEmpty())

        store.addTag("GIMNASIO")
        store.addTag("PRAIA")

        assertEquals(setOf("GIMNASIO", "PRAIA"), store.getCustomTags())
    }

    @Test
    fun test2_accountScreen_StartsWithThePersistedTags() = runTest {
        val store = FakeContextTagsStore(initialTags = setOf("GIMNASIO", "PRAIA"))

        val viewModel = buildAccountViewModel(store)
        advanceUntilIdle()

        val tags = viewModel.uiState.value.contextTags
        assertTrue(tags.contains("GIMNASIO"))
        assertTrue(tags.contains("PRAIA"))
        assertTrue("Os contextos por omissão têm de continuar disponíveis", tags.containsAll(DefaultSeed.CONTEXTS.map { it.tag }))
    }

    @Test
    fun test3_addSheet_StartsWithThePersistedTags() = runTest {
        val store = FakeContextTagsStore(initialTags = setOf("GIMNASIO"))

        val viewModel = buildAddSheetViewModel(store)

        assertTrue(viewModel.uiState.value.customContextTags.contains("GIMNASIO"))
    }

    @Test
    fun test4_addingATagPersistsItAndANewSessionReloadsIt() = runTest {
        val store = FakeContextTagsStore()

        val firstSession = buildAccountViewModel(store)
        advanceUntilIdle()

        firstSession.addContextTag("Gimnasio")
        advanceUntilIdle()

        assertTrue(store.getCustomTags().contains("GIMNASIO"))
        assertEquals("Contexto \"GIMNASIO\" adicionado.", firstSession.uiState.value.message)

        val secondSession = buildAccountViewModel(store)
        advanceUntilIdle()

        assertTrue(secondSession.uiState.value.contextTags.contains("GIMNASIO"))
    }

    @Test
    fun test5_tagsAreNormalizedToUppercaseWithUnderscores() = runTest {
        val store = FakeContextTagsStore()

        val viewModel = buildAccountViewModel(store)
        advanceUntilIdle()

        viewModel.addContextTag("casa de praia")
        advanceUntilIdle()

        assertTrue(store.getCustomTags().contains("CASA_DE_PRAIA"))
        assertTrue(viewModel.uiState.value.contextTags.contains("CASA_DE_PRAIA"))
        assertEquals("Contexto \"CASA_DE_PRAIA\" adicionado.", viewModel.uiState.value.message)
    }

    @Test
    fun test6_blankTagsAreIgnoredAndDoNotChangeTheState() = runTest {
        val store = FakeContextTagsStore()

        val viewModel = buildAccountViewModel(store)
        advanceUntilIdle()

        val initialTags = viewModel.uiState.value.contextTags
        viewModel.addContextTag("   ")
        advanceUntilIdle()

        assertTrue(store.getCustomTags().isEmpty())
        assertEquals(initialTags, viewModel.uiState.value.contextTags)
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun test7_addSheet_ShowsTheCustomTagsAndAllowsSelectingThem() = runTest {
        val store = FakeContextTagsStore(initialTags = setOf("GIMNASIO"))

        val viewModel = buildAddSheetViewModel(store)

        assertEquals(setOf("GIMNASIO"), viewModel.uiState.value.customContextTags)

        viewModel.onContextSelected("GIMNASIO")

        assertEquals("GIMNASIO", viewModel.uiState.value.selectedContextTag)
    }

    @Test
    fun test8_addSheet_SavesTheTransactionWithTheSelectedCustomTag() = runTest {
        val store = FakeContextTagsStore(initialTags = setOf("GIMNASIO"))

        val viewModel = buildAddSheetViewModel(store)

        viewModel.onContextSelected("GIMNASIO")
        viewModel.onMerchantChanged("Ginasio")
        viewModel.onDigit('2')
        viewModel.onDigit('0')
        viewModel.saveTransaction()

        val inserted = transactionDao.getAllSync().single()
        assertEquals("GIMNASIO", inserted.contextTag)
        assertEquals("Ginasio", inserted.merchant)
        assertEquals(2000L, inserted.amountCents)
    }

    @Test
    fun test9_defaultContextsAreAlwaysAvailableNextToTheCustomOnes() = runTest {
        val store = FakeContextTagsStore(initialTags = setOf("GIMNASIO"))

        val viewModel = buildAccountViewModel(store)
        advanceUntilIdle()

        val tags = viewModel.uiState.value.contextTags
        assertEquals(DefaultSeed.CONTEXTS.size + 1, tags.size)
        assertEquals(DefaultSeed.CONTEXTS.size, tags.count { it in DefaultSeed.CONTEXTS.map { ctx -> ctx.tag } })
        assertEquals(1, tags.count { it == "GIMNASIO" })
    }
}
