package com.financas.app.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.financas.app.data.db.BudgetDao
import com.financas.app.data.db.CategoryDao
import com.financas.app.data.db.GoalDao
import com.financas.app.data.db.InvestmentAssetDao
import com.financas.app.data.db.PendingTransactionDao
import com.financas.app.data.db.TransactionDao
import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.entity.PendingTransactionEntity
import com.financas.app.data.prefs.ContextTagsStore
import com.financas.app.data.prefs.UserProfileStore
import com.financas.app.data.repository.BackupSerializer
import com.financas.app.data.repository.RestoreResult
import com.financas.app.data.seed.DefaultSeed
import com.financas.app.domain.model.MoneyCents
import com.financas.app.domain.usecase.GoalProgress
import com.financas.app.domain.usecase.GoalProgressCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GoalWithProgress(
    val goal: GoalEntity,
    val progress: GoalProgress
)

data class AccountUiState(
    val userName: String = "",
    val profilePhotoUri: String? = null,
    val defaultCurrency: String = "EUR (€)",
    val monthlyCycleDay: Int = 1,
    val themeMode: String = "SYSTEM",
    val appLanguage: String = "EN",
    val parentCategories: List<CategoryEntity> = emptyList(),
    val subcategoriesByParent: Map<String, List<CategoryEntity>> = emptyMap(),
    val contextTags: List<String> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val budgetedCategoryIds: Set<String> = emptySet(),
    val goalItems: List<GoalWithProgress> = emptyList(),
    val isBiometricsEnabled: Boolean = false,
    val message: String? = null,
    val isLoading: Boolean = false,
    val totalTransactionsCount: Int = 0
)

