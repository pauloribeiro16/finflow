package com.financas.app.ui.fakes

import com.financas.app.data.db.BudgetDao
import com.financas.app.data.db.CategoryDao
import com.financas.app.data.db.GoalDao
import com.financas.app.data.db.InvestmentAssetDao
import com.financas.app.data.db.PendingTransactionDao
import com.financas.app.data.db.TransactionDao
import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.data.entity.PendingTransactionEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.prefs.ContextTagsStore
import com.financas.app.data.prefs.UserProfileStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicInteger

class FakeCategoryDao(initialCategories: List<CategoryEntity> = emptyList()) : CategoryDao {

    private val categories = MutableStateFlow(initialCategories)

    override fun getAll(): Flow<List<CategoryEntity>> = categories.asStateFlow()

    override suspend fun getAllSync(): List<CategoryEntity> = categories.value

    override fun getByType(type: String): Flow<List<CategoryEntity>> =
        categories.map { list -> list.filter { it.type == type } }

    override fun getParentCategories(): Flow<List<CategoryEntity>> =
        categories.map { list -> list.filter { it.parentId == null } }

    override fun getSubcategories(parentId: String): Flow<List<CategoryEntity>> =
        categories.map { list -> list.filter { it.parentId == parentId } }

    override suspend fun getById(id: String): CategoryEntity? =
        categories.value.find { it.id == id }

    override suspend fun insert(category: CategoryEntity) {
        val current = categories.value.filterNot { it.id == category.id }.toMutableList()
        current.add(category)
        categories.value = current
    }

    override suspend fun insertAll(newCategories: List<CategoryEntity>) {
        val newIds = newCategories.map { it.id }.toSet()
        val current = categories.value.filterNot { it.id in newIds }.toMutableList()
        current.addAll(newCategories)
        categories.value = current
    }

    override suspend fun update(category: CategoryEntity) {
        insert(category)
    }

    override suspend fun delete(category: CategoryEntity) {
        categories.value = categories.value.filterNot { it.id == category.id }
    }

    override suspend fun count(): Int = categories.value.size
}

class FakeTransactionDao(initialTransactions: List<TransactionEntity> = emptyList()) : TransactionDao {

    private val transactions = MutableStateFlow(initialTransactions)
    private var nextId = (initialTransactions.maxOfOrNull { it.id } ?: 0L) + 1L

    override fun getByMonth(yearMonth: Int): Flow<List<TransactionEntity>> =
        transactions.map { list ->
            list.filter { it.yearMonth == yearMonth }
                .sortedWith(compareByDescending<TransactionEntity> { it.epochDay }.thenByDescending { it.id })
        }

    override suspend fun getByMonthSync(yearMonth: Int): List<TransactionEntity> =
        transactions.value.filter { it.yearMonth == yearMonth }
            .sortedWith(compareByDescending<TransactionEntity> { it.epochDay }.thenByDescending { it.id })

    override fun getByMonthAndType(yearMonth: Int, type: String): Flow<List<TransactionEntity>> =
        transactions.map { list ->
            list.filter { it.yearMonth == yearMonth && it.type == type }
        }

    override fun getByYearMonthRange(startYearMonth: Int, endYearMonth: Int): Flow<List<TransactionEntity>> =
        transactions.map { list ->
            list.filter { it.yearMonth in startYearMonth..endYearMonth }
        }

    override suspend fun getByYearMonthRangeSync(startYearMonth: Int, endYearMonth: Int): List<TransactionEntity> =
        transactions.value.filter { it.yearMonth in startYearMonth..endYearMonth }

    override fun getAll(): Flow<List<TransactionEntity>> =
        transactions.map { list ->
            list.sortedWith(compareByDescending<TransactionEntity> { it.epochDay }.thenByDescending { it.id })
        }

    override suspend fun getAllSync(): List<TransactionEntity> =
        transactions.value.sortedWith(compareByDescending<TransactionEntity> { it.epochDay }.thenByDescending { it.id })

    override fun getDistinctMerchants(): Flow<List<String>> =
        transactions.map { list ->
            list.map { it.merchant }.filter { it.isNotBlank() }.distinct()
        }

    override suspend fun getLatestByMerchant(merchant: String): TransactionEntity? =
        transactions.value
            .filter { it.merchant.trim().equals(merchant.trim(), ignoreCase = true) }
            .maxWithOrNull(compareBy({ it.epochDay }, { it.id }))

