package com.financas.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.financas.app.data.db.BudgetDao
import com.financas.app.data.db.CategoryDao
import com.financas.app.data.db.GoalDao
import com.financas.app.data.db.InvestmentAssetDao
import com.financas.app.data.repository.InvestmentTransferService
import com.financas.app.domain.usecase.InvestmentPlatform
import com.financas.app.domain.usecase.InvestmentTransferRules
import com.financas.app.data.db.PendingTransactionDao
import com.financas.app.data.db.TransactionDao
import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.data.entity.PendingTransactionEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.prefs.UserProfileStore
import com.financas.app.data.seed.DefaultSeed
import com.financas.app.domain.model.MonthlySummary
import com.financas.app.domain.model.PortfolioSummary
import com.financas.app.domain.usecase.BudgetProgressCalculator
import com.financas.app.domain.usecase.BudgetProgressReport
import com.financas.app.domain.usecase.GoalProgress
import com.financas.app.domain.usecase.GoalProgressCalculator
import com.financas.app.domain.usecase.InvestmentPortfolioCalculator
import com.financas.app.domain.usecase.MonthProjection
import com.financas.app.domain.usecase.MonthProjectionCalculator
import com.financas.app.domain.usecase.MonthlyAnalyticsCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class GoalProgressUi(
    val goalId: String,
    val name: String,
    val targetCents: Long,
    val accumulatedSavingsCents: Long,
    val percentComplete: Double,
    val monthsRemaining: Int?,
    val averageMonthlySavingsCents: Long,
    val projectedMonthsToComplete: Long?,
    val onTrack: Boolean?,
    val projectionText: String,
    val trackBadgeText: String?
)

