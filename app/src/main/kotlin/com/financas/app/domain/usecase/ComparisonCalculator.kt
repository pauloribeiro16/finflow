package com.financas.app.domain.usecase

import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.TransactionEntity

data class CategoryDelta(
    val categoryId: String,
    val categoryName: String,
    val colorHex: Long,
    val currentCents: Long,
    val baselineCents: Long,
    val deltaCents: Long,
    val deltaPercent: Double?
)

data class MonthComparison(
    val currentExpenseCents: Long,
    val baselineExpenseCents: Long,
    val currentIncomeCents: Long,
    val baselineIncomeCents: Long,
    val currentSavingsCents: Long,
    val baselineSavingsCents: Long,
    val expenseDeltaCents: Long,
    val expenseDeltaPercent: Double?,
    val incomeDeltaCents: Long,
    val incomeDeltaPercent: Double?,
    val savingsDeltaCents: Long,
    val savingsDeltaPercent: Double?,
    val categoryDeltas: List<CategoryDelta>
)

class ComparisonCalculator {

    companion object {
        fun calculatePercent(currentCents: Long, baselineCents: Long): Double? {
            if (baselineCents <= 0L) return null
            return ((currentCents - baselineCents).toDouble() / baselineCents.toDouble()) * 100.0
        }
    }

    fun calculateCategoryDeltas(
        currentTransactions: List<TransactionEntity>,
        baselineTransactions: List<TransactionEntity>,
        categories: List<CategoryEntity>
    ): List<CategoryDelta> {
        val categoryMap = categories.associateBy { it.id }
        val currentByParent = sumByParent(currentTransactions, categoryMap)
        val baselineByParent = sumByParent(baselineTransactions, categoryMap)
        val parentIds = currentByParent.keys + baselineByParent.keys

        return parentIds.map { parentId ->
            val parent = resolveParent(parentId, categoryMap)
            val currentCents = currentByParent[parentId] ?: 0L
            val baselineCents = baselineByParent[parentId] ?: 0L
            CategoryDelta(
                categoryId = parent.id,
                categoryName = parent.name,
                colorHex = parent.colorHex,
                currentCents = currentCents,
                baselineCents = baselineCents,
                deltaCents = currentCents - baselineCents,
                deltaPercent = calculatePercent(currentCents, baselineCents)
            )
        }.sortedByDescending { it.deltaCents }
    }

    fun calculateMonthComparison(
        currentTransactions: List<TransactionEntity>,
        baselineTransactions: List<TransactionEntity>,
        categories: List<CategoryEntity>
    ): MonthComparison {
        val currentIncome = sumByType(currentTransactions, "INCOME")
        val currentExpense = sumByType(currentTransactions, "EXPENSE")
        val baselineIncome = sumByType(baselineTransactions, "INCOME")
        val baselineExpense = sumByType(baselineTransactions, "EXPENSE")
        val currentSavings = currentIncome - currentExpense
        val baselineSavings = baselineIncome - baselineExpense

        return MonthComparison(
            currentExpenseCents = currentExpense,
            baselineExpenseCents = baselineExpense,
            currentIncomeCents = currentIncome,
            baselineIncomeCents = baselineIncome,
            currentSavingsCents = currentSavings,
            baselineSavingsCents = baselineSavings,
            expenseDeltaCents = currentExpense - baselineExpense,
            expenseDeltaPercent = calculatePercent(currentExpense, baselineExpense),
            incomeDeltaCents = currentIncome - baselineIncome,
            incomeDeltaPercent = calculatePercent(currentIncome, baselineIncome),
            savingsDeltaCents = currentSavings - baselineSavings,
            savingsDeltaPercent = calculatePercent(currentSavings, baselineSavings),
            categoryDeltas = calculateCategoryDeltas(currentTransactions, baselineTransactions, categories)
        )
    }

    private fun sumByType(transactions: List<TransactionEntity>, type: String): Long =
        transactions.filter { it.type.equals(type, ignoreCase = true) }.sumOf { it.amountCents }

    private fun sumByParent(
        transactions: List<TransactionEntity>,
        categoryMap: Map<String, CategoryEntity>
    ): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        for (tx in transactions) {
            if (!tx.type.equals("EXPENSE", ignoreCase = true)) continue
            val parentId = when (val cat = categoryMap[tx.categoryId]) {
                null -> tx.categoryId
                else -> cat.parentId ?: cat.id
            }
            result[parentId] = (result[parentId] ?: 0L) + tx.amountCents
        }
        return result
    }

    private fun resolveParent(categoryId: String, categoryMap: Map<String, CategoryEntity>): CategoryEntity {
        val cat = categoryMap[categoryId] ?: return CategoryEntity(
            id = categoryId,
            parentId = null,
            name = categoryId,
            icon = "category",
            colorHex = 0xFF888888,
            type = "EXPENSE"
        )
        if (cat.parentId == null) return cat
        return categoryMap[cat.parentId] ?: cat
    }
}