    override suspend fun getFixedTransactionsForMonth(yearMonth: Int): List<TransactionEntity> =
        transactions.value.filter { it.yearMonth == yearMonth && it.isFixed }

    override suspend fun getRefundsForTransaction(parentTxId: Long): List<TransactionEntity> =
        transactions.value.filter { it.linkedTransactionId == parentTxId }

    override suspend fun getRecentExpensesSync(limit: Int): List<TransactionEntity> =
        transactions.value
            .filter { it.type == "EXPENSE" }
            .sortedByDescending { it.epochDay }
            .take(limit)

    override suspend fun insert(transaction: TransactionEntity): Long {
        val id = if (transaction.id == 0L) nextId++ else transaction.id
        val entity = transaction.copy(id = id)
        val current = transactions.value.filterNot { it.id == id }.toMutableList()
        current.add(entity)
        transactions.value = current
        return id
    }

    override suspend fun insertAll(newTransactions: List<TransactionEntity>) {
        val current = transactions.value.toMutableList()
        for (tx in newTransactions) {
            val id = if (tx.id == 0L) nextId++ else tx.id
            current.add(tx.copy(id = id))
        }
        transactions.value = current
    }

    override suspend fun update(transaction: TransactionEntity) {
        val current = transactions.value.filterNot { it.id == transaction.id }.toMutableList()
        current.add(transaction)
        transactions.value = current
    }

    override suspend fun delete(transaction: TransactionEntity) {
        transactions.value = transactions.value.filterNot { it.id == transaction.id }
    }

    override suspend fun deleteById(id: Long) {
        transactions.value = transactions.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        transactions.value = emptyList()
    }

    override suspend fun count(): Int = transactions.value.size
}

/**
 * Contador de subscrições [Flow] ativas sobre [FakeTransactionDao.getByMonth].
 *
 * Cada `collect` incrementa o contador e decrementa-o no cancelamento, o que
 * permite verificar que os ViewModels não acumulam coletores ao mudar de mês.
 */
class CountingTransactionDao(
    initialTransactions: List<TransactionEntity> = emptyList()
) : TransactionDao {

    private val delegate = FakeTransactionDao(initialTransactions)
    private val activeMonthCollectors = AtomicInteger(0)
    private val totalMonthSubscriptions = AtomicInteger(0)

    val activeSubscriptions: Int get() = activeMonthCollectors.get()
    val totalSubscriptions: Int get() = totalMonthSubscriptions.get()

    private fun <T> tracked(source: Flow<T>): Flow<T> = flow {
        activeMonthCollectors.incrementAndGet()
        totalMonthSubscriptions.incrementAndGet()
        try {
            emitAll(source)
        } finally {
            activeMonthCollectors.decrementAndGet()
        }
    }

    override fun getByMonth(yearMonth: Int): Flow<List<TransactionEntity>> =
        tracked(delegate.getByMonth(yearMonth))

    override suspend fun getByMonthSync(yearMonth: Int): List<TransactionEntity> =
        delegate.getByMonthSync(yearMonth)

    override fun getByMonthAndType(yearMonth: Int, type: String): Flow<List<TransactionEntity>> =
        delegate.getByMonthAndType(yearMonth, type)

    override fun getByYearMonthRange(startYearMonth: Int, endYearMonth: Int): Flow<List<TransactionEntity>> =
        delegate.getByYearMonthRange(startYearMonth, endYearMonth)

    override suspend fun getByYearMonthRangeSync(startYearMonth: Int, endYearMonth: Int): List<TransactionEntity> =
        delegate.getByYearMonthRangeSync(startYearMonth, endYearMonth)

    override fun getAll(): Flow<List<TransactionEntity>> = delegate.getAll()

    override suspend fun getAllSync(): List<TransactionEntity> = delegate.getAllSync()

    override fun getDistinctMerchants(): Flow<List<String>> = delegate.getDistinctMerchants()

    override suspend fun getLatestByMerchant(merchant: String): TransactionEntity? =
        delegate.getLatestByMerchant(merchant)

    override suspend fun getFixedTransactionsForMonth(yearMonth: Int): List<TransactionEntity> =
        delegate.getFixedTransactionsForMonth(yearMonth)

    override suspend fun getRefundsForTransaction(parentTxId: Long): List<TransactionEntity> =
        delegate.getRefundsForTransaction(parentTxId)

    override suspend fun getRecentExpensesSync(limit: Int): List<TransactionEntity> =
        delegate.getRecentExpensesSync(limit)

    override suspend fun insert(transaction: TransactionEntity): Long = delegate.insert(transaction)

    override suspend fun insertAll(transactions: List<TransactionEntity>) = delegate.insertAll(transactions)

    override suspend fun update(transaction: TransactionEntity) = delegate.update(transaction)

    override suspend fun delete(transaction: TransactionEntity) = delegate.delete(transaction)

    override suspend fun deleteById(id: Long) = delegate.deleteById(id)

    override suspend fun deleteAll() = delegate.deleteAll()

    override suspend fun count(): Int = delegate.count()
}