data class HomeUiState(
    val selectedYearMonth: Int,
    val monthTitle: String = "",
    val previousMonthExpenseCents: Long = 0L,
    val monthlySummary: MonthlySummary = MonthlySummary(
        yearMonth = 0,
        totalIncomeCents = 0L,
        baseIncomeCents = 0L,
        extraIncomeCents = 0L,
        totalExpenseCents = 0L,
        fixedExpenseCents = 0L,
        variableExpenseCents = 0L,
        netSavingsCents = 0L,
        savingsRatePercent = 0.0,
        expenseIncomeRatioPercent = 0.0,
        fixedExpenseIncomeRatioPercent = 0.0,
        variableExpenseIncomeRatioPercent = 0.0
    ),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val allMonthTransactions: List<TransactionEntity> = emptyList(),
    val categoryMap: Map<String, CategoryEntity> = emptyMap(),
    val budgetReport: BudgetProgressReport = BudgetProgressReport(),
    val monthProjection: MonthProjection? = null,
    val goalProgress: List<GoalProgressUi> = emptyList(),
    val pendingTransactions: List<PendingTransactionEntity> = emptyList(),
    val pendingCount: Int = 0,
    val refundsMap: Map<Long, Long> = emptyMap(),
    val profilePhotoUri: String? = null,
    val userName: String = "",
    val portfolioSummary: PortfolioSummary = PortfolioSummary(),
    val investmentAssets: List<InvestmentAssetEntity> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val goalDao: GoalDao,
    private val pendingTransactionDao: PendingTransactionDao? = null,
    private val investmentAssetDao: InvestmentAssetDao? = null,
    private val monthlyAnalyticsCalculator: MonthlyAnalyticsCalculator = MonthlyAnalyticsCalculator(),
    private val budgetProgressCalculator: BudgetProgressCalculator = BudgetProgressCalculator(),
    private val goalProgressCalculator: GoalProgressCalculator = GoalProgressCalculator(),
    private val monthProjectionCalculator: MonthProjectionCalculator = MonthProjectionCalculator(),
    private val investmentPortfolioCalculator: InvestmentPortfolioCalculator = InvestmentPortfolioCalculator(),
    private val userProfileStore: UserProfileStore? = null,
    initialYearMonth: Int = run {
        val now = LocalDate.now()
        now.year * 100 + now.monthValue
    }
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            selectedYearMonth = initialYearMonth,
            monthTitle = formatMonthTitle(initialYearMonth),
            profilePhotoUri = userProfileStore?.getProfilePhotoUri(),
            userName = userProfileStore?.getUserName() ?: ""
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var cachedCategories: List<CategoryEntity> = emptyList()
    private var cachedMonthTxs: List<TransactionEntity> = emptyList()
    private var cachedBudgets: List<BudgetEntity> = emptyList()
    private var cachedGoals: List<GoalEntity> = emptyList()
    private var monthDataJob: Job? = null

    init {
        loadCategories()
        loadBudgets()
        loadGoals()
        loadPending()
        loadInvestments()
        userProfileStore?.let { store ->
            viewModelScope.launch {
                store.profilePhotoFlow.collect { uri ->
                    _uiState.update { it.copy(profilePhotoUri = uri) }
                }
            }
            viewModelScope.launch {
                store.userNameFlow.collect { name ->
                    _uiState.update { it.copy(userName = name) }
                }
            }
        }
        loadMonthData(_uiState.value.selectedYearMonth)
    }

    private fun loadPending() {
        val dao = pendingTransactionDao ?: return
        viewModelScope.launch {
            dao.getAll().collect { pendingList ->
                _uiState.update { it.copy(pendingTransactions = pendingList, pendingCount = pendingList.size) }
            }
        }
    }

    private fun loadInvestments() {
        val dao = investmentAssetDao ?: return
        viewModelScope.launch {
            dao.getAll().collect { assets ->
                val summary = investmentPortfolioCalculator.calculate(assets)
                _uiState.update { it.copy(portfolioSummary = summary, investmentAssets = assets.filter { a -> !a.archived }) }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryDao.getAll().collect { categories ->
                cachedCategories = if (categories.isEmpty()) DefaultSeed.getDefaultCategories() else categories
                val map = cachedCategories.associateBy { it.id }
                _uiState.update { it.copy(categoryMap = map) }
                recalculateSummary()
                recomputeBudgets()
            }
        }
    }

    private fun loadBudgets() {
        viewModelScope.launch {
            budgetDao.getAllActive().collect { budgets ->
                cachedBudgets = budgets
                recomputeBudgets()
            }
        }
    }

    private fun loadGoals() {
        viewModelScope.launch {
            goalDao.getActive().collect { goals ->
                cachedGoals = goals
                recomputeGoals()
            }
        }
    }

    fun setYearMonth(yearMonth: Int) {
        _uiState.update {
            it.copy(
                selectedYearMonth = yearMonth,
                monthTitle = formatMonthTitle(yearMonth)
            )
        }
        loadMonthData(yearMonth)
        recomputeGoals()
    }

    fun selectPreviousMonth() {
        val current = _uiState.value.selectedYearMonth
        val ym = YearMonth.of(current / 100, current % 100).minusMonths(1)
        setYearMonth(ym.year * 100 + ym.monthValue)
    }

    fun selectNextMonth() {
        val current = _uiState.value.selectedYearMonth
        val ym = YearMonth.of(current / 100, current % 100).plusMonths(1)
        setYearMonth(ym.year * 100 + ym.monthValue)
    }

    fun loadMonthData(yearMonth: Int) {
        monthDataJob?.cancel()
        monthDataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            transactionDao.getByMonth(yearMonth).collect { txs ->
                val recent = txs.take(5)
                val refundsMap = txs
                    .filter { it.type.equals("INCOME", ignoreCase = true) && it.linkedTransactionId != null }
                    .groupBy { it.linkedTransactionId!! }
                    .mapValues { (_, r) -> r.sumOf { it.amountCents } }
                val summary = monthlyAnalyticsCalculator.calculateMonthlySummary(
                    yearMonth = yearMonth,
                    transactions = txs,
                    categories = cachedCategories
                )
                val projection = if (yearMonth == currentRealYearMonth() || txs.isNotEmpty()) {
                    monthProjectionCalculator.calculate(txs, yearMonth, LocalDate.now().toEpochDay())
                } else {
                    null
                }

                val previousYm = YearMonth.of(yearMonth / 100, yearMonth % 100).minusMonths(1)
                val previousExpenseCents = transactionDao
                    .getByMonthSync(previousYm.year * 100 + previousYm.monthValue)
                    .filter { it.type.equals("EXPENSE", ignoreCase = true) }
                    .sumOf { it.amountCents }

                cachedMonthTxs = txs
                _uiState.update {
                    it.copy(
                        previousMonthExpenseCents = previousExpenseCents,
                        monthlySummary = summary,
                        recentTransactions = recent,
                        allMonthTransactions = txs,
                        refundsMap = refundsMap,
                        monthProjection = projection,
                        isLoading = false
                    )
                }
                recomputeBudgets()
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionDao.deleteById(id)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val service = transferService
            if (transaction.investmentAssetId != null && service != null) {
                service.deleteTransaction(transaction)
            } else {
                transactionDao.deleteById(transaction.id)
            }
        }
    }

    private val transferService: InvestmentTransferService? =
        investmentAssetDao?.let { InvestmentTransferService(transactionDao, it) }

    fun confirmPending(
        pending: PendingTransactionEntity,
        chosenCategoryId: String? = null,
        chosenContextTag: String? = null,
        investmentAssetId: String? = null
    ) {
        viewModelScope.launch {
            val epochDay = pending.detectedEpochDay
            val localDate = LocalDate.ofEpochDay(epochDay)
            val txYearMonth = localDate.year * 100 + localDate.monthValue

            val newTx = TransactionEntity(
                type = pending.suggestedType,
                amountCents = pending.amountCents,
                yearMonth = txYearMonth,
                epochDay = epochDay,
                categoryId = chosenCategoryId ?: pending.suggestedCategoryId,
                contextTag = chosenContextTag ?: pending.suggestedContextTag,
                merchant = pending.rawMerchant,
                paymentMethod = pending.suggestedPaymentMethod,
                isFixed = pending.isFixed
            )
            val service = transferService
            if (investmentAssetId != null && service != null) {
                service.recordTransfer(newTx, investmentAssetId)
            } else {
                transactionDao.insert(newTx)
            }
            pendingTransactionDao?.deleteById(pending.id)
            if (txYearMonth == _uiState.value.selectedYearMonth) {
                loadMonthData(txYearMonth)
            }
        }
    }

    fun updatePendingItem(pending: PendingTransactionEntity) {
        viewModelScope.launch {
            pendingTransactionDao?.update(pending)
        }
    }

    fun discardPending(pendingId: Long) {
        viewModelScope.launch {
            pendingTransactionDao?.deleteById(pendingId)
        }
    }

    fun confirmAllPending() {
        viewModelScope.launch {
            val items = _uiState.value.pendingTransactions
            for (pending in items) {
                val epochDay = pending.detectedEpochDay
                val localDate = LocalDate.ofEpochDay(epochDay)
                val txYearMonth = localDate.year * 100 + localDate.monthValue
                val newTx = TransactionEntity(
                    type = pending.suggestedType,
                    amountCents = pending.amountCents,
                    yearMonth = txYearMonth,
                    epochDay = epochDay,
                    categoryId = pending.suggestedCategoryId,
                    contextTag = pending.suggestedContextTag,
                    merchant = pending.rawMerchant,
                    paymentMethod = pending.suggestedPaymentMethod,
                    isFixed = pending.isFixed
                )
                val platform = InvestmentTransferRules.detectPlatform(pending.rawMerchant)
                val service = transferService
                if (platform == InvestmentPlatform.OPTIMIZE) {
                    continue
                } else if (platform == InvestmentPlatform.XTB && service != null) {
                    service.recordTransfer(newTx, InvestmentTransferRules.XTB_HOLDING_ASSET_ID)
                } else {
                    transactionDao.insert(newTx)
                }
                pendingTransactionDao?.deleteById(pending.id)
            }
            loadMonthData(_uiState.value.selectedYearMonth)
        }
    }

    fun simulatePendingNotification(
        amountCents: Long = 1250L,
        merchant: String = "Continente Bom Dia",
        sourceApp: String = "MB WAY"
    ) {
        viewModelScope.launch {
            val defaultCat = cachedCategories.firstOrNull { it.id == "sub_groceries" }
                ?: cachedCategories.firstOrNull { it.type == "EXPENSE" && it.parentId != null }
                ?: cachedCategories.firstOrNull()

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
            pendingTransactionDao?.insert(mock)
        }
    }

    private fun recalculateSummary() {
        val summary = monthlyAnalyticsCalculator.calculateMonthlySummary(
            yearMonth = _uiState.value.selectedYearMonth,
            transactions = cachedMonthTxs,
            categories = cachedCategories
        )
        _uiState.update { it.copy(monthlySummary = summary) }
    }

    private fun recomputeBudgets() {
        val report = budgetProgressCalculator.calculate(cachedBudgets, cachedMonthTxs, cachedCategories)
        _uiState.update { it.copy(budgetReport = report) }
    }

    private fun recomputeGoals() {
        viewModelScope.launch {
            val selected = _uiState.value.selectedYearMonth
            val goals = cachedGoals
            if (goals.isEmpty()) {
                _uiState.update { it.copy(goalProgress = emptyList()) }
                return@launch
            }

            val minStart = goals.minOf { it.startYearMonth }
            val rangeTxs = if (minStart <= selected) {
                transactionDao.getByYearMonthRangeSync(minStart, selected)
            } else {
                emptyList()
            }

            val progress = goals.map { goal ->
                val goalTxs = rangeTxs.filter { it.yearMonth in goal.startYearMonth..selected }
                goalProgressCalculator.calculate(goal, goalTxs, selected).toUi()
            }
            _uiState.update { it.copy(goalProgress = progress) }
        }
    }

    private fun GoalProgress.toUi(): GoalProgressUi = GoalProgressUi(
        goalId = goalId,
        name = name,
        targetCents = targetCents,
        accumulatedSavingsCents = accumulatedSavingsCents,
        percentComplete = percentComplete,
        monthsRemaining = monthsRemaining,
        averageMonthlySavingsCents = averageMonthlySavingsCents,
        projectedMonthsToComplete = projectedMonthsToComplete,
        onTrack = onTrack,
        projectionText = formatProjectionText(projectedMonthsToComplete),
        trackBadgeText = formatTrackBadgeText(onTrack)
    )

    private fun currentRealYearMonth(): Int {
        val now = LocalDate.now()
        return now.year * 100 + now.monthValue
    }

    companion object {
        fun formatProjectionText(projectedMonthsToComplete: Long?): String = when {
            projectedMonthsToComplete == null -> "Sem dados para projetar"
            projectedMonthsToComplete <= 0L -> "Previsão: meta atingida"
            projectedMonthsToComplete == 1L -> "Previsão: ~1 mês"
            else -> "Previsão: ~$projectedMonthsToComplete meses"
        }

        fun formatTrackBadgeText(onTrack: Boolean?): String? = when (onTrack) {
            true -> "No prazo"
            false -> "Atrasado"
            null -> null
        }

        fun formatMonthTitle(yearMonth: Int): String {
            val year = yearMonth / 100
            val month = yearMonth % 100
            val ym = YearMonth.of(year, month)
            val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("pt", "PT"))
            val formatted = ym.format(formatter)
            return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "PT")) else it.toString() }
        }

        fun provideFactory(
            transactionDao: TransactionDao,
            categoryDao: CategoryDao,
            budgetDao: BudgetDao,
            goalDao: GoalDao,
            monthlyAnalyticsCalculator: MonthlyAnalyticsCalculator,
            pendingTransactionDao: PendingTransactionDao? = null,
            investmentAssetDao: InvestmentAssetDao? = null,
            investmentPortfolioCalculator: InvestmentPortfolioCalculator = InvestmentPortfolioCalculator(),
            userProfileStore: UserProfileStore? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    transactionDao = transactionDao,
                    categoryDao = categoryDao,
                    budgetDao = budgetDao,
                    goalDao = goalDao,
                    pendingTransactionDao = pendingTransactionDao,
                    investmentAssetDao = investmentAssetDao,
                    monthlyAnalyticsCalculator = monthlyAnalyticsCalculator,
                    investmentPortfolioCalculator = investmentPortfolioCalculator,
                    userProfileStore = userProfileStore
                ) as T
            }
        }
    }
}
