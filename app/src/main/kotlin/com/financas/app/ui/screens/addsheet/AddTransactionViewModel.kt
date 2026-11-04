package com.financas.app.ui.screens.addsheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.financas.app.data.db.CategoryDao
import com.financas.app.data.db.InvestmentAssetDao
import com.financas.app.data.repository.InvestmentTransferService
import com.financas.app.domain.usecase.InvestmentTransferRules
import com.financas.app.data.db.TransactionDao
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.PendingTransactionEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.prefs.ContextTagsStore
import com.financas.app.domain.usecase.SmartPrefillEngine
import com.financas.app.ui.components.CalculatorNumpadEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddTransactionUiState(
    val type: String = "EXPENSE",
    val rawAmount: String = "0",
    val amountCents: Long = 0L,
    val merchant: String = "",
    val selectedParentCategoryId: String? = null,
    val selectedCategoryId: String = "",
    val selectedContextTag: String = "COMERCIO",
    val selectedPaymentMethod: String = "MBWAY",
    val isFixed: Boolean = false,
    val epochDay: Long = LocalDate.now().toEpochDay(),
    val editingTransactionId: Long? = null,
    val isRefund: Boolean = false,
    val linkedTransactionId: Long? = null,
    val hasLinkedRefunds: Boolean = false,
    val investmentAssetId: String? = null,
    val recentExpenses: List<TransactionEntity> = emptyList(),
    val recentMerchants: List<String> = emptyList(),
    val customContextTags: Set<String> = emptySet(),
    val parentCategories: List<CategoryEntity> = emptyList(),
    val subcategories: List<CategoryEntity> = emptyList(),
    val allCategories: List<CategoryEntity> = emptyList(),
    val isSaved: Boolean = false
)