class FakeBudgetDao(initialBudgets: List<BudgetEntity> = emptyList()) : BudgetDao {

    private val budgets = MutableStateFlow(initialBudgets)

    override fun getAll(): Flow<List<BudgetEntity>> =
        budgets.map { list -> list.sortedBy { it.categoryId } }

    override fun getAllActive(): Flow<List<BudgetEntity>> =
        budgets.map { list -> list.filter { it.isActive }.sortedBy { it.categoryId } }

    override suspend fun getAllSync(): List<BudgetEntity> =
        budgets.value.sortedBy { it.categoryId }

    override suspend fun getById(id: String): BudgetEntity? =
        budgets.value.find { it.id == id }

    override suspend fun insert(budget: BudgetEntity) {
        val current = budgets.value.filterNot { it.id == budget.id }.toMutableList()
        current.add(budget)
        budgets.value = current
    }

    override suspend fun deleteById(id: String) {
        budgets.value = budgets.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        budgets.value = emptyList()
    }

    override suspend fun count(): Int = budgets.value.size
}

class FakeGoalDao(initialGoals: List<GoalEntity> = emptyList()) : GoalDao {

    private val goals = MutableStateFlow(initialGoals)

    override fun getAll(): Flow<List<GoalEntity>> =
        goals.map { list -> list.sortedWith(compareBy({ it.startYearMonth }, { it.name })) }

    override fun getActive(): Flow<List<GoalEntity>> =
        goals.map { list ->
            list.filter { !it.archived }
                .sortedWith(compareBy({ it.startYearMonth }, { it.name }))
        }

    override suspend fun getAllSync(): List<GoalEntity> =
        goals.value.sortedWith(compareBy({ it.startYearMonth }, { it.name }))

    override suspend fun getById(id: String): GoalEntity? =
        goals.value.find { it.id == id }

    override suspend fun insert(goal: GoalEntity) {
        val current = goals.value.filterNot { it.id == goal.id }.toMutableList()
        current.add(goal)
        goals.value = current
    }

    override suspend fun deleteById(id: String) {
        goals.value = goals.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        goals.value = emptyList()
    }

    override suspend fun count(): Int = goals.value.size
}

class FakeContextTagsStore(
    initialTags: Set<String> = emptySet()
) : ContextTagsStore {

    private val tags = initialTags.toMutableSet()

    override fun getCustomTags(): Set<String> = tags.toSet()

    override fun addTag(tag: String) {
        tags.add(tag)
    }
}

class FakePendingTransactionDao(
    initialPending: List<PendingTransactionEntity> = emptyList()
) : PendingTransactionDao {

    private val pending = MutableStateFlow(initialPending)
    private var nextId = (initialPending.maxOfOrNull { it.id } ?: 0L) + 1L

    override fun getAll(): Flow<List<PendingTransactionEntity>> =
        pending.map { list -> list.sortedByDescending { it.receivedTimestamp } }

    override suspend fun getAllSync(): List<PendingTransactionEntity> =
        pending.value.sortedByDescending { it.receivedTimestamp }

    override suspend fun getById(id: Long): PendingTransactionEntity? =
        pending.value.find { it.id == id }

    override suspend fun insert(pendingItem: PendingTransactionEntity): Long {
        val idToUse = if (pendingItem.id <= 0L) nextId++ else pendingItem.id
        val entity = pendingItem.copy(id = idToUse)
        val current = pending.value.filterNot { it.id == idToUse }.toMutableList()
        current.add(entity)
        pending.value = current
        return idToUse
    }

    override suspend fun insertAll(pendingItems: List<PendingTransactionEntity>) {
        for (pendingItem in pendingItems) {
            insert(pendingItem)
        }
    }

    override suspend fun update(pendingItem: PendingTransactionEntity) {
        val current = pending.value.filterNot { it.id == pendingItem.id }.toMutableList()
        current.add(pendingItem)
        pending.value = current
    }

    override suspend fun deleteById(id: Long) {
        pending.value = pending.value.filterNot { it.id == id }
    }

    override fun count(): Flow<Int> = pending.map { it.size }

    override suspend fun clearAll() {
        pending.value = emptyList()
    }
}

