package com.financas.app.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.financas.app.data.db.CategoryDao
import com.financas.app.data.db.InvestmentAssetDao
import com.financas.app.data.repository.InvestmentTransferService
import com.financas.app.data.db.TransactionDao
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.seed.DefaultSeed
import com.financas.app.domain.usecase.MonthlyAnalyticsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class PeriodFilter(val label: String) {
    CURRENT_MONTH("Mês"),
    YEAR("Ano"),
    ALL("Todos")
}

enum class FlowFilter(val label: String) {
    ALL("Todos"),
    FIXED("Fixos"),
    VARIABLE("Variáveis"),
    INCOMES("Receitas")
}

/**
 * Faixas de valor aplicadas ao montante de cada movimento (receitas e despesas
 * usam o seu próprio valor). As fronteiras pertencem às faixas fechadas:
 * 10,00 € entra em "≤ 10 €" e 50,00 € entra em "≥ 50 €"; a faixa intermédia
 * é estritamente entre 10,00 € e 50,00 €.
 */
enum class AmountFilter(val label: String) {
    ANY("Todos"),
    UP_TO_10("≤ 10 €"),
    BETWEEN_10_50("10–50 €"),
    OVER_50("≥ 50 €");

    fun matches(amountCents: Long): Boolean = when (this) {
        ANY -> true
        UP_TO_10 -> amountCents <= 1000L
        BETWEEN_10_50 -> amountCents > 1000L && amountCents < 5000L
        OVER_50 -> amountCents >= 5000L
    }
}

data class TransactionsUiState(
    val searchQuery: String = "",
    val periodFilter: PeriodFilter = PeriodFilter.CURRENT_MONTH,
    val flowFilter: FlowFilter = FlowFilter.ALL,
    val contextFilter: String? = null,
    val methodFilter: String? = null,
    val amountFilter: AmountFilter = AmountFilter.ANY,
    val currentYearMonth: Int,
    val monthTitle: String = "",
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val filteredCount: Int = 0,
    val filteredTotalCents: Long = 0L,
    val filteredExpenseCents: Long = 0L,
    val filteredIncomeCents: Long = 0L,
    val periodIncomeCents: Long = 0L,
    val filteredIncomeSharePercent: Double = 0.0,
    val categoryMap: Map<String, CategoryEntity> = emptyMap(),
    val refundsMap: Map<Long, Long> = emptyMap(),
    val isLoading: Boolean = false
)