class AddTransactionViewModel(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val smartPrefillEngine: SmartPrefillEngine = SmartPrefillEngine(transactionDao),
    private val contextTagsStore: ContextTagsStore,
    private val investmentAssetDao: InvestmentAssetDao? = null
) : ViewModel() {

    private val transferService: InvestmentTransferService? =
        investmentAssetDao?.let { InvestmentTransferService(transactionDao, it) }

    private val evaluator = CalculatorNumpadEvaluator()
    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    private var allTransactionsHistory: List<TransactionEntity> = emptyList()

    init {
        loadCustomContextTags()
        loadCategories()
        loadHistory()
    }

    private fun loadCustomContextTags() {
        _uiState.update { it.copy(customContextTags = contextTagsStore.getCustomTags()) }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryDao.getAll().collect { categories ->
                val parents = categories.filter { it.parentId == null && it.type == _uiState.value.type }
                val currentParent = _uiState.value.selectedParentCategoryId ?: parents.firstOrNull()?.id
                val subs = if (currentParent != null) {
                    categories.filter { it.parentId == currentParent }
                } else {
                    emptyList()
                }
                val defaultSelectedCategory = _uiState.value.selectedCategoryId.ifEmpty {
                    subs.firstOrNull()?.id ?: currentParent ?: ""
                }

                _uiState.update {
                    it.copy(
                        allCategories = categories,
                        parentCategories = parents,
                        selectedParentCategoryId = currentParent,
                        subcategories = subs,
                        selectedCategoryId = defaultSelectedCategory
                    )
                }
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            transactionDao.getAll().collect { transactions ->
                allTransactionsHistory = transactions
                val recent = smartPrefillEngine.getRecentMerchants(transactions, limit = 10)
                val expenses = transactions
                    .filter { it.type.equals("EXPENSE", ignoreCase = true) }
                    .take(20)
                _uiState.update { it.copy(recentMerchants = recent, recentExpenses = expenses) }
            }
        }
    }

    fun onTypeChanged(newType: String) {
        val parents = _uiState.value.allCategories.filter { it.parentId == null && it.type == newType }
        val firstParent = parents.firstOrNull()?.id
        val subs = if (firstParent != null) {
            _uiState.value.allCategories.filter { it.parentId == firstParent }
        } else {
            emptyList()
        }
        val defaultCat = subs.firstOrNull()?.id ?: firstParent ?: ""

        _uiState.update {
            it.copy(
                type = newType,
                parentCategories = parents,
                selectedParentCategoryId = firstParent,
                subcategories = subs,
                selectedCategoryId = defaultCat,
                isRefund = if (newType == "EXPENSE") false else it.isRefund,
                linkedTransactionId = if (newType == "EXPENSE") null else it.linkedTransactionId
            )
        }
    }

    fun onRefundToggled(isRefund: Boolean) {
        _uiState.update { current ->
            current.copy(
                isRefund = isRefund,
                linkedTransactionId = if (isRefund) current.linkedTransactionId else null
            )
        }
    }

    fun onLinkedTransactionSelected(linkedTxId: Long?) {
        val linkedTx = allTransactionsHistory.find { it.id == linkedTxId }
        _uiState.update { current ->
            if (linkedTx != null) {
                val updatedMerchant = if (current.merchant.isBlank()) "Reembolso: ${linkedTx.merchant.ifBlank { "Despesa" }}" else current.merchant
                current.copy(
                    linkedTransactionId = linkedTxId,
                    merchant = updatedMerchant
                )
            } else {
                current.copy(linkedTransactionId = linkedTxId)
            }
        }
    }

    fun onDigit(digit: Char) {
        evaluator.appendDigit(digit)
        syncEvaluator()
    }

    fun onDoubleZero() {
        evaluator.appendDoubleZero()
        syncEvaluator()
    }

    fun onComma() {
        evaluator.appendComma()
        syncEvaluator()
    }

    fun onPlus() {
        evaluator.appendPlus()
        syncEvaluator()
    }

    fun onBackspace() {
        evaluator.backspace()
        syncEvaluator()
    }

    fun onClear() {
        evaluator.clear()
        syncEvaluator()
    }

    fun addQuickAmountCents(deltaCents: Long) {
        val newAmountCents = _uiState.value.amountCents + deltaCents
        setRawAmount(formatCentsAsRaw(newAmountCents))
    }

    fun setRawAmount(input: String) {
        evaluator.setRaw(input)
        syncEvaluator()
    }

    private fun syncEvaluator() {
        _uiState.update {
            it.copy(
                rawAmount = evaluator.rawInput,
                amountCents = evaluator.evaluateToCents()
            )
        }
    }

    fun onMerchantChanged(merchant: String) {
        _uiState.update { it.copy(merchant = merchant) }
    }

    fun onMerchantChipSelected(merchant: String) {
        val suggestion = smartPrefillEngine.findPrefill(merchant, allTransactionsHistory)
        if (suggestion != null) {
            val category = _uiState.value.allCategories.find { it.id == suggestion.categoryId }
            val parentId = category?.parentId ?: category?.id

            val subs = if (parentId != null) {
                _uiState.value.allCategories.filter { it.parentId == parentId }
            } else {
                _uiState.value.subcategories
            }

            _uiState.update {
                it.copy(
                    merchant = suggestion.merchant,
                    selectedCategoryId = suggestion.categoryId,
                    selectedParentCategoryId = parentId,
                    subcategories = subs,
                    selectedContextTag = suggestion.contextTag,
                    selectedPaymentMethod = suggestion.paymentMethod,
                    isFixed = suggestion.isFixed
                )
            }
        } else {
            _uiState.update { it.copy(merchant = merchant) }
        }
    }

    fun onParentCategorySelected(parentId: String) {
        val subs = _uiState.value.allCategories.filter { it.parentId == parentId }
        val newSelectedCategory = subs.firstOrNull()?.id ?: parentId
        _uiState.update {
            it.copy(
                selectedParentCategoryId = parentId,
                subcategories = subs,
                selectedCategoryId = newSelectedCategory
            )
        }
    }

    fun onSubcategorySelected(categoryId: String) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun createCategory(
        name: String,
        parentId: String? = null,
        type: String = _uiState.value.type,
        colorHex: Long = 0xFF3B82F6,
        icon: String = "category",
        isFixed: Boolean = false
    ) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            val safeName = trimmed.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
            val prefix = if (parentId != null) parentId else if (type == "INCOME") "inc" else "exp"
            val id = "${prefix}_${safeName}_${System.currentTimeMillis() % 10000}"

            val existingInParent = if (parentId != null) {
                _uiState.value.allCategories.filter { it.parentId == parentId }
            } else {
                _uiState.value.parentCategories
            }
            val nextSortOrder = (existingInParent.maxOfOrNull { it.sortOrder } ?: 0) + 1

            val newCategory = CategoryEntity(
                id = id,
                parentId = parentId,
                name = trimmed,
                icon = icon,
                colorHex = colorHex,
                type = type,
                isDefaultFixed = isFixed,
                sortOrder = nextSortOrder
            )

            categoryDao.insert(newCategory)

            if (parentId != null) {
                onParentCategorySelected(parentId)
                onSubcategorySelected(id)
            } else {
                onParentCategorySelected(id)
            }
        }
    }

    fun onContextSelected(contextTag: String) {
        _uiState.update { it.copy(selectedContextTag = contextTag) }
    }

    fun onPaymentMethodSelected(method: String) {
        _uiState.update { it.copy(selectedPaymentMethod = method) }
    }

    fun onFixedToggled(isFixed: Boolean) {
        _uiState.update { it.copy(isFixed = isFixed) }
    }

    fun onDateSelected(epochDay: Long) {
        _uiState.update { it.copy(epochDay = epochDay) }
    }

    fun startEdit(tx: TransactionEntity) {
        val categories = _uiState.value.allCategories
        val category = categories.find { it.id == tx.categoryId }
        val parentId = category?.parentId ?: category?.id
        val subs = if (parentId != null) categories.filter { it.parentId == parentId } else emptyList()
        val parents = categories.filter { it.parentId == null && it.type == tx.type }
        val raw = formatCentsAsRaw(tx.amountCents)
        val isRefund = tx.type.equals("INCOME", ignoreCase = true) && tx.linkedTransactionId != null
        val hasRefunds = tx.type.equals("EXPENSE", ignoreCase = true) &&
                allTransactionsHistory.any { it.linkedTransactionId == tx.id }

        evaluator.setRaw(raw)

        _uiState.update {
            it.copy(
                type = tx.type,
                rawAmount = raw,
                amountCents = tx.amountCents,
                merchant = tx.merchant,
                selectedParentCategoryId = parentId,
                selectedCategoryId = tx.categoryId,
                selectedContextTag = tx.contextTag,
                selectedPaymentMethod = tx.paymentMethod,
                isFixed = tx.isFixed,
                epochDay = tx.epochDay,
                editingTransactionId = tx.id,
                isRefund = isRefund,
                linkedTransactionId = tx.linkedTransactionId,
                investmentAssetId = tx.investmentAssetId,
                hasLinkedRefunds = hasRefunds,
                parentCategories = parents,
                subcategories = subs
            )
        }
    }

    fun startNewEntry() {
        evaluator.clear()

        val categories = _uiState.value.allCategories
        val parents = categories.filter { it.parentId == null && it.type == "EXPENSE" }
        val firstParent = parents.firstOrNull()?.id
        val subs = if (firstParent != null) categories.filter { it.parentId == firstParent } else emptyList()
        val defaultCat = subs.firstOrNull()?.id ?: firstParent ?: ""

        _uiState.update { current ->
            AddTransactionUiState(
                recentMerchants = current.recentMerchants,
                recentExpenses = current.recentExpenses,
                customContextTags = current.customContextTags,
                allCategories = categories,
                parentCategories = parents,
                subcategories = subs,
                selectedParentCategoryId = firstParent,
                selectedCategoryId = defaultCat,
                isRefund = false,
                linkedTransactionId = null,
                investmentAssetId = null
            )
        }
    }

    fun startFromPending(pending: PendingTransactionEntity) {
        val euros = pending.amountCents / 100
        val cents = pending.amountCents % 100
        val raw = if (cents == 0L) "$euros" else "$euros,$cents"
        evaluator.setRaw(raw)

        val categories = _uiState.value.allCategories
        val cat = categories.firstOrNull { it.id == pending.suggestedCategoryId }
        val parentId = cat?.parentId ?: cat?.id
        val pendingType = pending.suggestedType
        val parents = categories.filter { it.parentId == null && it.type == pendingType }
        val subs = if (parentId != null) categories.filter { it.parentId == parentId } else emptyList()

        _uiState.update {
            it.copy(
                type = pendingType,
                rawAmount = raw,
                amountCents = pending.amountCents,
                merchant = pending.rawMerchant,
                selectedParentCategoryId = parentId,
                selectedCategoryId = pending.suggestedCategoryId,
                selectedContextTag = pending.suggestedContextTag,
                selectedPaymentMethod = pending.suggestedPaymentMethod,
                isFixed = pending.isFixed,
                epochDay = pending.detectedEpochDay,
                editingTransactionId = null,
                isRefund = false,
                linkedTransactionId = null,
                investmentAssetId = null,
                hasLinkedRefunds = false,
                parentCategories = parents,
                subcategories = subs
            )
        }
    }

    fun saveTransaction(onSuccess: () -> Unit = {}) {
        val state = _uiState.value
        val cents = state.amountCents
        if (cents <= 0L) return

        val date = LocalDate.ofEpochDay(state.epochDay)
        val yearMonth = date.year * 100 + date.monthValue
        val editingId = state.editingTransactionId
        val linkedId = if (state.isRefund && state.type.equals("INCOME", ignoreCase = true)) {
            state.linkedTransactionId
        } else {
            null
        }

        val entity = TransactionEntity(
            id = editingId ?: 0L,
            type = state.type,
            amountCents = cents,
            yearMonth = yearMonth,
            epochDay = state.epochDay,
            categoryId = state.selectedCategoryId.ifEmpty { "exp_financial_other" },
            contextTag = state.selectedContextTag,
            merchant = state.merchant.trim(),
            paymentMethod = state.selectedPaymentMethod,
            isFixed = state.isFixed,
            linkedTransactionId = linkedId,
            investmentAssetId = state.investmentAssetId
        )

        viewModelScope.launch {
            if (editingId != null) {
                val linkedAssetId = state.investmentAssetId
                val original = allTransactionsHistory.find { it.id == editingId }
                var toSave = entity
                if (linkedAssetId != null && original != null) {
                    if (state.type == InvestmentTransferRules.TRANSFER_TYPE) {
                        transferService?.adjustContribution(linkedAssetId, cents - original.amountCents)
                    } else {
                        transferService?.adjustContribution(linkedAssetId, -original.amountCents)
                        toSave = entity.copy(investmentAssetId = null)
                    }
                }
                transactionDao.update(toSave)
            } else {
                transactionDao.insert(entity)
            }
            _uiState.update { it.copy(isSaved = true) }
            onSuccess()
        }
    }

    fun deleteTransaction(onSuccess: () -> Unit = {}) {
        val editingId = _uiState.value.editingTransactionId ?: return

        viewModelScope.launch {
            val target = transactionDao.getAllSync().find { it.id == editingId } ?: return@launch
            if (target.type.equals("EXPENSE", ignoreCase = true)) {
                transactionDao.getRefundsForTransaction(editingId).forEach { refund ->
                    transactionDao.update(refund.copy(linkedTransactionId = null))
                }
            }
            if (target.investmentAssetId != null && transferService != null) {
                transferService.deleteTransaction(target)
            } else {
                transactionDao.deleteById(editingId)
            }
            startNewEntry()
            onSuccess()
        }
    }

    private fun formatCentsAsRaw(cents: Long): String {
        val euros = cents / 100
        val fraction = (cents % 100).toInt()
        return if (fraction == 0) euros.toString() else "$euros," + fraction.toString().padStart(2, '0')
    }

    companion object {
        fun provideFactory(
            transactionDao: TransactionDao,
            categoryDao: CategoryDao,
            smartPrefillEngine: SmartPrefillEngine,
            contextTagsStore: ContextTagsStore,
            investmentAssetDao: InvestmentAssetDao? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AddTransactionViewModel(transactionDao, categoryDao, smartPrefillEngine, contextTagsStore, investmentAssetDao) as T
            }
        }
    }
}