class FakeUserProfileStore(
    private var userName: String = "Utilizador Teste",
    private var photoUri: String? = null,
    private var currency: String = "EUR (€)",
    private var cycleDay: Int = 1,
    private var themeMode: String = "SYSTEM",
    private var appLanguage: String = "EN"
) : UserProfileStore {
    private val _userNameFlow = MutableStateFlow(userName)
    override val userNameFlow: StateFlow<String> = _userNameFlow.asStateFlow()

    private val _profilePhotoFlow = MutableStateFlow(photoUri)
    override val profilePhotoFlow: StateFlow<String?> = _profilePhotoFlow.asStateFlow()

    private val _themeModeFlow = MutableStateFlow(themeMode)
    override val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    private val _appLanguageFlow = MutableStateFlow(appLanguage)
    override val appLanguageFlow: StateFlow<String> = _appLanguageFlow.asStateFlow()

    override fun getUserName(): String = userName
    override fun setUserName(name: String) {
        userName = name
        _userNameFlow.value = name
    }
    override fun getProfilePhotoUri(): String? = photoUri
    override fun setProfilePhotoUri(uri: String?) {
        photoUri = uri
        _profilePhotoFlow.value = uri
    }
    override fun getDefaultCurrency(): String = currency
    override fun setDefaultCurrency(currency: String) { this.currency = currency }
    override fun getMonthlyCycleDay(): Int = cycleDay
    override fun setMonthlyCycleDay(day: Int) { cycleDay = day }
    override fun getThemeMode(): String = themeMode
    override fun setThemeMode(mode: String) {
        themeMode = mode
        _themeModeFlow.value = mode
    }
    override fun getAppLanguage(): String = appLanguage
    override fun setAppLanguage(lang: String) {
        appLanguage = lang.uppercase()
        _appLanguageFlow.value = appLanguage
    }
}

class FakeInvestmentAssetDao(
    initialAssets: List<InvestmentAssetEntity> = emptyList()
) : InvestmentAssetDao {

    private val assets = MutableStateFlow(initialAssets)

    override fun getAll(): Flow<List<InvestmentAssetEntity>> =
        assets.map { list -> list.sortedWith(compareByDescending<InvestmentAssetEntity> { it.currentValueCents }.thenBy { it.name }) }

    override fun getActive(): Flow<List<InvestmentAssetEntity>> =
        assets.map { list ->
            list.filter { !it.archived }
                .sortedWith(compareByDescending<InvestmentAssetEntity> { it.currentValueCents }.thenBy { it.name })
        }

    override suspend fun getAllSync(): List<InvestmentAssetEntity> =
        assets.value.sortedBy { it.name }

    override suspend fun getById(id: String): InvestmentAssetEntity? =
        assets.value.find { it.id == id }

    override suspend fun insert(asset: InvestmentAssetEntity) {
        val current = assets.value.filterNot { it.id == asset.id }.toMutableList()
        current.add(asset)
        assets.value = current
    }

    override suspend fun update(asset: InvestmentAssetEntity) {
        insert(asset)
    }

    override suspend fun updateCurrentValue(id: String, newValueCents: Long, epochDay: Long) {
        val current = assets.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = current[index].copy(
                currentValueCents = newValueCents,
                lastUpdatedEpochDay = epochDay
            )
            current[index] = updated
            assets.value = current
        }
    }

    override suspend fun deleteById(id: String) {
        assets.value = assets.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        assets.value = emptyList()
    }

    override suspend fun count(): Int = assets.value.size
}

