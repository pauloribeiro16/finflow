package com.financas.app.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.financas.app.data.db.BudgetDao
import com.financas.app.data.db.CategoryDao
import com.financas.app.data.db.GoalDao
import com.financas.app.data.db.TransactionDao
import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.seed.DefaultSeed
import com.financas.app.domain.model.DetectedSubscription
import com.financas.app.domain.model.MoneyCents
import com.financas.app.domain.model.SubscriptionReport
import com.financas.app.domain.usecase.BudgetProgressCalculator
import com.financas.app.domain.usecase.BudgetProgressReport
import com.financas.app.domain.usecase.CalendarAggregator
import com.financas.app.domain.usecase.CategoryDelta
import com.financas.app.domain.usecase.ComparisonCalculator
import com.financas.app.domain.usecase.GoalProgress
import com.financas.app.domain.usecase.GoalProgressCalculator
import com.financas.app.domain.usecase.MonthlyAnalyticsCalculator
import com.financas.app.domain.usecase.MonthComparison
import com.financas.app.domain.usecase.MonthlyReportGenerator
import com.financas.app.domain.usecase.SavingsRatePoint
import com.financas.app.domain.usecase.SubscriptionDetector
import com.financas.app.domain.usecase.TrendCalculator
import com.financas.app.ui.components.LinePoint
import com.financas.app.ui.components.YearlyBarData
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class DisplayMode {
    EURO,
    INCOME_PERCENTAGE
}

enum class AnalyticsLens(val label: String) {
    ANUAL("Visão Anual"),
    NO_QUE("No Quê"),
    ONDE("Onde"),
    COMO("Como"),
    EVOLUCAO("Evolução"),
    COMPARAR("Comparar"),
    CALENDARIO("Calendário");

    fun localizedLabel(strings: com.financas.app.ui.i18n.AppStrings): String = when (this) {
        ANUAL -> strings.lensAnnual
        NO_QUE -> strings.lensCategory
        ONDE -> strings.lensContext
        COMO -> strings.lensPayment
        EVOLUCAO -> strings.lensEvolution
        COMPARAR -> strings.lensCompare
        CALENDARIO -> strings.lensCalendar
    }
}

data class AnnualMonthSummary(
    val yearMonth: Int,
    val monthNumber: Int,
    val monthLabel: String,
    val incomeCents: Long,
    val expenseCents: Long,
    val netSavingsCents: Long,
    val savingsRatePercent: Double,
    val hasActivity: Boolean,
    val formattedIncome: String,
    val formattedExpense: String,
    val formattedSavings: String,
    val transactionCount: Int
)

data class AnnualSubcategoryUi(
    val id: String,
    val name: String,
    val icon: String,
    val colorHex: Long,
    val amountCents: Long,
    val formattedValue: String,
    val percentageOfParent: Double,
    val transactionCount: Int,
    val transactions: List<TransactionEntity>
)

data class AnnualCategoryUi(
    val id: String,
    val name: String,
    val icon: String,
    val colorHex: Long,
    val amountCents: Long,
    val formattedValue: String,
    val percentageOfExpense: Double,
    val percentageOfIncome: Double,
    val transactionCount: Int,
    val subcategories: List<AnnualSubcategoryUi>,
    val transactions: List<TransactionEntity>
)

data class AnnualAnalysisUi(
    val selectedYear: Int,
    val availableYears: List<Int> = listOf(2026, 2025, 2024),
    val totalIncomeCents: Long = 0L,
    val totalExpenseCents: Long = 0L,
    val netSavingsCents: Long = 0L,
    val savingsRatePercent: Double = 0.0,
    val monthlyAverageExpenseCents: Long = 0L,
    val activeMonthsCount: Int = 0,
    val formattedTotalIncome: String = "0,00 €",
    val formattedTotalExpense: String = "0,00 €",
    val formattedNetSavings: String = "0,00 €",
    val formattedSavingsRate: String = "0,0%",
    val formattedMonthlyAverageExpense: String = "0,00 €",
    val months: List<AnnualMonthSummary> = emptyList(),
    val categories: List<AnnualCategoryUi> = emptyList(),
    val selectedMonthDetail: AnnualMonthSummary? = null,
    val selectedCategoryId: String? = null,
    val expandedSubcategoryId: String? = null
)

enum class ComparisonBaseline(val label: String) {
    PREVIOUS_MONTH("Mês Anterior"),
    MONTHLY_AVERAGE("Média Mensal"),
    SAME_MONTH_LAST_YEAR("Ano Anterior")
}

data class SubcategoryItem(
    val id: String,
    val name: String,
    val icon: String,
    val colorHex: Long,
    val amountCents: Long,
    val formattedValue: String,
    val percentageOfParent: Double,
    val percentageOfIncome: Double,
    val percentageOfExpense: Double,
    val transactionCount: Int,
    val transactions: List<TransactionEntity> = emptyList()
)

data class ParentCategoryItem(
    val id: String,
    val name: String,
    val icon: String,
    val colorHex: Long,
    val amountCents: Long,
    val formattedValue: String,
    val percentageOfExpense: Double,
    val percentageOfIncome: Double,
    val transactionCount: Int,
    val deviationVsAveragePercent: Double?,
    val subcategories: List<SubcategoryItem>
)

data class ContextItem(
    val tag: String,
    val label: String,
    val amountCents: Long,
    val formattedValue: String,
    val percentageOfExpense: Double,
    val percentageOfIncome: Double,
    val transactionCount: Int
)

data class MerchantItem(
    val merchant: String,
    val amountCents: Long,
    val formattedValue: String,
    val percentageOfExpense: Double,
    val percentageOfIncome: Double,
    val transactionCount: Int,
    val averagePerVisitCents: Long,
    val formattedAverageTicket: String
)

data class MethodItem(
    val code: String,
    val label: String,
    val amountCents: Long,
    val formattedValue: String,
    val percentageOfExpense: Double,
    val percentageOfIncome: Double,
    val transactionCount: Int
)

data class RigidityStructure(
    val fixedExpenseCents: Long,
    val variableExpenseCents: Long,
    val netSavingsCents: Long,
    val formattedFixed: String,
    val formattedVariable: String,
    val formattedSavings: String,
    val fixedPercentOfIncome: Double,
    val variablePercentOfIncome: Double,
    val savingsPercentOfIncome: Double
)

data class EvolutionMonthItem(
    val yearMonth: Int,
    val label: String,
    val incomeCents: Long,
    val expenseCents: Long,
    val netSavingsCents: Long,
    val savingsRatePercent: Double,
    val hasActivity: Boolean,
    val formattedIncome: String,
    val formattedExpense: String,
    val formattedSavings: String,
    val deviationVsAveragePercent: Double?
)

data class TrendChipUi(
    val categoryId: String,
    val name: String,
    val colorHex: Long,
    val selected: Boolean
)

data class CategoryDeltaUi(
    val categoryId: String,
    val categoryName: String,
    val colorHex: Long,
    val current: String,
    val baseline: String,
    val delta: String,
    val deltaPercent: String?
)

data class MonthComparisonUi(
    val baselineLabel: String,
    val currentExpense: String,
    val baselineExpense: String,
    val expenseDelta: String,
    val expenseDeltaPercent: String?,
    val expenseDeltaPositive: Boolean,
    val currentIncome: String,
    val baselineIncome: String,
    val incomeDelta: String,
    val incomeDeltaPercent: String?,
    val incomeDeltaPositive: Boolean,
    val currentSavings: String,
    val baselineSavings: String,
    val savingsDelta: String,
    val savingsDeltaPercent: String?,
    val savingsDeltaPositive: Boolean,
    val increases: List<CategoryDeltaUi>,
    val decreases: List<CategoryDeltaUi>
)

