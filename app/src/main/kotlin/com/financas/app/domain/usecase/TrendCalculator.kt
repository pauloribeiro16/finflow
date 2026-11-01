package com.financas.app.domain.usecase

import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.TransactionEntity

data class CategoryMonthlyPoint(
    val yearMonth: Int,
    val expenseCents: Long
)

data class CategoryTrend(
    val categoryId: String,
    val categoryName: String,
    val colorHex: Long,
    val points: List<CategoryMonthlyPoint>,
    val averageExpenseCents: Long,
    val totalExpenseCents: Long
)

data class SavingsRatePoint(
    val yearMonth: Int,
    val savingsRatePercent: Double,
    val hasActivity: Boolean
)

data class YearAggregate(
    val year: Int,
    val incomeCents: Long,
    val expenseCents: Long,
    val netSavingsCents: Long,
    val savingsRatePercent: Double,
    val monthlyExpenseCents: List<Long>,
    val topCategoryNames: List<String>
)

class TrendCalculator {

    fun calculateCategoryTrend(
        parentCategoryId: String,
        year: Int,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>
    ): CategoryTrend {
        val categoryMap = categories.associateBy { it.id }
        val monthlyExpenses = MutableList(12) { 0L }

        for (tx in transactions) {
            if (!tx.type.equals("EXPENSE", ignoreCase = true)) continue
            if (tx.yearMonth / 100 != year) continue
            val parentId = when (val cat = categoryMap[tx.categoryId]) {
                null -> tx.categoryId
                else -> cat.parentId ?: cat.id
            }
            if (parentId != parentCategoryId) continue
            val month = tx.yearMonth % 100
            if (month in 1..12) {
                monthlyExpenses[month - 1] += tx.amountCents
            }
        }

        val points = (1..12).map { month ->
            CategoryMonthlyPoint(yearMonth = year * 100 + month, expenseCents = monthlyExpenses[month - 1])
        }
        val positiveMonths = monthlyExpenses.filter { it > 0L }
        val averageExpenseCents = if (positiveMonths.isEmpty()) 0L else positiveMonths.sum() / positiveMonths.size
        val category = categoryMap[parentCategoryId]

        return CategoryTrend(
            categoryId = parentCategoryId,
            categoryName = category?.name ?: parentCategoryId,
            colorHex = category?.colorHex ?: 0xFF888888,
            points = points,
            averageExpenseCents = averageExpenseCents,
            totalExpenseCents = monthlyExpenses.sum()
        )
    }

    fun calculateSavingsRateSeries(
        year: Int,
        transactions: List<TransactionEntity>
    ): List<SavingsRatePoint> {
        return (1..12).map { month ->
            val yearMonth = year * 100 + month
            val monthTxs = transactions.filter { it.yearMonth == yearMonth }
            val income = monthTxs.filter { it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amountCents }
            val expense = monthTxs.filter { it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amountCents }
            SavingsRatePoint(
                yearMonth = yearMonth,
                savingsRatePercent = MonthlyAnalyticsCalculator.calculateSavingsRate(income - expense, income),
                hasActivity = income > 0L || expense > 0L
            )
        }
    }

    fun calculateSavingsRateAverage(points: List<SavingsRatePoint>): Double {
        val activePoints = points.filter { it.hasActivity }
        if (activePoints.isEmpty()) return 0.0
        return activePoints.sumOf { it.savingsRatePercent } / activePoints.size
    }

    fun calculateYearAggregates(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>
    ): List<YearAggregate> {
        if (transactions.isEmpty()) return emptyList()
        val categoryMap = categories.associateBy { it.id }
        val byYear = transactions.groupBy { it.yearMonth / 100 }.toSortedMap()

        return byYear.map { (year, yearTxs) ->
            val incomeTxs = yearTxs.filter { it.type.equals("INCOME", ignoreCase = true) }
            val expenseTxs = yearTxs.filter { it.type.equals("EXPENSE", ignoreCase = true) }
            val incomeCents = incomeTxs.sumOf { it.amountCents }
            val expenseCents = expenseTxs.sumOf { it.amountCents }
            val netSavingsCents = incomeCents - expenseCents

            val monthlyExpenseCents = MutableList(12) { 0L }
            val expenseByParent = mutableMapOf<String, Long>()
            for (tx in expenseTxs) {
                val parentId = when (val cat = categoryMap[tx.categoryId]) {
                    null -> tx.categoryId
                    else -> cat.parentId ?: cat.id
                }
                val month = tx.yearMonth % 100
                if (month in 1..12) {
                    monthlyExpenseCents[month - 1] += tx.amountCents
                }
                expenseByParent[parentId] = (expenseByParent[parentId] ?: 0L) + tx.amountCents
            }
            val topCategoryNames = expenseByParent.entries
                .sortedByDescending { it.value }
                .take(3)
                .map { (parentId, _) -> categoryMap[parentId]?.name ?: parentId }

            YearAggregate(
                year = year,
                incomeCents = incomeCents,
                expenseCents = expenseCents,
                netSavingsCents = netSavingsCents,
                savingsRatePercent = MonthlyAnalyticsCalculator.calculateSavingsRate(netSavingsCents, incomeCents),
                monthlyExpenseCents = monthlyExpenseCents,
                topCategoryNames = topCategoryNames
            )
        }
    }
}
