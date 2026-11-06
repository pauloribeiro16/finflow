package com.financas.app.ui.screens.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.financas.app.data.db.BudgetDao
import com.financas.app.data.db.CategoryDao
import com.financas.app.data.db.GoalDao
import com.financas.app.data.db.InvestmentAssetDao
import com.financas.app.data.repository.InvestmentTransferService
import com.financas.app.domain.usecase.InvestmentTransferRules
import com.financas.app.data.db.TransactionDao
import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.domain.model.InvestmentType
import com.financas.app.domain.model.PortfolioSummary
import com.financas.app.domain.model.XtbParsedReport
import com.financas.app.domain.model.XtbSyncResult
import com.financas.app.domain.usecase.BudgetProgress
import com.financas.app.domain.usecase.BudgetProgressCalculator
import com.financas.app.domain.usecase.GoalProgress
import com.financas.app.domain.usecase.GoalProgressCalculator
import com.financas.app.domain.usecase.InvestmentPortfolioCalculator
import com.financas.app.domain.usecase.XtbReportParser
import com.financas.app.domain.usecase.XtbSyncEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

enum class WealthTab(val title: String, val iconLabel: String) {
    INVESTMENTS("Investimentos", "🏛️ Investimentos"),
    GOALS("Metas", "🎯 Metas"),
    BUDGETS("Orçamentos", "📊 Orçamentos");

    fun localizedTitle(strings: com.financas.app.ui.i18n.AppStrings): String = when (this) {
        INVESTMENTS -> strings.tabInvestments
        GOALS -> strings.tabGoals
        BUDGETS -> strings.tabBudgets
    }

    fun localizedIconLabel(strings: com.financas.app.ui.i18n.AppStrings): String = when (this) {
        INVESTMENTS -> "🏛️ " + strings.tabInvestments
        GOALS -> "🎯 " + strings.tabGoals
        BUDGETS -> "📊 " + strings.tabBudgets
    }
}

data class BudgetsUiState(
    val selectedYearMonth: Int = 0,
    val monthTitle: String = "",
    val selectedTab: WealthTab = WealthTab.INVESTMENTS,
    // Budgets
    val totalLimitCents: Long = 0L,
    val totalSpentCents: Long = 0L,
    val totalRemainingCents: Long = 0L,
    val percentUsed: Double = 0.0,
    val daysRemainingInMonth: Int = 0,
    val dailyBudgetRemainingCents: Long = 0L,
    val categoryBudgets: List<BudgetProgress> = emptyList(),
    // Goals
    val goals: List<GoalProgress> = emptyList(),
    val allCategories: List<CategoryEntity> = emptyList(),
    val rolloverEnabled: Boolean = false,
    // Investments
    val investmentAssets: List<InvestmentAssetEntity> = emptyList(),
    val filteredInvestmentAssets: List<InvestmentAssetEntity> = emptyList(),
    val unallocatedHoldingAsset: InvestmentAssetEntity? = null,
    val unallocatedBalanceCents: Long = 0L,
    val safeSavingsTotalCents: Long = 0L,
    val longTermInvestedCents: Long = 0L,
    val longTermCurrentValueCents: Long = 0L,
    val longTermProfitLossCents: Long = 0L,
    val longTermProfitLossPct: Double = 0.0,
    val portfolioSummary: PortfolioSummary = PortfolioSummary(),
    val selectedAssetTypeFilter: InvestmentType? = null,
    val isLoading: Boolean = false
)

