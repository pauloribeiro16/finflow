package com.financas.app.ui.screens.account

import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.repository.BackupSerializer
import com.financas.app.ui.fakes.FakeBudgetDao
import com.financas.app.ui.fakes.FakeCategoryDao
import com.financas.app.ui.fakes.FakeContextTagsStore
import com.financas.app.ui.fakes.FakeGoalDao
import com.financas.app.ui.fakes.FakeTransactionDao
import com.financas.app.ui.fakes.FakeUserProfileStore
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
class AccountViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var budgetDao: FakeBudgetDao
    private lateinit var goalDao: FakeGoalDao
    private lateinit var fakePrefStore: FakeBiometricPreferenceStore
    private lateinit var biometricGate: BiometricLockGate
    private lateinit var backupSerializer: BackupSerializer
    private lateinit var contextTagsStore: FakeContextTagsStore
    private lateinit var userProfileStore: FakeUserProfileStore
    private lateinit var viewModel: AccountViewModel

    private val sampleParentCategory = CategoryEntity(
        id = "exp_housing",
        parentId = null,
        name = "Habitação",
        icon = "home",
        colorHex = 0xFF1976D2,
        type = "EXPENSE",
        isDefaultFixed = true,
        sortOrder = 1
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        categoryDao = FakeCategoryDao(listOf(sampleParentCategory))
        transactionDao = FakeTransactionDao()
        budgetDao = FakeBudgetDao()
        goalDao = FakeGoalDao()
        fakePrefStore = FakeBiometricPreferenceStore(enabled = false)
        biometricGate = BiometricLockGate(fakePrefStore)
        backupSerializer = BackupSerializer()
        contextTagsStore = FakeContextTagsStore()
        userProfileStore = FakeUserProfileStore()

        viewModel = buildViewModel()
    }

    private fun buildViewModel(
        todayYearMonth: () -> Int = {
            java.time.LocalDate.now().let { it.year * 100 + it.monthValue }
        },
        profileStore: FakeUserProfileStore = userProfileStore
    ): AccountViewModel = AccountViewModel(
        categoryDao = categoryDao,
        transactionDao = transactionDao,
        budgetDao = budgetDao,
        goalDao = goalDao,
        biometricLockGate = biometricGate,
        contextTagsStore = contextTagsStore,
        backupSerializer = backupSerializer,
        userProfileStore = profileStore,
        todayYearMonth = todayYearMonth
    )

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialBiometricsStateIsFalse() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isBiometricsEnabled)
        assertFalse(biometricGate.isBiometricsEnabled())
    }

    @Test
    fun testToggleBiometrics() = runTest {
        advanceUntilIdle()
        viewModel.toggleBiometrics(true)
        assertTrue(viewModel.uiState.value.isBiometricsEnabled)
        assertTrue(biometricGate.isBiometricsEnabled())

        viewModel.toggleBiometrics(false)
        assertFalse(viewModel.uiState.value.isBiometricsEnabled)
        assertFalse(biometricGate.isBiometricsEnabled())
    }

    @Test
    fun testAddSubcategoryInheritsParentProperties() = runTest {
        advanceUntilIdle()
        viewModel.addSubcategory(
            parentId = "exp_housing",
            name = "Condomínio",
            icon = "apartment",
            isFixed = true
        )
        advanceUntilIdle()

        val allCategories = categoryDao.getAllSync()
        val created = allCategories.find { it.name == "Condomínio" }
        assertNotNull(created)
        assertEquals("exp_housing", created?.parentId)
        assertEquals(0xFF1976D2, created?.colorHex)
        assertEquals("EXPENSE", created?.type)
        assertTrue(created?.isDefaultFixed == true)
        assertEquals(2, allCategories.size)
    }

    @Test
    fun testDeleteSubcategory() = runTest {
        advanceUntilIdle()
        val sub = CategoryEntity(
            id = "exp_housing_water",
            parentId = "exp_housing",
            name = "Água",
            icon = "water_drop",
            colorHex = 0xFF1976D2,
            type = "EXPENSE"
        )
        categoryDao.insert(sub)
        advanceUntilIdle()

        assertEquals(2, categoryDao.count())

        viewModel.deleteSubcategory(sub)
        advanceUntilIdle()

        assertEquals(1, categoryDao.count())
    }

    @Test
    fun testAddContextTag() = runTest {
        advanceUntilIdle()
        val initialSize = viewModel.uiState.value.contextTags.size
        viewModel.addContextTag("GIMNASIO")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.contextTags.contains("GIMNASIO"))
        assertEquals(initialSize + 1, viewModel.uiState.value.contextTags.size)
    }

    @Test
    fun testContextTags_loadFromStoreAtInitAndPersistOnAdd() = runTest {
        advanceUntilIdle()
        val store = FakeContextTagsStore(initialTags = setOf("PRAIA"))
        val vm = AccountViewModel(
            categoryDao = categoryDao,
            transactionDao = transactionDao,
            budgetDao = budgetDao,
            goalDao = goalDao,
            biometricLockGate = biometricGate,
            contextTagsStore = store,
            backupSerializer = backupSerializer
        )
        advanceUntilIdle()

        assertTrue(vm.uiState.value.contextTags.contains("PRAIA"))

        vm.addContextTag("Gimnasio Novo")
        advanceUntilIdle()

        assertTrue(store.getCustomTags().contains("GIMNASIO_NOVO"))
        assertTrue(vm.uiState.value.contextTags.contains("GIMNASIO_NOVO"))
        assertEquals("Contexto \"GIMNASIO_NOVO\" adicionado.", vm.uiState.value.message)

        assertFalse(store.getCustomTags().contains("PRAIA_X"))
        store.addTag("PRAIA_X")
        assertTrue(store.getCustomTags().contains("PRAIA_X"))
    }

    @Test
    fun testExportAndImportJsonFlow() = runTest {
        advanceUntilIdle()
        transactionDao.insert(
            TransactionEntity(
                id = 1L,
                type = "EXPENSE",
                amountCents = 3500L,
                yearMonth = 202610,
                epochDay = LocalDate.of(2026, 10, 20).toEpochDay(),
                categoryId = "exp_housing",
                contextTag = "CASA",
                merchant = "EDP",
                paymentMethod = "DIRECT_DEBIT",
                isFixed = true
            )
        )

        val exportedJson = viewModel.exportJson()
        assertTrue(exportedJson.contains("Habitação"))
        assertTrue(exportedJson.contains("EDP"))

        val exportedCsv = viewModel.exportCsv()
        assertTrue(exportedCsv.contains("35,00"))
        assertTrue(exportedCsv.contains("Habitação"))

        // Clear DAOs and restore
        categoryDao.delete(sampleParentCategory)
        transactionDao.deleteById(1L)
        assertEquals(0, categoryDao.count())
        assertEquals(0, transactionDao.count())

        val importResult = viewModel.importJson(exportedJson)
        advanceUntilIdle()

        assertTrue(importResult.isSuccess)
        assertEquals(1, categoryDao.count())
        assertEquals(1, transactionDao.count())
    }

    @Test
    fun testAddSubcategory_sanitizedUniqueIdBadgeFixoAndExactMessage_blankNameIsNoOp() = runTest {
        advanceUntilIdle()
        viewModel.addSubcategory(
            parentId = "exp_housing",
            name = "Água Quente",
            icon = "water_drop",
            isFixed = true
        )
        advanceUntilIdle()

        val allCategories = categoryDao.getAllSync()
        assertEquals(2, allCategories.size)
        val created = allCategories.find { it.name == "Água Quente" }
        assertNotNull(created)
        assertTrue(created!!.id.startsWith("exp_housing_"))
        assertTrue(created.id.contains("água_quente"))
        assertTrue("O id tem de ser único", allCategories.count { it.id == created.id } == 1)
        assertEquals("exp_housing", created.parentId)
        assertEquals(0xFF1976D2, created.colorHex)
        assertEquals("EXPENSE", created.type)
        assertTrue(created.isDefaultFixed)
        assertEquals(
            "Subcategoria \"Água Quente\" criada com sucesso.",
            viewModel.uiState.value.message
        )

        viewModel.addSubcategory(parentId = "exp_housing", name = "   ")
        advanceUntilIdle()

        assertEquals(2, categoryDao.count())
        assertEquals(
            "Nome em branco não pode alterar a mensagem",
            "Subcategoria \"Água Quente\" criada com sucesso.",
            viewModel.uiState.value.message
        )
    }

    @Test
    fun testDeleteSubcategory_showsExactMessage_andContextTagNormalizesToUppercaseSnake() = runTest {
        advanceUntilIdle()
        val sub = CategoryEntity(
            id = "exp_housing_agua",
            parentId = "exp_housing",
            name = "Água",
            icon = "water_drop",
            colorHex = 0xFF1976D2,
            type = "EXPENSE"
        )
        categoryDao.insert(sub)
        advanceUntilIdle()
        assertEquals(2, categoryDao.count())

        viewModel.deleteSubcategory(sub)
        advanceUntilIdle()

        assertEquals(1, categoryDao.count())
        assertEquals("Subcategoria \"Água\" eliminada.", viewModel.uiState.value.message)

        val initialTags = viewModel.uiState.value.contextTags
        viewModel.addContextTag("Casa Nova")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.contextTags.contains("CASA_NOVA"))
        assertEquals(initialTags.size + 1, viewModel.uiState.value.contextTags.size)
        assertEquals("Contexto \"CASA_NOVA\" adicionado.", viewModel.uiState.value.message)
    }

    @Test
    fun testExportJsonVersionAndCsvHeader_restoreMessagesForValidAndInvalidPayloads() = runTest {
        advanceUntilIdle()
        transactionDao.insertAll(
            listOf(
                TransactionEntity(
                    id = 1L,
                    type = "EXPENSE",
                    amountCents = 3500L,
                    yearMonth = 202610,
                    epochDay = LocalDate.of(2026, 10, 20).toEpochDay(),
                    categoryId = "exp_housing",
                    contextTag = "CASA",
                    merchant = "EDP",
                    paymentMethod = "DIRECT_DEBIT",
                    isFixed = true
                ),
                TransactionEntity(
                    id = 2L,
                    type = "INCOME",
                    amountCents = 120000L,
                    yearMonth = 202610,
                    epochDay = LocalDate.of(2026, 10, 1).toEpochDay(),
                    categoryId = "inc_fixed",
                    contextTag = "TRABALHO",
                    merchant = "Empresa",
                    paymentMethod = "TRANSFER",
                    isFixed = true
                )
            )
        )

        val exportedJson = viewModel.exportJson()
        assertTrue(exportedJson.startsWith("{\n  \"version\": 3,\n"))
        assertTrue(exportedJson.contains("\"Habitação\""))
        assertTrue(exportedJson.contains("EDP"))

        val exportedCsv = viewModel.exportCsv()
        assertTrue(exportedCsv.startsWith(BackupSerializer.CSV_HEADER + "\n"))
        assertEquals(3, exportedCsv.trim().split("\n").size)

        categoryDao.delete(sampleParentCategory)
        transactionDao.deleteById(1L)
        transactionDao.deleteById(2L)
        assertEquals(0, categoryDao.count())
        assertEquals(0, transactionDao.count())

        val validResult = viewModel.importJson(exportedJson)
        advanceUntilIdle()

        assertTrue(validResult.isSuccess)
        assertEquals(1, categoryDao.count())
        assertEquals(2, transactionDao.count())
        assertEquals(
            "Backup restaurado: 2 novos movimentos, 1 categorias sincronizadas, 0 orçamentos e 0 metas sincronizados.",
            viewModel.uiState.value.message
        )

        val invalidResult = viewModel.importJson("{invalid")
        advanceUntilIdle()

        assertTrue(invalidResult.isFailure)
        assertTrue(viewModel.uiState.value.message!!.startsWith("Erro ao restaurar backup: "))
        assertEquals(1, categoryDao.count())
        assertEquals(2, transactionDao.count())
    }

    @Test
    fun criterion8_addBudget_deterministicIdReplacesPerCategoryAndExactMessage() = runTest {
        advanceUntilIdle()

        viewModel.addBudget("exp_housing", 50000L)
        advanceUntilIdle()

        val budget = budgetDao.getById("budget_exp_housing")
        assertNotNull(budget)
        assertEquals("exp_housing", budget?.categoryId)
        assertEquals(50000L, budget?.limitCents)
        assertTrue(budget?.isActive == true)
        assertEquals(1, budgetDao.count())
        assertEquals(
            "Orçamento para \"Habitação\" definido: 500,00 €.",
            viewModel.uiState.value.message
        )
        assertTrue(viewModel.uiState.value.budgetedCategoryIds.contains("exp_housing"))

        viewModel.addBudget("exp_housing", 60000L)
        advanceUntilIdle()

        assertEquals(1, budgetDao.count())
        assertEquals(60000L, budgetDao.getById("budget_exp_housing")?.limitCents)
        assertEquals(
            "Orçamento para \"Habitação\" definido: 600,00 €.",
            viewModel.uiState.value.message
        )
    }

    @Test
    fun criterion8_updateAndDeleteBudget_exactMessages() = runTest {
        advanceUntilIdle()

        viewModel.addBudget("exp_housing", 50000L)
        advanceUntilIdle()
        val budget = budgetDao.getById("budget_exp_housing")!!

        viewModel.updateBudgetLimit(budget, 75000L)
        advanceUntilIdle()

        assertEquals(75000L, budgetDao.getById("budget_exp_housing")?.limitCents)
        assertEquals(
            "Orçamento para \"Habitação\" definido: 750,00 €.",
            viewModel.uiState.value.message
        )

        viewModel.deleteBudget(budgetDao.getById("budget_exp_housing")!!)
        advanceUntilIdle()

        assertEquals(0, budgetDao.count())
        assertEquals("Orçamento eliminado.", viewModel.uiState.value.message)
    }

    @Test
    fun criterion8_budget_invalidInputsAreNoOp() = runTest {
        advanceUntilIdle()

        viewModel.addBudget("", 50000L)
        viewModel.addBudget("exp_housing", 0L)
        viewModel.addBudget("exp_housing", -100L)
        advanceUntilIdle()

        assertEquals(0, budgetDao.count())

        viewModel.updateBudgetLimit(
            com.financas.app.data.entity.BudgetEntity("budget_exp_housing", "exp_housing", 50000L),
            0L
        )
        advanceUntilIdle()

        assertEquals(0, budgetDao.count())
    }

    @Test
    fun criterion8_goalCrud_createArchiveDeleteWithExactMessages() = runTest {
        advanceUntilIdle()

        val vm = buildViewModel(todayYearMonth = { 202610 })
        advanceUntilIdle()

        vm.createGoal("Viagem ao Japão", 300000L, 202712)
        advanceUntilIdle()

        val goals = goalDao.getAllSync()
        assertEquals(1, goals.size)
        val goal = goals.first()
        assertEquals("Viagem ao Japão", goal.name)
        assertEquals(300000L, goal.targetCents)
        assertEquals(202610, goal.startYearMonth)
        assertEquals(202712, goal.deadlineYearMonth)
        assertFalse(goal.archived)
        assertEquals("Meta \"Viagem ao Japão\" criada.", vm.uiState.value.message)

        vm.archiveGoal(goal)
        advanceUntilIdle()

        assertTrue(goalDao.getById(goal.id)?.archived == true)
        assertEquals("Meta \"Viagem ao Japão\" arquivada.", vm.uiState.value.message)

        vm.deleteGoal(goalDao.getById(goal.id)!!)
        advanceUntilIdle()

        assertEquals(0, goalDao.count())
        assertEquals("Meta eliminada.", vm.uiState.value.message)
    }

    @Test
    fun criterion8_goal_invalidInputsAndPastDeadlineAreNoOp() = runTest {
        val vm = buildViewModel(todayYearMonth = { 202610 })
        advanceUntilIdle()

        vm.createGoal("   ", 100000L)
        vm.createGoal("Fundo", 0L)
        vm.createGoal("Fundo", -50L)
        vm.createGoal("Fundo", 100000L, 202609)
        advanceUntilIdle()

        assertEquals(0, goalDao.count())
    }

    @Test
    fun criterion8_backupExportImport_preservesBudgetsAndGoals() = runTest {
        val vm = buildViewModel(todayYearMonth = { 202610 })
        advanceUntilIdle()

        vm.addBudget("exp_housing", 50000L)
        vm.createGoal("Fundo de Emergência", 200000L, 202710)
        advanceUntilIdle()

        assertEquals(1, budgetDao.count())
        assertEquals(1, goalDao.count())

        val exported = vm.exportJson()
        assertTrue(exported.startsWith("{\n  \"version\": 3,\n"))
        assertTrue(exported.contains("\"budgets\": ["))
        assertTrue(exported.contains("\"goals\": ["))
        assertTrue(exported.contains("budget_exp_housing"))
        assertTrue(exported.contains("Fundo de Emergência"))

        val goalId = goalDao.getAllSync().first().id
        budgetDao.deleteById("budget_exp_housing")
        goalDao.deleteById(goalId)
        assertEquals(0, budgetDao.count())
        assertEquals(0, goalDao.count())

        val result = vm.importJson(exported)
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertEquals(1, budgetDao.count())
        assertEquals(1, goalDao.count())
        assertEquals(50000L, budgetDao.getById("budget_exp_housing")?.limitCents)
        assertEquals(200000L, goalDao.getById(goalId)?.targetCents)
        assertEquals(1, result.getOrThrow().budgetsImported)
        assertEquals(1, result.getOrThrow().goalsImported)
        assertEquals(
            "Backup restaurado: 0 novos movimentos, 1 categorias sincronizadas, 1 orçamentos e 1 metas sincronizados.",
            vm.uiState.value.message
        )

        val secondResult = vm.importJson(exported)
        advanceUntilIdle()

        assertTrue(secondResult.isSuccess)
        assertEquals(1, budgetDao.count())
        assertEquals(1, goalDao.count())
    }

    @Test
    fun criterion8_goalId_isDeterministicForSameNameAndStartMonth() = runTest {
        val vm = buildViewModel(todayYearMonth = { 202610 })
        advanceUntilIdle()

        vm.createGoal("Fundo de Emergência", 200000L)
        advanceUntilIdle()

        val first = goalDao.getAllSync().single()
        assertEquals("goal_fundo_de_emergência_202610", first.id)

        vm.createGoal("Fundo de Emergência", 250000L)
        advanceUntilIdle()

        val second = goalDao.getAllSync().single()
        assertEquals(first.id, second.id)
        assertEquals(250000L, second.targetCents)
        assertEquals(1, goalDao.count())
    }

    @Test
    fun criterion8_goalProgress_accumulatedNetSavingsUpToCurrentMonth() = runTest {
        val vm = buildViewModel(todayYearMonth = { 202610 })
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
        advanceUntilIdle()

        vm.createGoal("Fundo de Emergência", 200000L)
        advanceUntilIdle()

        val items = vm.uiState.value.goalItems
        assertEquals(1, items.size)
        assertEquals(50000L, items[0].progress.accumulatedSavingsCents)
        assertEquals(25.0, items[0].progress.percentComplete, 0.001)
        assertEquals(50000L, items[0].progress.averageMonthlySavingsCents)
        assertEquals(3L, items[0].progress.projectedMonthsToComplete)
        assertNull(items[0].progress.monthsRemaining)
        assertNull(items[0].progress.onTrack)
    }

    @Test
    fun testProfile_initialValuesFromStore() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("Utilizador Teste", state.userName)
        assertNull(state.profilePhotoUri)
        assertEquals("EUR (€)", state.defaultCurrency)
        assertEquals(1, state.monthlyCycleDay)
    }

    @Test
    fun testProfile_customInitialValuesFromStore() = runTest {
        val customStore = FakeUserProfileStore(
            userName = "Utilizador Exemplo",
            photoUri = "content://images/avatar.jpg",
            currency = "USD ($)",
            cycleDay = 15
        )
        val vm = buildViewModel(profileStore = customStore)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Utilizador Exemplo", state.userName)
        assertEquals("content://images/avatar.jpg", state.profilePhotoUri)
        assertEquals("USD ($)", state.defaultCurrency)
        assertEquals(15, state.monthlyCycleDay)
    }

    @Test
    fun testProfile_updateUserName() = runTest {
        advanceUntilIdle()
        viewModel.updateUserName("Utilizador M. Teste")
        advanceUntilIdle()

        assertEquals("Utilizador M. Teste", viewModel.uiState.value.userName)
        assertEquals("Utilizador M. Teste", userProfileStore.getUserName())
        assertEquals("Perfil atualizado para \"Utilizador M. Teste\".", viewModel.uiState.value.message)
    }

    @Test
    fun testProfile_updateUserName_blankIgnored() = runTest {
        advanceUntilIdle()
        viewModel.updateUserName("   ")
        advanceUntilIdle()

        assertEquals("Utilizador Teste", viewModel.uiState.value.userName)
        assertEquals("Utilizador Teste", userProfileStore.getUserName())
    }

    @Test
    fun testProfile_updateProfilePhoto_andRemove() = runTest {
        advanceUntilIdle()
        viewModel.updateProfilePhoto("content://media/photos/123")
        advanceUntilIdle()

        assertEquals("content://media/photos/123", viewModel.uiState.value.profilePhotoUri)
        assertEquals("content://media/photos/123", userProfileStore.getProfilePhotoUri())
        assertEquals("Foto de perfil atualizada!", viewModel.uiState.value.message)

        viewModel.updateProfilePhoto(null)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.profilePhotoUri)
        assertNull(userProfileStore.getProfilePhotoUri())
        assertEquals("Foto de perfil removida.", viewModel.uiState.value.message)
    }

    @Test
    fun testProfile_updateDefaultCurrency() = runTest {
        advanceUntilIdle()
        viewModel.updateDefaultCurrency("GBP (£)")
        advanceUntilIdle()

        assertEquals("GBP (£)", viewModel.uiState.value.defaultCurrency)
        assertEquals("GBP (£)", userProfileStore.getDefaultCurrency())
    }

    @Test
    fun testProfile_updateMonthlyCycleDay() = runTest {
        advanceUntilIdle()
        viewModel.updateMonthlyCycleDay(25)
        advanceUntilIdle()

        assertEquals(25, viewModel.uiState.value.monthlyCycleDay)
        assertEquals(25, userProfileStore.getMonthlyCycleDay())

        // Invalid day (> 28) should be ignored
        viewModel.updateMonthlyCycleDay(31)
        advanceUntilIdle()

        assertEquals(25, viewModel.uiState.value.monthlyCycleDay)
        assertEquals(25, userProfileStore.getMonthlyCycleDay())

        // Invalid day (< 1) should be ignored
        viewModel.updateMonthlyCycleDay(0)
        advanceUntilIdle()

        assertEquals(25, viewModel.uiState.value.monthlyCycleDay)
        assertEquals(25, userProfileStore.getMonthlyCycleDay())
    }

    @Test
    fun testProfile_updateThemeMode() = runTest {
        advanceUntilIdle()
        assertEquals("SYSTEM", viewModel.uiState.value.themeMode)

        viewModel.updateThemeMode("DARK")
        advanceUntilIdle()
        assertEquals("DARK", viewModel.uiState.value.themeMode)
        assertEquals("DARK", userProfileStore.getThemeMode())

        viewModel.updateThemeMode("LIGHT")
        advanceUntilIdle()
        assertEquals("LIGHT", viewModel.uiState.value.themeMode)
        assertEquals("LIGHT", userProfileStore.getThemeMode())
    }

    @Test
    fun testProfile_privacyModeToggle() = runTest {
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isPrivacyMode)
        assertEquals(false, userProfileStore.isPrivacyModeEnabled())

        viewModel.setPrivacyMode(true)
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.isPrivacyMode)
        assertEquals(true, userProfileStore.isPrivacyModeEnabled())
        assertEquals(true, com.financas.app.domain.model.MoneyCents.isPrivacyModeEnabled)

        viewModel.setPrivacyMode(false)
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isPrivacyMode)
        assertEquals(false, userProfileStore.isPrivacyModeEnabled())
        assertEquals(false, com.financas.app.domain.model.MoneyCents.isPrivacyModeEnabled)
    }
}