data class CategoryTrendUi(
    val categoryName: String,
    val colorHex: Long,
    val points: List<LinePoint>,
    val averageExpenseCents: Long,
    val formattedAverage: String,
    val selectedMonthDetail: String?,
    val selectedMonthDeviation: Double?
)

data class SavingsRateTrendUi(
    val points: List<LinePoint>,
    val formattedAverage: String,
    val averageRatePercent: Double
)

data class YearTotalsUi(
    val year: Int,
    val formattedIncome: String,
    val formattedExpense: String,
    val formattedSavings: String,
    val formattedRate: String
)

data class CalendarDayItem(
    val epochDay: Long,
    val dayOfMonth: Int,
    val totalExpenseCents: Long,
    val formattedTotal: String,
    val transactionCount: Int,
    val isToday: Boolean
)

data class CalendarDaySelection(
    val epochDay: Long,
    val label: String,
    val totalExpenseCents: Long,
    val formattedTotal: String,
    val transactions: List<TransactionEntity>
)

data class QuarterlyHistoryItem(
    val yearMonth: Int,
    val label: String,
    val expenseCents: Long,
    val formattedExpense: String,
    val isCurrent: Boolean
)

data class QuarterlyHistoryUi(
    val items: List<QuarterlyHistoryItem> = emptyList(),
    val averageExpenseCents: Long = 0L,
    val formattedAverageExpense: String = "0,00 €"
)

data class AnalyticsUiState(
    val selectedYearMonth: Int,
    val monthTitle: String = "",
    val displayMode: DisplayMode = DisplayMode.EURO,
    val activeLens: AnalyticsLens = AnalyticsLens.NO_QUE,
    val selectedParentCategoryId: String? = null,
    val selectedEvolutionMonth: Int? = null,
    val totalIncomeCents: Long = 0L,
    val totalExpenseCents: Long = 0L,
    val netSavingsCents: Long = 0L,
    val formattedTotalIncome: String = "0,00 €",
    val formattedTotalExpense: String = "0,00 €",
    val formattedNetSavings: String = "0,00 €",
    val savingsRatePercent: Double = 0.0,
    val historicalMonthlyAverageExpenseCents: Long = 0L,
    val formattedHistoricalMonthlyAverage: String = "0,00 €",
    val parentCategories: List<ParentCategoryItem> = emptyList(),
    val expandedSubcategories: List<SubcategoryItem> = emptyList(),
    val contexts: List<ContextItem> = emptyList(),
    val topMerchants: List<MerchantItem> = emptyList(),
    val rigidity: RigidityStructure = RigidityStructure(0L, 0L, 0L, "0,00 €", "0,00 €", "0,00 €", 0.0, 0.0, 0.0),
    val paymentMethods: List<MethodItem> = emptyList(),
    val evolutionMonths: List<EvolutionMonthItem> = emptyList(),
    val selectedEvolutionMonthItem: EvolutionMonthItem? = null,
    val comparisonBaseline: ComparisonBaseline = ComparisonBaseline.PREVIOUS_MONTH,
    val comparison: MonthComparisonUi? = null,
    val trendChips: List<TrendChipUi> = emptyList(),
    val categoryTrend: CategoryTrendUi? = null,
    val savingsRateTrend: SavingsRateTrendUi? = null,
    val yearComparison: List<YearlyBarData> = emptyList(),
    val yearTotals: List<YearTotalsUi> = emptyList(),
    val calendarDays: List<CalendarDayItem> = emptyList(),
    val selectedCalendarDay: CalendarDaySelection? = null,
    val monthlyReport: String = "",
    val categoryMap: Map<String, CategoryEntity> = emptyMap(),
    val subscriptionReport: SubscriptionReport = SubscriptionReport(),
    val annualAnalysis: AnnualAnalysisUi = AnnualAnalysisUi(selectedYear = 2026),
    val quarterlyHistory: QuarterlyHistoryUi = QuarterlyHistoryUi(),
    val isLoading: Boolean = false
)