class AccountViewModel(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val goalDao: GoalDao,
    private val biometricLockGate: BiometricLockGate,
    private val contextTagsStore: ContextTagsStore,
    private val backupSerializer: BackupSerializer = BackupSerializer(),
    private val goalProgressCalculator: GoalProgressCalculator = GoalProgressCalculator(),
    private val pendingTransactionDao: PendingTransactionDao? = null,
    private val investmentAssetDao: InvestmentAssetDao? = null,
    private val userProfileStore: UserProfileStore? = null,
    private val appUpdateManager: com.financas.app.data.updater.AppUpdateManager? = null,
    private val settingsBackupManager: com.financas.app.data.repository.SettingsBackupManager? = null,
    private val todayYearMonth: () -> Int = {
        LocalDate.now().let { it.year * 100 + it.monthValue }
    }
) : ViewModel() {

    val updateState: StateFlow<com.financas.app.data.updater.UpdateUiState> =
        appUpdateManager?.state ?: MutableStateFlow(com.financas.app.data.updater.UpdateUiState.Idle)

    private val _uiState = MutableStateFlow(
        AccountUiState(
            userName = userProfileStore?.getUserName() ?: "",
            profilePhotoUri = userProfileStore?.getProfilePhotoUri(),
            defaultCurrency = userProfileStore?.getDefaultCurrency() ?: "EUR (€)",
            monthlyCycleDay = userProfileStore?.getMonthlyCycleDay() ?: 1,
            themeMode = userProfileStore?.getThemeMode() ?: "SYSTEM",
            appLanguage = userProfileStore?.getAppLanguage() ?: "EN",
            isBiometricsEnabled = biometricLockGate.isBiometricsEnabled()
        )
    )
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private val customContextTags = MutableStateFlow(contextTagsStore.getCustomTags())

    init {
        userProfileStore?.let { store ->
            viewModelScope.launch {
                store.userNameFlow.collect { name ->
                    _uiState.update { it.copy(userName = name) }
                }
            }
            viewModelScope.launch {
                store.themeModeFlow.collect { mode ->
                    _uiState.update { it.copy(themeMode = mode) }
                }
            }
            viewModelScope.launch {
                store.appLanguageFlow.collect { lang ->
                    _uiState.update { it.copy(appLanguage = lang) }
                }
            }
        }
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                categoryDao.getAll(),
                customContextTags,
                budgetDao.getAllActive(),
                goalDao.getAll()
            ) { categories, customTags, budgets, goals ->
                val parents = categories.filter { it.parentId == null }
                val subcategories = categories.filter { it.parentId != null }.groupBy { it.parentId!! }
                val defaultTags = DefaultSeed.CONTEXTS.map { it.tag }
                val allTags = (defaultTags + customTags).distinct()

                BudgetGoalsSnapshot(parents, subcategories, allTags, budgets, goals)
            }.collect { snapshot ->
                _uiState.update { current ->
                    current.copy(
                        parentCategories = snapshot.parents,
                        subcategoriesByParent = snapshot.subcategories,
                        contextTags = snapshot.tags,
                        budgets = snapshot.budgets,
                        budgetedCategoryIds = snapshot.budgets.map { it.categoryId }.toSet(),
                        isBiometricsEnabled = biometricLockGate.isBiometricsEnabled()
                    )
                }
                recomputeGoalProgress(snapshot.goals)
                try {
                    val count = transactionDao.count()
                    _uiState.update { it.copy(totalTransactionsCount = count) }
                } catch (_: Exception) {}
            }
        }
    }

    private data class BudgetGoalsSnapshot(
        val parents: List<CategoryEntity>,
        val subcategories: Map<String, List<CategoryEntity>>,
        val tags: List<String>,
        val budgets: List<BudgetEntity>,
        val goals: List<GoalEntity>
    )

    private suspend fun recomputeGoalProgress(goals: List<GoalEntity>) {
        if (goals.isEmpty()) {
            _uiState.update { it.copy(goalItems = emptyList()) }
            return
        }

        val now = todayYearMonth()
        val minStart = goals.minOf { it.startYearMonth }
        val rangeTxs = if (minStart <= now) {
            transactionDao.getByYearMonthRangeSync(minStart, now)
        } else {
            emptyList()
        }

        val items = goals.map { goal ->
            val goalTxs = rangeTxs.filter { it.yearMonth in goal.startYearMonth..now }
            GoalWithProgress(goal, goalProgressCalculator.calculate(goal, goalTxs, now))
        }
        _uiState.update { it.copy(goalItems = items) }
    }

    fun toggleBiometrics(enabled: Boolean) {
        biometricLockGate.setBiometricsEnabled(enabled)
        _uiState.update { it.copy(isBiometricsEnabled = enabled) }
    }

    fun addSubcategory(
        parentId: String,
        name: String,
        icon: String = "category",
        isFixed: Boolean = false
    ) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            val parent = _uiState.value.parentCategories.find { it.id == parentId } ?: return@launch
            val existingSubs = _uiState.value.subcategoriesByParent[parentId] ?: emptyList()
            val safeName = trimmed.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
            val id = "${parentId}_${safeName}_${System.currentTimeMillis() % 10000}"
            val nextSortOrder = (existingSubs.maxOfOrNull { it.sortOrder } ?: parent.sortOrder) + 1

            val newCategory = CategoryEntity(
                id = id,
                parentId = parentId,
                name = trimmed,
                icon = icon,
                colorHex = parent.colorHex,
                type = parent.type,
                isDefaultFixed = isFixed,
                sortOrder = nextSortOrder
            )

            categoryDao.insert(newCategory)
            _uiState.update { it.copy(message = "Subcategoria \"$trimmed\" criada com sucesso.") }
        }
    }

    fun deleteSubcategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryDao.delete(category)
            _uiState.update { it.copy(message = "Subcategoria \"${category.name}\" eliminada.") }
        }
    }

    fun addContextTag(tag: String) {
        val clean = tag.trim().uppercase().replace(" ", "_")
        if (clean.isBlank()) return
        customContextTags.update { it + clean }
        contextTagsStore.addTag(clean)
        _uiState.update { it.copy(message = "Contexto \"$clean\" adicionado.") }
    }

    fun addBudget(categoryId: String, limitCents: Long) {
        if (categoryId.isBlank() || limitCents <= 0L) return

        viewModelScope.launch {
            val categoryName = resolveCategoryName(categoryId) ?: categoryId
            budgetDao.insert(
                BudgetEntity(
                    id = budgetIdFor(categoryId),
                    categoryId = categoryId,
                    limitCents = limitCents,
                    isActive = true
                )
            )
            _uiState.update {
                it.copy(message = "Orçamento para \"$categoryName\" definido: ${MoneyCents.formatCents(limitCents)}.")
            }
        }
    }

    fun updateBudgetLimit(budget: BudgetEntity, newLimitCents: Long) {
        if (newLimitCents <= 0L) return

        viewModelScope.launch {
            val categoryName = resolveCategoryName(budget.categoryId) ?: budget.categoryId
            budgetDao.insert(budget.copy(limitCents = newLimitCents))
            _uiState.update {
                it.copy(message = "Orçamento para \"$categoryName\" definido: ${MoneyCents.formatCents(newLimitCents)}.")
            }
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            budgetDao.deleteById(budget.id)
            _uiState.update { it.copy(message = "Orçamento eliminado.") }
        }
    }

    fun createGoal(name: String, targetCents: Long, deadlineYearMonth: Int? = null) {
        val trimmed = name.trim()
        if (trimmed.isBlank() || targetCents <= 0L) return

        val startYearMonth = todayYearMonth()
        if (deadlineYearMonth != null && deadlineYearMonth < startYearMonth) return

        viewModelScope.launch {
            val goal = GoalEntity(
                id = GoalEntity.deterministicId(trimmed, startYearMonth),
                name = trimmed,
                targetCents = targetCents,
                startYearMonth = startYearMonth,
                deadlineYearMonth = deadlineYearMonth
            )
            goalDao.insert(goal)
            _uiState.update { it.copy(message = "Meta \"$trimmed\" criada.") }
        }
    }

    fun archiveGoal(goal: GoalEntity) {
        viewModelScope.launch {
            goalDao.insert(goal.copy(archived = true))
            _uiState.update { it.copy(message = "Meta \"${goal.name}\" arquivada.") }
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            goalDao.deleteById(goal.id)
            _uiState.update { it.copy(message = "Meta eliminada.") }
        }
    }

    private fun resolveCategoryName(categoryId: String): String? {
        val state = _uiState.value
        return state.parentCategories.find { it.id == categoryId }?.name
            ?: state.subcategoriesByParent.values.flatten().find { it.id == categoryId }?.name
    }

    private fun budgetIdFor(categoryId: String): String = "budget_$categoryId"

    suspend fun exportJson(): String {
        val categories = categoryDao.getAllSync()
        val transactions = transactionDao.getAllSync()
        val budgets = budgetDao.getAllSync()
        val goals = goalDao.getAllSync()
        val investments = investmentAssetDao?.getAllSync() ?: emptyList()
        return backupSerializer.exportToJson(
            categories, transactions, budgets, goals, investments,
            settings = settingsBackupManager?.snapshot()
        )
    }

    suspend fun exportCsv(): String {
        val categories = categoryDao.getAllSync().associateBy { it.id }
        val transactions = transactionDao.getAllSync()
        return backupSerializer.exportToCsv(transactions, categories)
    }

    suspend fun importJson(jsonString: String): Result<RestoreResult> {
        _uiState.update { it.copy(isLoading = true) }
        return try {
            val backupData = backupSerializer.importFromJson(jsonString)
            val result = backupSerializer.restoreBackup(backupData, categoryDao, transactionDao, budgetDao, goalDao, investmentAssetDao)
            backupData.settings?.let { settings ->
                settingsBackupManager?.apply(settings)
                _uiState.update {
                    it.copy(
                        userName = settings.userName,
                        defaultCurrency = settings.defaultCurrency,
                        monthlyCycleDay = settings.monthlyCycleDay,
                        themeMode = settings.themeMode,
                        appLanguage = settings.appLanguage,
                        isBiometricsEnabled = biometricLockGate.isBiometricsEnabled(),
                        profilePhotoUri = userProfileStore?.getProfilePhotoUri()
                    )
                }
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    message = "Backup restaurado: ${result.transactionsImported} novos movimentos, ${result.categoriesImported} categorias sincronizadas, ${result.budgetsImported} orçamentos e ${result.goalsImported} metas sincronizados."
                )
            }
            Result.success(result)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    message = "Erro ao restaurar backup: ${e.message}"
                )
            }
            Result.failure(e)
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun simulatePendingNotification(
        amountCents: Long = 1250L,
        merchant: String = "Continente Bom Dia",
        sourceApp: String = "MB WAY"
    ) {
        viewModelScope.launch {
            val pendingDao = pendingTransactionDao ?: return@launch
            val categories = categoryDao.getAllSync()
            val defaultCat = categories.firstOrNull { it.id == "sub_groceries" }
                ?: categories.firstOrNull { it.type == "EXPENSE" && it.parentId != null }
                ?: categories.firstOrNull()

            val mock = PendingTransactionEntity(
                amountCents = amountCents,
                rawMerchant = merchant,
                suggestedCategoryId = defaultCat?.id ?: "sub_groceries",
                suggestedContextTag = "CASA",
                suggestedPaymentMethod = if (sourceApp == "MB WAY") "MBWAY" else "CARD",
                isFixed = false,
                sourceApp = sourceApp,
                detectedEpochDay = LocalDate.now().toEpochDay(),
                receivedTimestamp = System.currentTimeMillis()
            )
            pendingDao.insert(mock)
            _uiState.update {
                it.copy(message = "Notificação de teste criada ($merchant, ${MoneyCents.formatCents(amountCents)})! Vê na fila no Início.")
            }
        }
    }

    fun updateUserName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            userProfileStore?.setUserName(trimmed)
            _uiState.update { it.copy(userName = trimmed, message = "Perfil atualizado para \"$trimmed\".") }
        }
    }

    fun updateProfilePhoto(uri: String?) {
        userProfileStore?.setProfilePhotoUri(uri)
        _uiState.update {
            it.copy(
                profilePhotoUri = uri,
                message = if (uri != null) "Foto de perfil atualizada!" else "Foto de perfil removida."
            )
        }
    }

    fun updateDefaultCurrency(currency: String) {
        userProfileStore?.setDefaultCurrency(currency)
        _uiState.update { it.copy(defaultCurrency = currency, message = "Moeda predefinida alterada para $currency.") }
    }

    fun updateMonthlyCycleDay(day: Int) {
        if (day in 1..28) {
            userProfileStore?.setMonthlyCycleDay(day)
            _uiState.update { it.copy(monthlyCycleDay = day, message = "Início do ciclo mensal alterado para o dia $day.") }
        }
    }

    fun updateThemeMode(mode: String) {
        userProfileStore?.setThemeMode(mode)
        val msg = when (mode) {
            "DARK" -> "Modo Noturno ativado."
            "LIGHT" -> "Modo Diurno ativado."
            else -> "Modo automático do sistema ativado."
        }
        _uiState.update { it.copy(themeMode = mode, message = msg) }
    }

    fun updateAppLanguage(lang: String) {
        val uppercase = lang.uppercase()
        userProfileStore?.setAppLanguage(uppercase)
        val msg = if (uppercase == "EN") "Language set to English." else "Idioma alterado para Português."
        _uiState.update { it.copy(appLanguage = uppercase, message = msg) }
    }

    fun checkForUpdates(currentVersion: String) {
        viewModelScope.launch {
            appUpdateManager?.checkForUpdates(currentVersion, notifyIfAvailable = false)
        }
    }

    fun downloadUpdate(updateInfo: com.financas.app.domain.model.AppUpdateInfo) {
        viewModelScope.launch {
            appUpdateManager?.downloadUpdate(updateInfo)
        }
    }

    fun installApk(file: java.io.File) {
        appUpdateManager?.installApk(file)
    }

    fun resetUpdateState() {
        appUpdateManager?.resetState()
    }

    fun resetDemoData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val assetDao = investmentAssetDao ?: return@launch
            com.financas.app.data.seed.DemoDataSeeder.seedDemoData(
                categoryDao = categoryDao,
                transactionDao = transactionDao,
                budgetDao = budgetDao,
                goalDao = goalDao,
                investmentAssetDao = assetDao,
                userProfileStore = userProfileStore,
                pendingTransactionDao = pendingTransactionDao,
                clearExisting = true
            )
            _uiState.update { it.copy(isLoading = false, message = "Dados de demonstração repostos com sucesso.") }
        }
    }

    companion object {
        fun provideFactory(
            categoryDao: CategoryDao,
            transactionDao: TransactionDao,
            budgetDao: BudgetDao,
            goalDao: GoalDao,
            biometricLockGate: BiometricLockGate,
            backupSerializer: BackupSerializer = BackupSerializer(),
            contextTagsStore: ContextTagsStore,
            pendingTransactionDao: PendingTransactionDao? = null,
            investmentAssetDao: InvestmentAssetDao? = null,
            userProfileStore: UserProfileStore? = null,
            appUpdateManager: com.financas.app.data.updater.AppUpdateManager? = null,
            settingsBackupManager: com.financas.app.data.repository.SettingsBackupManager? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AccountViewModel(
                    categoryDao = categoryDao,
                    transactionDao = transactionDao,
                    budgetDao = budgetDao,
                    goalDao = goalDao,
                    biometricLockGate = biometricLockGate,
                    contextTagsStore = contextTagsStore,
                    backupSerializer = backupSerializer,
                    pendingTransactionDao = pendingTransactionDao,
                    investmentAssetDao = investmentAssetDao,
                    userProfileStore = userProfileStore,
                    appUpdateManager = appUpdateManager,
                    settingsBackupManager = settingsBackupManager
                ) as T
            }
        }
    }
}