class BudgetsViewModel(
    private val budgetDao: BudgetDao,
    private val goalDao: GoalDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val investmentAssetDao: InvestmentAssetDao? = null,
    private val budgetProgressCalculator: BudgetProgressCalculator = BudgetProgressCalculator(),
    private val goalProgressCalculator: GoalProgressCalculator = GoalProgressCalculator(),
    private val investmentPortfolioCalculator: InvestmentPortfolioCalculator = InvestmentPortfolioCalculator(),
    private val xtbReportParser: XtbReportParser = XtbReportParser(),
    private val xtbSyncEngine: XtbSyncEngine? = investmentAssetDao?.let { XtbSyncEngine(it, transactionDao) }
) : ViewModel() {

    private val _selectedYearMonth = MutableStateFlow(currentYearMonthInt())
    private val _selectedTab = MutableStateFlow(WealthTab.INVESTMENTS)
    private val _selectedAssetTypeFilter = MutableStateFlow<InvestmentType?>(null)
    private val _uiState = MutableStateFlow(BudgetsUiState())
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    init {
        val assetsFlow = investmentAssetDao?.getAll() ?: flowOf(emptyList())

        val selectionFlow = combine(_selectedYearMonth, _selectedTab, _selectedAssetTypeFilter) { ym, tab, filter ->
            Triple(ym, tab, filter)
        }
        val dataFlow = combine(transactionDao.getAll(), assetsFlow) { txs, assets ->
            Pair(txs, assets)
        }

        viewModelScope.launch {
            combine(
                selectionFlow,
                budgetDao.getAll(),
                goalDao.getActive(),
                categoryDao.getAll(),
                dataFlow
            ) { selection, budgets, goals, categories, data ->
                val (ym, tab, filterType) = selection
                val (allTransactions, assets) = data
                buildUiState(ym, tab, filterType, budgets, goals, categories, allTransactions, assets)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun buildUiState(
        ym: Int,
        tab: WealthTab,
        filterType: InvestmentType?,
        budgets: List<BudgetEntity>,
        goals: List<GoalEntity>,
        categories: List<CategoryEntity>,
        allTransactions: List<TransactionEntity>,
        assets: List<InvestmentAssetEntity>
    ): BudgetsUiState {
        val monthTxs = allTransactions.filter { it.yearMonth == ym }
        val budgetReport = budgetProgressCalculator.calculate(budgets, monthTxs, categories)

        val goalProgressList = goals.map { goal ->
            goalProgressCalculator.calculate(goal, allTransactions, ym)
        }

        val year = ym / 100
        val month = ym % 100
        val ymObj = YearMonth.of(year, month)
        val now = LocalDate.now()
        val daysInMonth = ymObj.lengthOfMonth()
        val daysRemaining = if (now.year == year && now.monthValue == month) {
            (daysInMonth - now.dayOfMonth).coerceAtLeast(1)
        } else {
            daysInMonth
        }

        val dailyRemaining = if (daysRemaining > 0 && budgetReport.totalRemainingCents > 0) {
            budgetReport.totalRemainingCents / daysRemaining
        } else 0L

        val percentUsed = if (budgetReport.totalLimitCents > 0L) {
            (budgetReport.totalSpentCents.toDouble() / budgetReport.totalLimitCents.toDouble()) * 100.0
        } else 0.0

        val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("pt", "PT"))
        val formattedTitle = ymObj.atDay(1).format(monthFormatter).replaceFirstChar { it.uppercase() }

        val portfolio = investmentPortfolioCalculator.calculate(assets)
        val realAssets = assets.filter { !it.archived && !InvestmentTransferRules.isHoldingAsset(it) }
        val filteredAssets = if (filterType == null) {
            realAssets
        } else {
            realAssets.filter { InvestmentType.fromKey(it.type) == filterType }
        }

        val unallocatedHolding = assets.find { InvestmentTransferRules.isHoldingAsset(it) && it.currentValueCents > 0L }
        val unallocatedCents = unallocatedHolding?.currentValueCents ?: 0L

        val safeAssets = realAssets.filter {
            val t = InvestmentType.fromKey(it.type)
            t == InvestmentType.DEPOSITO_PRAZO || t == InvestmentType.CERTIFICADOS_AFORRO
        }
        val safeSavingsTotal = safeAssets.sumOf { it.currentValueCents }

        val longTermAssets = realAssets.filter {
            val t = InvestmentType.fromKey(it.type)
            t != InvestmentType.DEPOSITO_PRAZO && t != InvestmentType.CERTIFICADOS_AFORRO
        }
        val longTermInvested = longTermAssets.sumOf { it.investedCents }
        val longTermCurrent = longTermAssets.sumOf { it.currentValueCents }
        val longTermProfitLoss = longTermCurrent - longTermInvested
        val longTermProfitLossPct = if (longTermInvested > 0L) {
            (longTermProfitLoss.toDouble() / longTermInvested.toDouble()) * 100.0
        } else 0.0

        return BudgetsUiState(
            selectedYearMonth = ym,
            monthTitle = formattedTitle,
            selectedTab = tab,
            totalLimitCents = budgetReport.totalLimitCents,
            totalSpentCents = budgetReport.totalSpentCents,
            totalRemainingCents = budgetReport.totalRemainingCents,
            percentUsed = percentUsed,
            daysRemainingInMonth = daysRemaining,
            dailyBudgetRemainingCents = dailyRemaining,
            categoryBudgets = budgetReport.items,
            goals = goalProgressList,
            allCategories = categories.filter { it.parentId == null },
            rolloverEnabled = false,
            investmentAssets = assets,
            filteredInvestmentAssets = filteredAssets,
            unallocatedHoldingAsset = unallocatedHolding,
            unallocatedBalanceCents = unallocatedCents,
            safeSavingsTotalCents = safeSavingsTotal,
            longTermInvestedCents = longTermInvested,
            longTermCurrentValueCents = longTermCurrent,
            longTermProfitLossCents = longTermProfitLoss,
            longTermProfitLossPct = longTermProfitLossPct,
            portfolioSummary = portfolio,
            selectedAssetTypeFilter = filterType,
            isLoading = false
        )
    }

    fun selectTab(tab: WealthTab) {
        _selectedTab.value = tab
    }

    fun selectAssetTypeFilter(type: InvestmentType?) {
        _selectedAssetTypeFilter.value = type
    }

    fun selectPreviousMonth() {
        val ym = _selectedYearMonth.value
        val obj = YearMonth.of(ym / 100, ym % 100).minusMonths(1)
        _selectedYearMonth.value = obj.year * 100 + obj.monthValue
    }

    fun selectNextMonth() {
        val ym = _selectedYearMonth.value
        val obj = YearMonth.of(ym / 100, ym % 100).plusMonths(1)
        _selectedYearMonth.value = obj.year * 100 + obj.monthValue
    }

    fun setYearMonth(ym: Int) {
        _selectedYearMonth.value = ym
    }

    fun saveBudget(categoryId: String, limitCents: Long) {
        viewModelScope.launch {
            val budget = BudgetEntity(
                id = "budget_$categoryId",
                categoryId = categoryId,
                limitCents = limitCents,
                isActive = true
            )
            budgetDao.insert(budget)
        }
    }

    fun saveGoal(name: String, targetCents: Long, deadlineYearMonth: Int?) {
        viewModelScope.launch {
            val startYm = _selectedYearMonth.value
            val goal = GoalEntity(
                id = GoalEntity.deterministicId(name, startYm),
                name = name,
                targetCents = targetCents,
                startYearMonth = startYm,
                deadlineYearMonth = deadlineYearMonth,
                archived = false
            )
            goalDao.insert(goal)
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            budgetDao.deleteById(budgetId)
        }
    }

    fun toggleRollover() {
        _uiState.value = _uiState.value.copy(rolloverEnabled = !_uiState.value.rolloverEnabled)
    }

    fun saveInvestmentAsset(
        id: String?,
        name: String,
        type: String,
        institution: String,
        investedCents: Long,
        currentValueCents: Long,
        monthlyContributionCents: Long = 0L,
        targetCents: Long? = null,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val asset = InvestmentAssetEntity(
                id = id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                name = name.trim(),
                type = type,
                institution = institution.trim(),
                investedCents = investedCents,
                currentValueCents = currentValueCents,
                monthlyContributionCents = monthlyContributionCents,
                targetCents = targetCents,
                notes = notes.trim(),
                lastUpdatedEpochDay = LocalDate.now().toEpochDay(),
                archived = false
            )
            investmentAssetDao?.insert(asset)
        }
    }

    fun addMoneyToAsset(assetId: String, amountCents: Long) {
        val dao = investmentAssetDao ?: return
        viewModelScope.launch {
            InvestmentTransferService(transactionDao, dao).addContribution(assetId, amountCents)
        }
    }

    fun allocateAsset(fromAssetId: String, toAssetId: String, amountCents: Long) {
        val dao = investmentAssetDao ?: return
        viewModelScope.launch {
            InvestmentTransferService(transactionDao, dao).allocate(fromAssetId, toAssetId, amountCents)
        }
    }

    fun updateAssetValue(id: String, newValueCents: Long) {
        viewModelScope.launch {
            investmentAssetDao?.updateCurrentValue(
                id = id,
                newValueCents = newValueCents,
                epochDay = LocalDate.now().toEpochDay()
            )
        }
    }

    fun deleteInvestmentAsset(id: String) {
        viewModelScope.launch {
            investmentAssetDao?.deleteById(id)
        }
    }

    fun parseXtbReport(csvContent: String): XtbParsedReport {
        return xtbReportParser.parse(csvContent)
    }

    fun importXtbReport(
        report: XtbParsedReport,
        importCashOperations: Boolean,
        onComplete: (XtbSyncResult) -> Unit
    ) {
        val engine = xtbSyncEngine ?: return
        viewModelScope.launch {
            val result = engine.sync(report, importCashOperations)
            onComplete(result)
        }
    }

    companion object {
        fun currentYearMonthInt(): Int {
            val now = LocalDate.now()
            return now.year * 100 + now.monthValue
        }

        fun provideFactory(
            budgetDao: BudgetDao,
            goalDao: GoalDao,
            categoryDao: CategoryDao,
            transactionDao: TransactionDao,
            investmentAssetDao: InvestmentAssetDao? = null,
            investmentPortfolioCalculator: InvestmentPortfolioCalculator = InvestmentPortfolioCalculator()
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BudgetsViewModel(
                    budgetDao = budgetDao,
                    goalDao = goalDao,
                    categoryDao = categoryDao,
                    transactionDao = transactionDao,
                    investmentAssetDao = investmentAssetDao,
                    investmentPortfolioCalculator = investmentPortfolioCalculator
                ) as T
            }
        }
    }
}