class TransactionsViewModel(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    investmentAssetDao: InvestmentAssetDao? = null,
    initialYearMonth: Int = run {
        val now = LocalDate.now()
        now.year * 100 + now.monthValue
    }
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TransactionsUiState(
            currentYearMonth = initialYearMonth,
            monthTitle = formatMonthTitle(initialYearMonth)
        )
    )
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    private var allTransactions: List<TransactionEntity> = emptyList()
    private var allCategories: Map<String, CategoryEntity> = emptyMap()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            categoryDao.getAll().collect { categories ->
                val cats = if (categories.isEmpty()) DefaultSeed.getDefaultCategories() else categories
                allCategories = cats.associateBy { it.id }
                _uiState.update { it.copy(categoryMap = allCategories) }
                applyFilters()
            }
        }

        viewModelScope.launch {
            transactionDao.getAll().collect { transactions ->
                allTransactions = transactions
                applyFilters()
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onPeriodFilterChanged(period: PeriodFilter) {
        _uiState.update { it.copy(periodFilter = period) }
        applyFilters()
    }

    fun setYearMonth(yearMonth: Int) {
        _uiState.update {
            it.copy(
                currentYearMonth = yearMonth,
                monthTitle = formatMonthTitle(yearMonth),
                periodFilter = PeriodFilter.CURRENT_MONTH
            )
        }
        applyFilters()
    }

    fun selectPreviousMonth() {
        val current = _uiState.value.currentYearMonth
        val ym = YearMonth.of(current / 100, current % 100).minusMonths(1)
        setYearMonth(ym.year * 100 + ym.monthValue)
    }

    fun selectNextMonth() {
        val current = _uiState.value.currentYearMonth
        val ym = YearMonth.of(current / 100, current % 100).plusMonths(1)
        setYearMonth(ym.year * 100 + ym.monthValue)
    }

    fun onFlowFilterChanged(flow: FlowFilter) {
        _uiState.update { it.copy(flowFilter = flow) }
        applyFilters()
    }

    fun onContextFilterChanged(contextTag: String?) {
        val next = if (_uiState.value.contextFilter == contextTag) null else contextTag
        _uiState.update { it.copy(contextFilter = next) }
        applyFilters()
    }

    fun onMethodFilterChanged(method: String?) {
        val next = if (_uiState.value.methodFilter == method) null else method
        _uiState.update { it.copy(methodFilter = next) }
        applyFilters()
    }

    fun onAmountFilterChanged(filter: AmountFilter) {
        val next = if (_uiState.value.amountFilter == filter) AmountFilter.ANY else filter
        _uiState.update { it.copy(amountFilter = next) }
        applyFilters()
    }

    private val transferService: InvestmentTransferService? =
        investmentAssetDao?.let { InvestmentTransferService(transactionDao, it) }

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

    fun applyFilters() {
        val state = _uiState.value
        val currentYm = state.currentYearMonth
        val currentYear = currentYm / 100

        // 1. Period filter
        val inPeriod = allTransactions.filter { tx ->
            when (state.periodFilter) {
                PeriodFilter.CURRENT_MONTH -> tx.yearMonth == currentYm
                PeriodFilter.YEAR -> (tx.yearMonth / 100) == currentYear
                PeriodFilter.ALL -> true
            }
        }

        // Calculate total income in period for denominator (excluding refunds)
        val periodIncome = inPeriod
            .filter { it.type.equals("INCOME", ignoreCase = true) && it.linkedTransactionId == null }
            .sumOf { it.amountCents }

        // 2. Flow filter
        val inFlow = inPeriod.filter { tx ->
            when (state.flowFilter) {
                FlowFilter.ALL -> true
                FlowFilter.FIXED -> tx.isFixed
                FlowFilter.VARIABLE -> !tx.isFixed && tx.type.equals("EXPENSE", ignoreCase = true)
                FlowFilter.INCOMES -> tx.type.equals("INCOME", ignoreCase = true)
            }
        }

        // 3. Context filter
        val inContext = if (state.contextFilter != null) {
            inFlow.filter { it.contextTag.equals(state.contextFilter, ignoreCase = true) }
        } else {
            inFlow
        }

        // 4. Method filter
        val inMethod = if (state.methodFilter != null) {
            inContext.filter { tx ->
                tx.paymentMethod.equals(state.methodFilter, ignoreCase = true) ||
                (state.methodFilter.equals("CARD", ignoreCase = true) && tx.paymentMethod.equals("DEBITO", ignoreCase = true)) ||
                (state.methodFilter.equals("TRANSFER", ignoreCase = true) && tx.paymentMethod.equals("TRANSFERENCIA", ignoreCase = true)) ||
                (state.methodFilter.equals("CASH", ignoreCase = true) && tx.paymentMethod.equals("DINHEIRO", ignoreCase = true))
            }
        } else {
            inContext
        }

        // 5. Amount filter
        val inAmount = if (state.amountFilter == AmountFilter.ANY) {
            inMethod
        } else {
            inMethod.filter { tx -> state.amountFilter.matches(tx.amountCents) }
        }

        // 6. Search Query
        val query = state.searchQuery.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            inAmount
        } else {
            inAmount.filter { tx ->
                val categoryName = allCategories[tx.categoryId]?.name?.lowercase().orEmpty()
                val merchant = tx.merchant.lowercase()
                val context = tx.contextTag.lowercase()
                val method = tx.paymentMethod.lowercase()

                merchant.contains(query) ||
                        categoryName.contains(query) ||
                        context.contains(query) ||
                        method.contains(query)
            }
        }.sortedWith(compareByDescending<TransactionEntity> { it.epochDay }.thenByDescending { it.id })

        val totalCents = filtered.sumOf { it.amountCents }
        val filteredExpenseCents = filtered
            .filter { it.type.equals("EXPENSE", ignoreCase = true) }
            .sumOf { it.amountCents }
        val filteredIncomeCents = filtered
            .filter { it.type.equals("INCOME", ignoreCase = true) }
            .sumOf { it.amountCents }

        val incomeShare = when (state.flowFilter) {
            FlowFilter.ALL -> MonthlyAnalyticsCalculator.calculateIncomeShare(
                filteredExpenseCents,
                periodIncome
            )
            FlowFilter.FIXED, FlowFilter.VARIABLE, FlowFilter.INCOMES ->
                MonthlyAnalyticsCalculator.calculateIncomeShare(totalCents, periodIncome)
        }

        val refundsMap = allTransactions
            .filter { it.type.equals("INCOME", ignoreCase = true) && it.linkedTransactionId != null }
            .groupBy { it.linkedTransactionId!! }
            .mapValues { (_, refunds) -> refunds.sumOf { it.amountCents } }

        _uiState.update {
            it.copy(
                filteredTransactions = filtered,
                filteredCount = filtered.size,
                filteredTotalCents = totalCents,
                filteredExpenseCents = filteredExpenseCents,
                filteredIncomeCents = filteredIncomeCents,
                periodIncomeCents = periodIncome,
                filteredIncomeSharePercent = incomeShare,
                refundsMap = refundsMap
            )
        }
    }

    companion object {
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
            investmentAssetDao: InvestmentAssetDao? = null,
            initialYearMonth: Int? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return if (initialYearMonth != null) {
                    TransactionsViewModel(transactionDao, categoryDao, investmentAssetDao, initialYearMonth) as T
                } else {
                    TransactionsViewModel(transactionDao, categoryDao, investmentAssetDao) as T
                }
            }
        }
    }
}
