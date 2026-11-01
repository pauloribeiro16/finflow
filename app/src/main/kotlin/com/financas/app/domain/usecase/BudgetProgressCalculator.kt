package com.financas.app.domain.usecase

import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.TransactionEntity

enum class BudgetStatus { OK, WARNING, EXCEEDED }

data class BudgetProgress(
    val budgetId: String,
    val categoryId: String,
    val categoryName: String,
    val colorHex: Long,
    val icon: String,
    val spentCents: Long,
    val limitCents: Long,
    val remainingCents: Long,
    val percentUsed: Double,
    val status: BudgetStatus
)

data class BudgetProgressReport(
    val items: List<BudgetProgress> = emptyList(),
    val totalLimitCents: Long = 0L,
    val totalSpentCents: Long = 0L,
    val totalRemainingCents: Long = 0L
)

class BudgetProgressCalculator {

    fun calculate(
        budgets: List<BudgetEntity>,
        monthTransactions: List<TransactionEntity>,
        categories: List<CategoryEntity>
    ): BudgetProgressReport {
        if (budgets.isEmpty()) return BudgetProgressReport()

        val categoryMap = categories.associateBy { it.id }
        val subcategoriesByParent = categories.filter { it.parentId != null }.groupBy { it.parentId!! }
        val expenseTransactions = monthTransactions.filter { it.type.equals("EXPENSE", ignoreCase = true) }
        val spentByCategoryId = expenseTransactions
            .groupBy { it.categoryId }
            .mapValues { (_, txs) -> txs.sumOf { it.amountCents } }

        fun spentFor(categoryId: String): Long {
            val category = categoryMap[categoryId] ?: return 0L
            val own = spentByCategoryId[categoryId] ?: 0L
            val subs = subcategoriesByParent[categoryId]
                ?.sumOf { sub -> spentByCategoryId[sub.id] ?: 0L }
                ?: 0L
            return own + subs
        }

        val items = budgets.map { budget ->
            val category = categoryMap[budget.categoryId]
            val spent = spentFor(budget.categoryId)
            val percentUsed = if (budget.limitCents <= 0L) 0.0
            else (spent.toDouble() / budget.limitCents.toDouble()) * 100.0

            BudgetProgress(
                budgetId = budget.id,
                categoryId = budget.categoryId,
                categoryName = category?.name ?: budget.categoryId,
                colorHex = category?.colorHex ?: 0xFF888888,
                icon = category?.icon ?: "category",
                spentCents = spent,
                limitCents = budget.limitCents,
                remainingCents = budget.limitCents - spent,
                percentUsed = percentUsed,
                status = resolveStatus(percentUsed)
            )
        }.sortedByDescending { it.percentUsed }

        val totalLimit = items.sumOf { it.limitCents }
        val totalSpent = distinctCoveredSpend(
            expenseTransactions = expenseTransactions,
            budgets = budgets,
            categoryMap = categoryMap,
            subcategoriesByParent = subcategoriesByParent
        )

        return BudgetProgressReport(
            items = items,
            totalLimitCents = totalLimit,
            totalSpentCents = totalSpent,
            totalRemainingCents = totalLimit - totalSpent
        )
    }

    private fun distinctCoveredSpend(
        expenseTransactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        categoryMap: Map<String, CategoryEntity>,
        subcategoriesByParent: Map<String, List<CategoryEntity>>
    ): Long {
        val coveredCategoryIds = hashSetOf<String>()
        budgets.forEach { budget ->
            if (!categoryMap.containsKey(budget.categoryId)) return@forEach
            coveredCategoryIds.add(budget.categoryId)
            subcategoriesByParent[budget.categoryId]?.forEach { sub -> coveredCategoryIds.add(sub.id) }
        }
        if (coveredCategoryIds.isEmpty()) return 0L

        val seenIdentities = hashSetOf<String>()
        var total = 0L
        expenseTransactions.forEach { tx ->
            if (tx.categoryId !in coveredCategoryIds) return@forEach
            if (seenIdentities.add(identityOf(tx))) total += tx.amountCents
        }
        return total
    }

    private fun identityOf(tx: TransactionEntity): String =
        if (tx.id > 0L) {
            "id:${tx.id}"
        } else {
            "tx:${tx.type}:${tx.amountCents}:${tx.epochDay}:${tx.categoryId}:${tx.merchant}"
        }

    private fun resolveStatus(percentUsed: Double): BudgetStatus = when {
        percentUsed > 100.0 -> BudgetStatus.EXCEEDED
        percentUsed >= 80.0 -> BudgetStatus.WARNING
        else -> BudgetStatus.OK
    }
}