class AnalyticsViewModel(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao? = null,
    private val goalDao: GoalDao? = null,
    private val monthlyAnalyticsCalculator: MonthlyAnalyticsCalculator = MonthlyAnalyticsCalculator(),
    private val comparisonCalculator: ComparisonCalculator = ComparisonCalculator(),
    private val trendCalculator: TrendCalculator = TrendCalculator(),
    private val calendarAggregator: CalendarAggregator = CalendarAggregator(),
    private val monthlyReportGenerator: MonthlyReportGenerator = MonthlyReportGenerator(),
    private val budgetProgressCalculator: BudgetProgressCalculator = BudgetProgressCalculator(),
    private val goalProgressCalculator: GoalProgressCalculator = GoalProgressCalculator(),
    private val subscriptionDetector: SubscriptionDetector = SubscriptionDetector(),
    private val todayEpochDayProvider: () -> Long = { LocalDate.now().toEpochDay() },
    initialYearMonth: Int = run {
        val now = LocalDate.now()
        now.year * 100 + now.monthValue
    }
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AnalyticsUiState(
            selectedYearMonth = initialYearMonth,
            monthTitle = formatMonthTitle(initialYearMonth),
            selectedEvolutionMonth = initialYearMonth,
            annualAnalysis = AnnualAnalysisUi(selectedYear = initialYearMonth / 100)
        )
    )
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private var cachedCategories: List<CategoryEntity> = emptyList()
    private var cachedMonthTransactions: List<TransactionEntity> = emptyList()
    private var cachedYearTransactions: List<TransactionEntity> = emptyList()
    private var cachedAllTransactions: List<TransactionEntity> = emptyList()
    private var cachedBudgets: List<BudgetEntity> = emptyList()
    private var cachedGoals: List<GoalEntity> = emptyList()
    private var evolutionCategorySelection: String? = null
    private var monthJob: Job? = null
    private var yearJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            categoryDao.getAll().collect { categories ->
                cachedCategories = if (categories.isEmpty()) DefaultSeed.getDefaultCategories() else categories
                recalculateAll()
            }
        }
        observeMonthTransactions(_uiState.value.selectedYearMonth)
        observeYearTransactions(_uiState.value.selectedYearMonth / 100)
        observeAllTransactions()
        observeBudgets()
        observeGoals()
    }

    private fun observeBudgets() {
        val dao = budgetDao ?: return
        viewModelScope.launch {
            dao.getAllActive().collect { budgets ->
                cachedBudgets = budgets
                recalculateAll()
            }
        }
    }

    private fun observeGoals() {
        val dao = goalDao ?: return
        viewModelScope.launch {
            dao.getAll().collect { goals ->
                cachedGoals = goals
                recalculateAll()
            }
        }
    }

    private fun observeMonthTransactions(yearMonth: Int) {
        monthJob?.cancel()
        monthJob = viewModelScope.launch {
            transactionDao.getByMonth(yearMonth).collect { txs ->
                cachedMonthTransactions = txs
                recalculateAll()
            }
        }
    }

    private fun observeYearTransactions(year: Int) {
        yearJob?.cancel()
        yearJob = viewModelScope.launch {
            val startYm = year * 100 + 1
            val endYm = year * 100 + 12
            transactionDao.getByYearMonthRange(startYm, endYm).collect { txs ->
                cachedYearTransactions = txs
                recalculateAll()
            }
        }
    }

    private fun observeAllTransactions() {
        viewModelScope.launch {
            transactionDao.getAll().collect { txs ->
                cachedAllTransactions = txs
                recalculateAll()
            }
        }
    }

    fun setDisplayMode(mode: DisplayMode) {
        if (_uiState.value.displayMode == mode) return
        _uiState.update { it.copy(displayMode = mode) }
        recalculateAll()
    }

    fun toggleDisplayMode() {
        val nextMode = if (_uiState.value.displayMode == DisplayMode.EURO) {
            DisplayMode.INCOME_PERCENTAGE
        } else {
            DisplayMode.EURO
        }
        setDisplayMode(nextMode)
    }

    fun setActiveLens(lens: AnalyticsLens) {
        _uiState.update { it.copy(activeLens = lens) }
    }

    fun setYearMonth(yearMonth: Int) {
        val oldYear = _uiState.value.selectedYearMonth / 100
        val newYear = yearMonth / 100

        _uiState.update {
            it.copy(
                selectedYearMonth = yearMonth,
                monthTitle = formatMonthTitle(yearMonth),
                selectedEvolutionMonth = yearMonth,
                selectedParentCategoryId = null,
                selectedCalendarDay = null
            )
        }

        observeMonthTransactions(yearMonth)
        if (oldYear != newYear) {
            observeYearTransactions(newYear)
        }
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

    fun selectParentCategory(categoryId: String?) {
        _uiState.update { current ->
            val newSelection = if (current.selectedParentCategoryId == categoryId) null else categoryId
            val selectedParent = current.parentCategories.find { it.id == newSelection }
            current.copy(
                selectedParentCategoryId = newSelection,
                expandedSubcategories = selectedParent?.subcategories ?: emptyList()
            )
        }
    }

    fun selectEvolutionMonth(yearMonth: Int) {
        _uiState.update { current ->
            val selectedItem = current.evolutionMonths.find { it.yearMonth == yearMonth }
            current.copy(
                selectedEvolutionMonth = yearMonth,
                selectedEvolutionMonthItem = selectedItem
            )
        }
    }

    fun selectAnnualYear(year: Int) {
        _uiState.update { current ->
            current.copy(
                annualAnalysis = current.annualAnalysis.copy(
                    selectedYear = year,
                    selectedCategoryId = null,
                    expandedSubcategoryId = null,
                    selectedMonthDetail = null
                )
            )
        }
        observeYearTransactions(year)
        recalculateAll()
    }

    fun selectAnnualPreviousYear() {
        val currentYear = _uiState.value.annualAnalysis.selectedYear
        selectAnnualYear(currentYear - 1)
    }

    fun selectAnnualNextYear() {
        val currentYear = _uiState.value.annualAnalysis.selectedYear
        selectAnnualYear(currentYear + 1)
    }

    fun selectAnnualMonth(yearMonth: Int) {
        _uiState.update { current ->
            val monthSummary = current.annualAnalysis.months.find { it.yearMonth == yearMonth }
            current.copy(
                annualAnalysis = current.annualAnalysis.copy(selectedMonthDetail = monthSummary)
            )
        }
    }

    fun selectAnnualCategory(categoryId: String?) {
        _uiState.update { current ->
            val newSelection = if (current.annualAnalysis.selectedCategoryId == categoryId) null else categoryId
            current.copy(
                annualAnalysis = current.annualAnalysis.copy(
                    selectedCategoryId = newSelection,
                    expandedSubcategoryId = null
                )
            )
        }
    }

    fun selectAnnualSubcategory(subcategoryId: String?) {
        _uiState.update { current ->
            val newSelection = if (current.annualAnalysis.expandedSubcategoryId == subcategoryId) null else subcategoryId
            current.copy(
                annualAnalysis = current.annualAnalysis.copy(expandedSubcategoryId = newSelection)
            )
        }
    }

    fun setComparisonBaseline(baseline: ComparisonBaseline) {
        if (_uiState.value.comparisonBaseline == baseline) return
        _uiState.update { it.copy(comparisonBaseline = baseline) }
        recalculateAll()
    }

    fun selectEvolutionCategory(categoryId: String?) {
        evolutionCategorySelection = categoryId
        recalculateAll()
    }

    fun selectCalendarDay(epochDay: Long) {
        val day = _uiState.value.calendarDays.find { it.epochDay == epochDay } ?: return
        val dayTransactions = cachedMonthTransactions
            .filter { it.epochDay == epochDay }
            .sortedWith(compareByDescending<TransactionEntity> { it.id })

        _uiState.update { current ->
            current.copy(
                selectedCalendarDay = CalendarDaySelection(
                    epochDay = epochDay,
                    label = formatDayLabel(epochDay),
                    totalExpenseCents = day.totalExpenseCents,
                    formattedTotal = day.formattedTotal,
                    transactions = dayTransactions
                )
            )
        }
    }

    /**
     * Gera o relatório do mês atualmente selecionado no momento da chamada.
     *
     * Usado pela partilha para garantir que o texto partilhado corresponde
     * sempre ao mês visível, mesmo que a emission do fluxo do mês ainda não
     * tenha propagating o cache.
     */
    fun buildMonthlyReport(): String {
        val selectedYm = _uiState.value.selectedYearMonth
        val categories = cachedCategories.ifEmpty { DefaultSeed.getDefaultCategories() }
        return monthlyReportGenerator.generate(
            yearMonth = selectedYm,
            transactions = transactionsForMonth(selectedYm),
            categories = categories,
            budgetProgress = buildBudgetReport(categories),
            goalProgress = buildGoalsProgress(selectedYm)
        )
    }

    private fun transactionsForMonth(yearMonth: Int): List<TransactionEntity> {
        val fromAll = cachedAllTransactions.filter { it.yearMonth == yearMonth }
        if (fromAll.isNotEmpty()) return fromAll
        return cachedMonthTransactions.filter { it.yearMonth == yearMonth }
    }

    private fun buildBudgetReport(categories: List<CategoryEntity>): BudgetProgressReport? {
        if (cachedBudgets.isEmpty()) return null
        return budgetProgressCalculator.calculate(
            cachedBudgets,
            transactionsForMonth(_uiState.value.selectedYearMonth),
            categories
        )
    }

    private fun buildGoalsProgress(selectedYm: Int): List<GoalProgress>? {
        if (cachedGoals.isEmpty()) return null
        return cachedGoals.map { goal ->
            val goalTransactions = cachedAllTransactions.filter { tx ->
                tx.yearMonth in goal.startYearMonth..selectedYm
            }
            goalProgressCalculator.calculate(goal, goalTransactions, selectedYm)
        }
    }

    private fun formatDayLabel(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        val formatted = date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("pt", "PT")))
        return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "PT")) else it.toString() }
    }

    private fun recalculateAll() {
        val displayMode = _uiState.value.displayMode
        val selectedYm = _uiState.value.selectedYearMonth
        val selectedParentId = _uiState.value.selectedParentCategoryId
        val selectedEvoYm = _uiState.value.selectedEvolutionMonth ?: selectedYm
        val categories = cachedCategories.ifEmpty { DefaultSeed.getDefaultCategories() }
        val categoryMap = categories.associateBy { it.id }

        // 1. Month overview calculations
        val incomeTxs = cachedMonthTransactions.filter { it.type.equals("INCOME", ignoreCase = true) }
        val expenseTxs = cachedMonthTransactions.filter { it.type.equals("EXPENSE", ignoreCase = true) }

        val totalIncomeCents = incomeTxs.sumOf { it.amountCents }
        val totalExpenseCents = expenseTxs.sumOf { it.amountCents }
        val fixedExpenseCents = expenseTxs.filter { it.isFixed }.sumOf { it.amountCents }
        val variableExpenseCents = totalExpenseCents - fixedExpenseCents
        val netSavingsCents = totalIncomeCents - totalExpenseCents
        val savingsRatePercent = MonthlyAnalyticsCalculator.calculateSavingsRate(netSavingsCents, totalIncomeCents)

        // 2. Evolution 12 months & Historical monthly average calculation
        val currentYear = selectedYm / 100
        val monthLabels = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")

        val rawEvolutionData = (1..12).map { monthIndex ->
            val ym = currentYear * 100 + monthIndex
            val monthTxs = cachedYearTransactions.filter { it.yearMonth == ym }
            val mIncome = monthTxs.filter { it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amountCents }
            val mExpense = monthTxs.filter { it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amountCents }
            val mSavings = mIncome - mExpense
            val mSavingsRate = MonthlyAnalyticsCalculator.calculateSavingsRate(mSavings, mIncome)
            val hasActivity = mIncome > 0L || mExpense > 0L

            MonthlyBarCalculated(
                yearMonth = ym,
                label = monthLabels[monthIndex - 1],
                incomeCents = mIncome,
                expenseCents = mExpense,
                netSavingsCents = mSavings,
                savingsRatePercent = mSavingsRate,
                hasActivity = hasActivity
            )
        }

        // Active months average (no dilution by future zero-months)
        val activeExpenseMonths = rawEvolutionData.filter { it.expenseCents > 0L }
        val historicalMonthlyAverageExpenseCents = if (activeExpenseMonths.isNotEmpty()) {
            activeExpenseMonths.sumOf { it.expenseCents } / activeExpenseMonths.size
        } else {
            0L
        }

        val evolutionMonths = rawEvolutionData.map { item ->
            EvolutionMonthItem(
                yearMonth = item.yearMonth,
                label = item.label,
                incomeCents = item.incomeCents,
                expenseCents = item.expenseCents,
                netSavingsCents = item.netSavingsCents,
                savingsRatePercent = item.savingsRatePercent,
                hasActivity = item.hasActivity,
                formattedIncome = formatValue(item.incomeCents, item.incomeCents, displayMode),
                formattedExpense = formatValue(item.expenseCents, item.incomeCents, displayMode),
                formattedSavings = formatValue(item.netSavingsCents, item.incomeCents, displayMode),
                deviationVsAveragePercent = MonthlyAnalyticsCalculator.calculateDeviationVsAverage(
                    currentExpenseCents = item.expenseCents,
                    historicalAverageCents = historicalMonthlyAverageExpenseCents
                )
            )
        }

        val selectedEvolutionMonthItem = evolutionMonths.find { it.yearMonth == selectedEvoYm }
            ?: evolutionMonths.find { it.yearMonth == selectedYm }
            ?: evolutionMonths.firstOrNull()

        // 3. Lens: No Quê (Categories drill-down)
        data class ResolvedTx(
            val tx: TransactionEntity,
            val parentCat: CategoryEntity,
            val subCat: CategoryEntity?
        )

        val resolvedTxs = expenseTxs.map { tx ->
            val cat = categoryMap[tx.categoryId]
            if (cat == null) {
                val fallback = CategoryEntity(
                    id = tx.categoryId,
                    parentId = null,
                    name = tx.categoryId,
                    icon = "category",
                    colorHex = 0xFF888888,
                    type = "EXPENSE"
                )
                ResolvedTx(tx, fallback, null)
            } else if (cat.parentId != null) {
                val parent = categoryMap[cat.parentId] ?: cat
                ResolvedTx(tx, parent, cat)
            } else {
                ResolvedTx(tx, cat, null)
            }
        }

        val parentCategories = resolvedTxs.groupBy { it.parentCat }.map { (parent, items) ->
            val parentAmountCents = items.sumOf { it.tx.amountCents }
            val parentPctExpense = MonthlyAnalyticsCalculator.calculateExpenseShare(parentAmountCents, totalExpenseCents)
            val parentPctIncome = MonthlyAnalyticsCalculator.calculateIncomeShare(parentAmountCents, totalIncomeCents)

            val subcategoriesList = mutableListOf<SubcategoryItem>()

            val directItems = items.filter { it.subCat == null }
            if (directItems.isNotEmpty()) {
                val directAmount = directItems.sumOf { it.tx.amountCents }
                val pctOfParent = if (parentAmountCents > 0L) (directAmount.toDouble() / parentAmountCents.toDouble()) * 100.0 else 0.0
                subcategoriesList.add(
                    SubcategoryItem(
                        id = "${parent.id}_direct",
                        name = "${parent.name} (Geral)",
                        icon = parent.icon,
                        colorHex = parent.colorHex,
                        amountCents = directAmount,
                        formattedValue = formatValue(directAmount, totalIncomeCents, displayMode),
                        percentageOfParent = pctOfParent,
                        percentageOfIncome = MonthlyAnalyticsCalculator.calculateIncomeShare(directAmount, totalIncomeCents),
                        percentageOfExpense = MonthlyAnalyticsCalculator.calculateExpenseShare(directAmount, totalExpenseCents),
                        transactionCount = directItems.size,
                        transactions = directItems.map { it.tx }
                    )
                )
            }

            val bySubCat = items.filter { it.subCat != null }.groupBy { it.subCat!! }
            bySubCat.forEach { (subCat, subItems) ->
                val subAmount = subItems.sumOf { it.tx.amountCents }
                val pctOfParent = if (parentAmountCents > 0L) (subAmount.toDouble() / parentAmountCents.toDouble()) * 100.0 else 0.0
                subcategoriesList.add(
                    SubcategoryItem(
                        id = subCat.id,
                        name = subCat.name,
                        icon = subCat.icon,
                        colorHex = subCat.colorHex,
                        amountCents = subAmount,
                        formattedValue = formatValue(subAmount, totalIncomeCents, displayMode),
                        percentageOfParent = pctOfParent,
                        percentageOfIncome = MonthlyAnalyticsCalculator.calculateIncomeShare(subAmount, totalIncomeCents),
                        percentageOfExpense = MonthlyAnalyticsCalculator.calculateExpenseShare(subAmount, totalExpenseCents),
                        transactionCount = subItems.size,
                        transactions = subItems.map { it.tx }
                    )
                )
            }

            subcategoriesList.sortByDescending { it.amountCents }

            ParentCategoryItem(
                id = parent.id,
                name = parent.name,
                icon = parent.icon,
                colorHex = parent.colorHex,
                amountCents = parentAmountCents,
                formattedValue = formatValue(parentAmountCents, totalIncomeCents, displayMode),
                percentageOfExpense = parentPctExpense,
                percentageOfIncome = parentPctIncome,
                transactionCount = items.size,
                deviationVsAveragePercent = null,
                subcategories = subcategoriesList
            )
        }.sortedByDescending { it.amountCents }

        val activeSelectedParent = parentCategories.find { it.id == selectedParentId }
        val expandedSubcategories = activeSelectedParent?.subcategories ?: emptyList()

        // 4. Lens: Onde (Contexts + Top 10 Merchants)
        val contextLabelMap = DefaultSeed.CONTEXTS.associate { it.tag to it.label }
        val contexts = expenseTxs.groupBy { it.contextTag }.map { (tag, txs) ->
            val amount = txs.sumOf { it.amountCents }
            ContextItem(
                tag = tag,
                label = contextLabelMap[tag] ?: tag,
                amountCents = amount,
                formattedValue = formatValue(amount, totalIncomeCents, displayMode),
                percentageOfExpense = MonthlyAnalyticsCalculator.calculateExpenseShare(amount, totalExpenseCents),
                percentageOfIncome = MonthlyAnalyticsCalculator.calculateIncomeShare(amount, totalIncomeCents),
                transactionCount = txs.size
            )
        }.sortedByDescending { it.amountCents }

        val topMerchants = expenseTxs
            .filter { it.merchant.isNotBlank() }
            .groupBy { it.merchant.trim() }
            .map { (merchant, txs) ->
                val amount = txs.sumOf { it.amountCents }
                val count = txs.size
                val avgTicket = if (count > 0) amount / count else 0L
                MerchantItem(
                    merchant = merchant,
                    amountCents = amount,
                    formattedValue = formatValue(amount, totalIncomeCents, displayMode),
                    percentageOfExpense = MonthlyAnalyticsCalculator.calculateExpenseShare(amount, totalExpenseCents),
                    percentageOfIncome = MonthlyAnalyticsCalculator.calculateIncomeShare(amount, totalIncomeCents),
                    transactionCount = count,
                    averagePerVisitCents = avgTicket,
                    formattedAverageTicket = "${MoneyCents.formatCents(avgTicket)} / ida"
                )
            }
            .sortedByDescending { it.amountCents }
            .take(10)

        // 5. Lens: Como (Rigidity + Payment Methods)
        val rigidity = RigidityStructure(
            fixedExpenseCents = fixedExpenseCents,
            variableExpenseCents = variableExpenseCents,
            netSavingsCents = netSavingsCents,
            formattedFixed = formatValue(fixedExpenseCents, totalIncomeCents, displayMode),
            formattedVariable = formatValue(variableExpenseCents, totalIncomeCents, displayMode),
            formattedSavings = formatValue(netSavingsCents, totalIncomeCents, displayMode),
            fixedPercentOfIncome = MonthlyAnalyticsCalculator.calculateIncomeShare(fixedExpenseCents, totalIncomeCents),
            variablePercentOfIncome = MonthlyAnalyticsCalculator.calculateIncomeShare(variableExpenseCents, totalIncomeCents),
            savingsPercentOfIncome = MonthlyAnalyticsCalculator.calculateSavingsRate(netSavingsCents, totalIncomeCents)
        )

        val methodLabelMap = buildMap {
            putAll(DefaultSeed.PAYMENT_METHODS.associate { it.code to it.label })
            put("DEBITO", "Cartão")
            put("DINHEIRO", "Dinheiro")
            put("TRANSFERENCIA", "Transferência")
            put("DEBITO_DIRETO", "Débito Direto")
        }
        val paymentMethods = expenseTxs.groupBy {
            when (it.paymentMethod.uppercase()) {
                "DEBITO" -> "CARD"
                "DINHEIRO" -> "CASH"
                "TRANSFERENCIA" -> "TRANSFER"
                "DEBITO_DIRETO" -> "DIRECT_DEBIT"
                else -> it.paymentMethod.uppercase()
            }
        }.map { (method, txs) ->
            val amount = txs.sumOf { it.amountCents }
            MethodItem(
                code = method,
                label = methodLabelMap[method] ?: method,
                amountCents = amount,
                formattedValue = formatValue(amount, totalIncomeCents, displayMode),
                percentageOfExpense = MonthlyAnalyticsCalculator.calculateExpenseShare(amount, totalExpenseCents),
                percentageOfIncome = MonthlyAnalyticsCalculator.calculateIncomeShare(amount, totalIncomeCents),
                transactionCount = txs.size
            )
        }.sortedByDescending { it.amountCents }

        // 6. Lens: Comparar (current month vs baseline)
        val baselineMode = _uiState.value.comparisonBaseline
        val comparisonComputation: ComparisonComputation? = when (baselineMode) {
            ComparisonBaseline.MONTHLY_AVERAGE -> {
                val activeMonths = rawEvolutionData.filter { it.hasActivity }
                if (activeMonths.isEmpty()) {
                    null
                } else {
                    val monthsWithIncome = rawEvolutionData.filter { it.incomeCents > 0L }
                    val monthsWithExpense = rawEvolutionData.filter { it.expenseCents > 0L }
                    val averageIncomeCents = if (monthsWithIncome.isEmpty()) 0L else {
                        monthsWithIncome.sumOf { it.incomeCents } / monthsWithIncome.size
                    }
                    val averageExpenseCents = if (monthsWithExpense.isEmpty()) 0L else {
                        monthsWithExpense.sumOf { it.expenseCents } / monthsWithExpense.size
                    }
                    val averageSavingsCents = activeMonths.sumOf { it.netSavingsCents } / activeMonths.size
                    val baselineCategoryTxs = buildAverageBaselineTransactions(currentYear, categoryMap)
                    val monthComparison = MonthComparison(
                        currentExpenseCents = totalExpenseCents,
                        baselineExpenseCents = averageExpenseCents,
                        currentIncomeCents = totalIncomeCents,
                        baselineIncomeCents = averageIncomeCents,
                        currentSavingsCents = netSavingsCents,
                        baselineSavingsCents = averageSavingsCents,
                        expenseDeltaCents = totalExpenseCents - averageExpenseCents,
                        expenseDeltaPercent = ComparisonCalculator.calculatePercent(totalExpenseCents, averageExpenseCents),
                        incomeDeltaCents = totalIncomeCents - averageIncomeCents,
                        incomeDeltaPercent = ComparisonCalculator.calculatePercent(totalIncomeCents, averageIncomeCents),
                        savingsDeltaCents = netSavingsCents - averageSavingsCents,
                        savingsDeltaPercent = ComparisonCalculator.calculatePercent(netSavingsCents, averageSavingsCents),
                        categoryDeltas = comparisonCalculator.calculateCategoryDeltas(
                            cachedMonthTransactions,
                            baselineCategoryTxs,
                            categories
                        )
                    )
                    ComparisonComputation(monthComparison, averageIncomeCents)
                }
            }
            ComparisonBaseline.PREVIOUS_MONTH, ComparisonBaseline.SAME_MONTH_LAST_YEAR -> {
                val baselineYm = if (baselineMode == ComparisonBaseline.PREVIOUS_MONTH) {
                    val ym = YearMonth.of(selectedYm / 100, selectedYm % 100).minusMonths(1)
                    ym.year * 100 + ym.monthValue
                } else {
                    selectedYm - 100
                }
                val baselineTxs = cachedAllTransactions.filter { it.yearMonth == baselineYm }
                if (baselineTxs.isEmpty()) {
                    null
                } else {
                    val monthComparison = comparisonCalculator.calculateMonthComparison(
                        cachedMonthTransactions,
                        baselineTxs,
                        categories
                    )
                    ComparisonComputation(monthComparison, monthComparison.baselineIncomeCents)
                }
            }
        }
        val comparison = comparisonComputation?.let {
            formatMonthComparison(it.comparison, baselineMode, displayMode)
        }

        // 7. Evolution: category trend chips (top 8 of the year) + selected category series
        val yearExpenseTxs = cachedYearTransactions.filter { it.type.equals("EXPENSE", ignoreCase = true) }
        val yearParentSums = mutableMapOf<String, Long>()
        for (tx in yearExpenseTxs) {
            val parentId = when (val cat = categoryMap[tx.categoryId]) {
                null -> tx.categoryId
                else -> cat.parentId ?: cat.id
            }
            yearParentSums[parentId] = (yearParentSums[parentId] ?: 0L) + tx.amountCents
        }
        val topParentIds = yearParentSums.entries
            .sortedByDescending { it.value }
            .take(8)
            .map { it.key }

        if (evolutionCategorySelection == null || topParentIds.none { it == evolutionCategorySelection }) {
            evolutionCategorySelection = topParentIds.firstOrNull()
        }

        val trendChips = topParentIds.map { parentId ->
            val parent = categoryMap[parentId] ?: CategoryEntity(
                id = parentId,
                parentId = null,
                name = parentId,
                icon = "category",
                colorHex = 0xFF888888,
                type = "EXPENSE"
            )
            TrendChipUi(
                categoryId = parentId,
                name = parent.name,
                colorHex = parent.colorHex,
                selected = parentId == evolutionCategorySelection
            )
        }

        val incomeByMonth = rawEvolutionData.associateBy { it.yearMonth }
        val categoryTrend = evolutionCategorySelection?.let { selection ->
            val trend = trendCalculator.calculateCategoryTrend(selection, currentYear, cachedAllTransactions, categories)
            val trendPoints = trend.points.map { point ->
                val monthIncome = incomeByMonth[point.yearMonth]?.incomeCents ?: 0L
                LinePoint(
                    label = monthLabels[point.yearMonth % 100 - 1],
                    value = point.expenseCents.toDouble(),
                    formattedValue = formatValue(point.expenseCents, monthIncome, displayMode)
                )
            }
            val trendSelectedIdx = if (selectedEvoYm / 100 == currentYear) (selectedEvoYm % 100) - 1 else null
            CategoryTrendUi(
                categoryName = trend.categoryName,
                colorHex = trend.colorHex,
                points = trendPoints,
                averageExpenseCents = trend.averageExpenseCents,
                formattedAverage = formatValue(
                    trend.averageExpenseCents,
                    selectedEvolutionMonthItem?.incomeCents ?: totalIncomeCents,
                    displayMode
                ),
                selectedMonthDetail = trendSelectedIdx?.let { idx -> trendPoints.getOrNull(idx)?.formattedValue },
                selectedMonthDeviation = trendSelectedIdx?.let { idx ->
                    MonthlyAnalyticsCalculator.calculateDeviationVsAverage(
                        currentExpenseCents = trend.points[idx].expenseCents,
                        historicalAverageCents = trend.averageExpenseCents
                    )
                }
            )
        }

        // 8. Savings rate trend (12 months + average over active months)
        val savingsRatePoints = rawEvolutionData.map { item ->
            SavingsRatePoint(
                yearMonth = item.yearMonth,
                savingsRatePercent = item.savingsRatePercent,
                hasActivity = item.hasActivity
            )
        }
        val savingsRateAverage = trendCalculator.calculateSavingsRateAverage(savingsRatePoints)
        val savingsRateTrend = SavingsRateTrendUi(
            points = savingsRatePoints.map { point ->
                LinePoint(
                    label = monthLabels[point.yearMonth % 100 - 1],
                    value = point.savingsRatePercent,
                    formattedValue = MoneyCents.formatPercent(point.savingsRatePercent)
                )
            },
            formattedAverage = MoneyCents.formatPercent(savingsRateAverage),
            averageRatePercent = savingsRateAverage
        )

        // 9. Multi-year aggregates (year vs year)
        val yearAggregates = trendCalculator.calculateYearAggregates(cachedAllTransactions, categories)
        val yearPalette = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFFF59E0B), Color(0xFF10B981))
        val yearComparison = yearAggregates.mapIndexed { index, aggregate ->
            YearlyBarData(
                year = aggregate.year,
                monthlyExpenseCents = aggregate.monthlyExpenseCents,
                totalExpenseCents = aggregate.expenseCents,
                color = yearPalette[index % yearPalette.size]
            )
        }
        val yearTotals = yearAggregates.map { aggregate ->
            YearTotalsUi(
                year = aggregate.year,
                formattedIncome = formatValue(aggregate.incomeCents, aggregate.incomeCents, displayMode),
                formattedExpense = formatValue(aggregate.expenseCents, aggregate.incomeCents, displayMode),
                formattedSavings = formatValue(aggregate.netSavingsCents, aggregate.incomeCents, displayMode),
                formattedRate = MoneyCents.formatPercent(aggregate.savingsRatePercent)
            )
        }

        // 10. Calendar (per-day spend) + shareable monthly report
        val calendarDays = calendarAggregator
            .aggregate(selectedYm, cachedMonthTransactions, todayEpochDayProvider())
            .map { day ->
                CalendarDayItem(
                    epochDay = day.epochDay,
                    dayOfMonth = day.dayOfMonth,
                    totalExpenseCents = day.expenseCents,
                    formattedTotal = MoneyCents.formatCents(day.expenseCents),
                    transactionCount = day.transactionCount,
                    isToday = day.isToday
                )
            }

        val budgetReport: BudgetProgressReport? = buildBudgetReport(categories)

        val goalsProgress: List<GoalProgress>? = buildGoalsProgress(selectedYm)

        val monthlyReport = monthlyReportGenerator.generate(
            yearMonth = selectedYm,
            transactions = transactionsForMonth(selectedYm),
            categories = categories,
            budgetProgress = budgetReport,
            goalProgress = goalsProgress
        )

        val allTxs = if (cachedAllTransactions.isNotEmpty()) cachedAllTransactions else (cachedYearTransactions.ifEmpty { cachedMonthTransactions })
        val subscriptionReport = subscriptionDetector.detect(allTxs, selectedYm)

        // 11. Multi-year annual analysis for selected year
        val annualYear = _uiState.value.annualAnalysis.selectedYear
        val allYearTxs = if (cachedAllTransactions.isNotEmpty()) {
            cachedAllTransactions.filter { it.yearMonth / 100 == annualYear }
        } else {
            cachedYearTransactions.filter { it.yearMonth / 100 == annualYear }
        }

        val annualIncomeTxs = allYearTxs.filter { it.type.equals("INCOME", ignoreCase = true) }
        val annualExpenseTxs = allYearTxs.filter { it.type.equals("EXPENSE", ignoreCase = true) }

        val annualTotalIncomeCents = annualIncomeTxs.sumOf { it.amountCents }
        val annualTotalExpenseCents = annualExpenseTxs.sumOf { it.amountCents }
        val annualNetSavingsCents = annualTotalIncomeCents - annualTotalExpenseCents
        val annualSavingsRatePercent = MonthlyAnalyticsCalculator.calculateSavingsRate(annualNetSavingsCents, annualTotalIncomeCents)

        val annualMonths = (1..12).map { monthIdx ->
            val ym = annualYear * 100 + monthIdx
            val mTxs = allYearTxs.filter { it.yearMonth == ym }
            val mIncome = mTxs.filter { it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amountCents }
            val mExpense = mTxs.filter { it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amountCents }
            val mSavings = mIncome - mExpense
            val mSavingsRate = MonthlyAnalyticsCalculator.calculateSavingsRate(mSavings, mIncome)
            val hasActivity = mIncome > 0L || mExpense > 0L
            AnnualMonthSummary(
                yearMonth = ym,
                monthNumber = monthIdx,
                monthLabel = monthLabels[monthIdx - 1],
                incomeCents = mIncome,
                expenseCents = mExpense,
                netSavingsCents = mSavings,
                savingsRatePercent = mSavingsRate,
                hasActivity = hasActivity,
                formattedIncome = formatValue(mIncome, mIncome, displayMode),
                formattedExpense = formatValue(mExpense, mIncome, displayMode),
                formattedSavings = formatValue(mSavings, mIncome, displayMode),
                transactionCount = mTxs.size
            )
        }

        val annualActiveMonths = annualMonths.filter { it.expenseCents > 0L }
        val annualMonthlyAverageExpenseCents = if (annualActiveMonths.isNotEmpty()) {
            annualTotalExpenseCents / annualActiveMonths.size
        } else {
            0L
        }

        val annualResolvedTxs = annualExpenseTxs.map { tx ->
            val cat = categoryMap[tx.categoryId]
            if (cat == null) {
                val fallback = CategoryEntity(
                    id = tx.categoryId,
                    parentId = null,
                    name = tx.categoryId,
                    icon = "category",
                    colorHex = 0xFF888888,
                    type = "EXPENSE"
                )
                ResolvedTx(tx, fallback, null)
            } else if (cat.parentId != null) {
                val parent = categoryMap[cat.parentId] ?: cat
                ResolvedTx(tx, parent, cat)
            } else {
                ResolvedTx(tx, cat, null)
            }
        }

        val annualCategories = annualResolvedTxs.groupBy { it.parentCat }.map { (parent, items) ->
            val parentAmount = items.sumOf { it.tx.amountCents }
            val pctExpense = MonthlyAnalyticsCalculator.calculateExpenseShare(parentAmount, annualTotalExpenseCents)
            val pctIncome = MonthlyAnalyticsCalculator.calculateIncomeShare(parentAmount, annualTotalIncomeCents)

            val subList = mutableListOf<AnnualSubcategoryUi>()
            val directItems = items.filter { it.subCat == null }
            if (directItems.isNotEmpty()) {
                val dAmount = directItems.sumOf { it.tx.amountCents }
                val pctOfParent = if (parentAmount > 0L) (dAmount.toDouble() / parentAmount.toDouble()) * 100.0 else 0.0
                subList.add(
                    AnnualSubcategoryUi(
                        id = "${parent.id}_direct",
                        name = "${parent.name} (Geral)",
                        icon = parent.icon,
                        colorHex = parent.colorHex,
                        amountCents = dAmount,
                        formattedValue = formatValue(dAmount, annualTotalIncomeCents, displayMode),
                        percentageOfParent = pctOfParent,
                        transactionCount = directItems.size,
                        transactions = directItems.map { it.tx }.sortedWith(compareByDescending<TransactionEntity> { it.epochDay }.thenByDescending { it.id })
                    )
                )
            }

            val bySubCat = items.filter { it.subCat != null }.groupBy { it.subCat!! }
            bySubCat.forEach { (subCat, subItems) ->
                val sAmount = subItems.sumOf { it.tx.amountCents }
                val pctOfParent = if (parentAmount > 0L) (sAmount.toDouble() / parentAmount.toDouble()) * 100.0 else 0.0
                subList.add(
                    AnnualSubcategoryUi(
                        id = subCat.id,
                        name = subCat.name,
                        icon = subCat.icon,
                        colorHex = subCat.colorHex,
                        amountCents = sAmount,
                        formattedValue = formatValue(sAmount, annualTotalIncomeCents, displayMode),
                        percentageOfParent = pctOfParent,
                        transactionCount = subItems.size,
                        transactions = subItems.map { it.tx }.sortedWith(compareByDescending<TransactionEntity> { it.epochDay }.thenByDescending { it.id })
                    )
                )
            }
            subList.sortByDescending { it.amountCents }

            AnnualCategoryUi(
                id = parent.id,
                name = parent.name,
                icon = parent.icon,
                colorHex = parent.colorHex,
                amountCents = parentAmount,
                formattedValue = formatValue(parentAmount, annualTotalIncomeCents, displayMode),
                percentageOfExpense = pctExpense,
                percentageOfIncome = pctIncome,
                transactionCount = items.size,
                subcategories = subList,
                transactions = items.map { it.tx }.sortedWith(compareByDescending<TransactionEntity> { it.epochDay }.thenByDescending { it.id })
            )
        }.sortedByDescending { it.amountCents }

        val availableYears = if (cachedAllTransactions.isNotEmpty()) {
            cachedAllTransactions.map { it.yearMonth / 100 }.distinct().sortedDescending()
        } else {
            listOf(2026, 2025, 2024)
        }

        val currentAnnualState = _uiState.value.annualAnalysis
        val annualAnalysis = AnnualAnalysisUi(
            selectedYear = annualYear,
            availableYears = if (availableYears.isEmpty()) listOf(2026, 2025, 2024) else availableYears,
            totalIncomeCents = annualTotalIncomeCents,
            totalExpenseCents = annualTotalExpenseCents,
            netSavingsCents = annualNetSavingsCents,
            savingsRatePercent = annualSavingsRatePercent,
            monthlyAverageExpenseCents = annualMonthlyAverageExpenseCents,
            activeMonthsCount = annualActiveMonths.size,
            formattedTotalIncome = formatValue(annualTotalIncomeCents, annualTotalIncomeCents, displayMode),
            formattedTotalExpense = formatValue(annualTotalExpenseCents, annualTotalIncomeCents, displayMode),
            formattedNetSavings = formatValue(annualNetSavingsCents, annualTotalIncomeCents, displayMode),
            formattedSavingsRate = MoneyCents.formatPercent(annualSavingsRatePercent),
            formattedMonthlyAverageExpense = formatValue(annualMonthlyAverageExpenseCents, annualTotalIncomeCents, displayMode),
            months = annualMonths,
            categories = annualCategories,
            selectedMonthDetail = annualMonths.find { it.yearMonth == currentAnnualState.selectedMonthDetail?.yearMonth }
                ?: annualMonths.find { it.yearMonth == selectedYm }
                ?: annualMonths.firstOrNull { it.hasActivity },
            selectedCategoryId = currentAnnualState.selectedCategoryId,
            expandedSubcategoryId = currentAnnualState.expandedSubcategoryId
        )

        // 12. Quarterly History (3 months: M-2, M-1, Current)
        val currentYmObj = YearMonth.of(selectedYm / 100, selectedYm % 100)
        val ymMinus2 = currentYmObj.minusMonths(2)
        val ymMinus1 = currentYmObj.minusMonths(1)

        val monthsToCompare = listOf(
            (ymMinus2.year * 100 + ymMinus2.monthValue) to ymMinus2,
            (ymMinus1.year * 100 + ymMinus1.monthValue) to ymMinus1,
            selectedYm to currentYmObj
        )

        val ptLocale = Locale("pt", "PT")
        val quarterlyItems = monthsToCompare.map { (ym, ymObj) ->
            val monthTxs = if (ym == selectedYm) {
                cachedMonthTransactions
            } else {
                cachedAllTransactions.filter { it.yearMonth == ym }
            }
            val expCents = monthTxs.filter { it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amountCents }
            val rawName = ymObj.month.getDisplayName(TextStyle.SHORT, ptLocale)
            val monthName = rawName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(ptLocale) else it.toString() }
                .trimEnd('.')

            QuarterlyHistoryItem(
                yearMonth = ym,
                label = monthName,
                expenseCents = expCents,
                formattedExpense = formatValue(expCents, totalIncomeCents, displayMode),
                isCurrent = (ym == selectedYm)
            )
        }

        val activeMonthsWithExpense = quarterlyItems.filter { it.expenseCents > 0L }
        val avgQuarterlyCents = if (activeMonthsWithExpense.isNotEmpty()) {
            activeMonthsWithExpense.sumOf { it.expenseCents } / activeMonthsWithExpense.size
        } else {
            0L
        }

        val quarterlyHistory = QuarterlyHistoryUi(
            items = quarterlyItems,
            averageExpenseCents = avgQuarterlyCents,
            formattedAverageExpense = formatValue(avgQuarterlyCents, totalIncomeCents, displayMode)
        )

        _uiState.update { current ->
            current.copy(
                totalIncomeCents = totalIncomeCents,
                totalExpenseCents = totalExpenseCents,
                netSavingsCents = netSavingsCents,
                formattedTotalIncome = formatValue(totalIncomeCents, totalIncomeCents, displayMode),
                formattedTotalExpense = formatValue(totalExpenseCents, totalIncomeCents, displayMode),
                formattedNetSavings = formatValue(netSavingsCents, totalIncomeCents, displayMode),
                savingsRatePercent = savingsRatePercent,
                historicalMonthlyAverageExpenseCents = historicalMonthlyAverageExpenseCents,
                formattedHistoricalMonthlyAverage = formatValue(historicalMonthlyAverageExpenseCents, totalIncomeCents, displayMode),
                parentCategories = parentCategories,
                expandedSubcategories = expandedSubcategories,
                contexts = contexts,
                topMerchants = topMerchants,
                rigidity = rigidity,
                paymentMethods = paymentMethods,
                evolutionMonths = evolutionMonths,
                selectedEvolutionMonthItem = selectedEvolutionMonthItem,
                comparison = comparison,
                trendChips = trendChips,
                categoryTrend = categoryTrend,
                savingsRateTrend = savingsRateTrend,
                yearComparison = yearComparison,
                yearTotals = yearTotals,
                calendarDays = calendarDays,
                monthlyReport = monthlyReport,
                categoryMap = categoryMap,
                subscriptionReport = subscriptionReport,
                annualAnalysis = annualAnalysis,
                quarterlyHistory = quarterlyHistory
            )
        }
    }

    private data class ComparisonComputation(
        val comparison: MonthComparison,
        val baselineIncomeCents: Long
    )

    private fun buildAverageBaselineTransactions(
        year: Int,
        categoryMap: Map<String, CategoryEntity>
    ): List<TransactionEntity> {
        val monthlyByParent = mutableMapOf<String, MutableMap<Int, Long>>()
        for (tx in cachedYearTransactions) {
            if (!tx.type.equals("EXPENSE", ignoreCase = true)) continue
            if (tx.yearMonth / 100 != year) continue
            val parentId = when (val cat = categoryMap[tx.categoryId]) {
                null -> tx.categoryId
                else -> cat.parentId ?: cat.id
            }
            val month = tx.yearMonth % 100
            if (month !in 1..12) continue
            val perMonth = monthlyByParent.getOrPut(parentId) { mutableMapOf() }
            perMonth[month] = (perMonth[month] ?: 0L) + tx.amountCents
        }
        return monthlyByParent.map { (parentId, perMonth) ->
            val positiveMonths = perMonth.values.filter { it > 0L }
            val average = if (positiveMonths.isEmpty()) 0L else positiveMonths.sum() / positiveMonths.size
            TransactionEntity(
                type = "EXPENSE",
                amountCents = average,
                yearMonth = year * 100 + 1,
                epochDay = 0L,
                categoryId = parentId,
                contextTag = "",
                merchant = "",
                paymentMethod = ""
            )
        }.filter { it.amountCents > 0L }
    }

    private fun formatMonthComparison(
        comparison: MonthComparison,
        baselineMode: ComparisonBaseline,
        displayMode: DisplayMode
    ): MonthComparisonUi {
        val currentIncomeCents = comparison.currentIncomeCents
        val baselineIncomeCents = comparison.baselineIncomeCents
        return MonthComparisonUi(
            baselineLabel = baselineMode.label,
            currentExpense = formatValue(comparison.currentExpenseCents, currentIncomeCents, displayMode),
            baselineExpense = formatValue(comparison.baselineExpenseCents, baselineIncomeCents, displayMode),
            expenseDelta = formatDeltaCents(comparison.expenseDeltaCents),
            expenseDeltaPercent = comparison.expenseDeltaPercent?.let { MoneyCents.formatPercent(it) },
            expenseDeltaPositive = comparison.expenseDeltaCents > 0L,
            currentIncome = formatValue(comparison.currentIncomeCents, currentIncomeCents, displayMode),
            baselineIncome = formatValue(comparison.baselineIncomeCents, baselineIncomeCents, displayMode),
            incomeDelta = formatDeltaCents(comparison.incomeDeltaCents),
            incomeDeltaPercent = comparison.incomeDeltaPercent?.let { MoneyCents.formatPercent(it) },
            incomeDeltaPositive = comparison.incomeDeltaCents > 0L,
            currentSavings = formatValue(comparison.currentSavingsCents, currentIncomeCents, displayMode),
            baselineSavings = formatValue(comparison.baselineSavingsCents, baselineIncomeCents, displayMode),
            savingsDelta = formatDeltaCents(comparison.savingsDeltaCents),
            savingsDeltaPercent = comparison.savingsDeltaPercent?.let { MoneyCents.formatPercent(it) },
            savingsDeltaPositive = comparison.savingsDeltaCents > 0L,
            increases = comparison.categoryDeltas
                .filter { it.deltaCents > 0L }
                .take(5)
                .map { formatCategoryDelta(it, currentIncomeCents, baselineIncomeCents, displayMode) },
            decreases = comparison.categoryDeltas
                .filter { it.deltaCents < 0L }
                .take(5)
                .map { formatCategoryDelta(it, currentIncomeCents, baselineIncomeCents, displayMode) }
        )
    }

    private fun formatCategoryDelta(
        delta: CategoryDelta,
        currentIncomeCents: Long,
        baselineIncomeCents: Long,
        displayMode: DisplayMode
    ): CategoryDeltaUi {
        return CategoryDeltaUi(
            categoryId = delta.categoryId,
            categoryName = delta.categoryName,
            colorHex = delta.colorHex,
            current = formatValue(delta.currentCents, currentIncomeCents, displayMode),
            baseline = formatValue(delta.baselineCents, baselineIncomeCents, displayMode),
            delta = formatDeltaCents(delta.deltaCents),
            deltaPercent = delta.deltaPercent?.let { MoneyCents.formatPercent(it) }
        )
    }

    private fun formatDeltaCents(cents: Long): String = when {
        cents > 0L -> "+${MoneyCents.formatCents(cents)}"
        cents == 0L -> MoneyCents.formatCents(0L)
        else -> MoneyCents.formatCents(cents)
    }

    private data class MonthlyBarCalculated(
        val yearMonth: Int,
        val label: String,
        val incomeCents: Long,
        val expenseCents: Long,
        val netSavingsCents: Long,
        val savingsRatePercent: Double,
        val hasActivity: Boolean
    )

    companion object {
        fun formatValue(amountCents: Long, totalIncomeCents: Long, mode: DisplayMode): String {
            return when (mode) {
                DisplayMode.EURO -> MoneyCents.formatCents(amountCents)
                DisplayMode.INCOME_PERCENTAGE -> {
                    val pct = if (totalIncomeCents > 0L) {
                        (amountCents.toDouble() / totalIncomeCents.toDouble()) * 100.0
                    } else 0.0
                    "${MoneyCents.formatPercent(pct)} do recebido"
                }
            }
        }

        fun formatMonthTitle(yearMonth: Int): String = MonthlyReportGenerator.formatMonthTitle(yearMonth)

        fun provideFactory(
            transactionDao: TransactionDao,
            categoryDao: CategoryDao,
            budgetDao: BudgetDao? = null,
            goalDao: GoalDao? = null,
            monthlyAnalyticsCalculator: MonthlyAnalyticsCalculator = MonthlyAnalyticsCalculator(),
            subscriptionDetector: SubscriptionDetector = SubscriptionDetector()
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AnalyticsViewModel(
                    transactionDao = transactionDao,
                    categoryDao = categoryDao,
                    budgetDao = budgetDao,
                    goalDao = goalDao,
                    monthlyAnalyticsCalculator = monthlyAnalyticsCalculator,
                    subscriptionDetector = subscriptionDetector
                ) as T
            }
        }
    }
}
