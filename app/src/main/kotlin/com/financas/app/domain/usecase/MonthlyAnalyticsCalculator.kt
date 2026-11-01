package com.financas.app.domain.usecase

import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.seed.DefaultSeed
import com.financas.app.domain.model.CategoryShare
import com.financas.app.domain.model.ContextShare
import com.financas.app.domain.model.MerchantShare
import com.financas.app.domain.model.MethodShare
import com.financas.app.domain.model.MonthlySummary

class MonthlyAnalyticsCalculator {

    companion object {
        fun calculateIncomeShare(partCents: Long, totalIncomeCents: Long): Double {
            if (totalIncomeCents <= 0L || partCents <= 0L) return 0.0
            return (partCents.toDouble() / totalIncomeCents.toDouble()) * 100.0
        }

        fun calculateExpenseShare(partCents: Long, totalExpenseCents: Long): Double {
            if (totalExpenseCents <= 0L || partCents <= 0L) return 0.0
            return (partCents.toDouble() / totalExpenseCents.toDouble()) * 100.0
        }

        fun calculateSavingsRate(netSavingsCents: Long, totalIncomeCents: Long): Double {
            if (totalIncomeCents <= 0L) return 0.0
            return (netSavingsCents.toDouble() / totalIncomeCents.toDouble()) * 100.0
        }

        fun calculateHistoricalAverage(monthlyExpenses: List<Long>): Long {
            if (monthlyExpenses.isEmpty()) return 0L
            val positiveExpenses = monthlyExpenses.filter { it > 0L }
            if (positiveExpenses.isEmpty()) return 0L
            return positiveExpenses.sum() / positiveExpenses.size
        }

        fun calculateDeviationVsAverage(currentExpenseCents: Long, historicalAverageCents: Long): Double? {
            if (historicalAverageCents <= 0L) return null
            return ((currentExpenseCents.toDouble() - historicalAverageCents.toDouble()) / historicalAverageCents.toDouble()) * 100.0
        }
    }

    fun calculateMonthlySummary(
        yearMonth: Int,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity> = DefaultSeed.getDefaultCategories(),
        historicalMonthlyExpenses: List<Long> = emptyList()
    ): MonthlySummary {
        val incomeTxs = NetExpenseCalculator.filterRegularIncomes(transactions)
        val expenseTxs = NetExpenseCalculator.resolveEffectiveExpenses(transactions)

        val totalIncomeCents = incomeTxs.sumOf { it.amountCents }
        val baseIncomeCents = incomeTxs.filter {
            it.isFixed || it.categoryId in setOf("inc_fixed", "inc_base_salary", "inc_meal_allowance")
        }.sumOf { it.amountCents }
        val extraIncomeCents = totalIncomeCents - baseIncomeCents

        val totalExpenseCents = expenseTxs.sumOf { it.amountCents }
        val fixedExpenseCents = expenseTxs.filter { it.isFixed }.sumOf { it.amountCents }
        val variableExpenseCents = totalExpenseCents - fixedExpenseCents

        val netSavingsCents = totalIncomeCents - totalExpenseCents

        val savingsRatePercent = calculateSavingsRate(netSavingsCents, totalIncomeCents)
        val expenseIncomeRatioPercent = calculateIncomeShare(totalExpenseCents, totalIncomeCents)
        val fixedExpenseIncomeRatioPercent = calculateIncomeShare(fixedExpenseCents, totalIncomeCents)
        val variableExpenseIncomeRatioPercent = calculateIncomeShare(variableExpenseCents, totalIncomeCents)

        val historicalAverage = calculateHistoricalAverage(historicalMonthlyExpenses)
        val deviationVsAveragePercent = calculateDeviationVsAverage(totalExpenseCents, historicalAverage)

        val categoryShares = rollupCategories(expenseTxs, categories, totalIncomeCents, totalExpenseCents)
        val contextShares = calculateContextShares(expenseTxs, totalIncomeCents, totalExpenseCents)
        val topMerchants = calculateMerchantShares(expenseTxs, totalIncomeCents, totalExpenseCents)
        val methodShares = calculateMethodShares(expenseTxs, totalIncomeCents, totalExpenseCents)

        return MonthlySummary(
            yearMonth = yearMonth,
            totalIncomeCents = totalIncomeCents,
            baseIncomeCents = baseIncomeCents,
            extraIncomeCents = extraIncomeCents,
            totalExpenseCents = totalExpenseCents,
            fixedExpenseCents = fixedExpenseCents,
            variableExpenseCents = variableExpenseCents,
            netSavingsCents = netSavingsCents,
            savingsRatePercent = savingsRatePercent,
            expenseIncomeRatioPercent = expenseIncomeRatioPercent,
            fixedExpenseIncomeRatioPercent = fixedExpenseIncomeRatioPercent,
            variableExpenseIncomeRatioPercent = variableExpenseIncomeRatioPercent,
            deviationVsAveragePercent = deviationVsAveragePercent,
            categoryShares = categoryShares,
            contextShares = contextShares,
            topMerchants = topMerchants,
            methodShares = methodShares
        )
    }

    fun rollupCategories(
        expenseTransactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        totalIncomeCents: Long,
        totalExpenseCents: Long
    ): List<CategoryShare> {
        if (expenseTransactions.isEmpty()) return emptyList()

        val categoryMap = categories.associateBy { it.id }

        // Group transactions by their resolved parent category and subcategory
        data class ResolvedTx(
            val tx: TransactionEntity,
            val parentCat: CategoryEntity,
            val subCat: CategoryEntity?
        )

        val resolvedList = expenseTransactions.map { tx ->
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

        val byParent = resolvedList.groupBy { it.parentCat }

        return byParent.map { (parent, items) ->
            val parentAmountCents = items.sumOf { it.tx.amountCents }
            val parentPctIncome = calculateIncomeShare(parentAmountCents, totalIncomeCents)
            val parentPctExpense = calculateExpenseShare(parentAmountCents, totalExpenseCents)

            val subcategories = items
                .filter { it.subCat != null }
                .groupBy { it.subCat!! }
                .map { (subCat, subItems) ->
                    val subAmountCents = subItems.sumOf { it.tx.amountCents }
                    CategoryShare(
                        categoryId = subCat.id,
                        categoryName = subCat.name,
                        parentCategoryId = parent.id,
                        parentCategoryName = parent.name,
                        colorHex = subCat.colorHex,
                        icon = subCat.icon,
                        amountCents = subAmountCents,
                        percentageOfIncome = calculateIncomeShare(subAmountCents, totalIncomeCents),
                        percentageOfExpense = calculateExpenseShare(subAmountCents, totalExpenseCents),
                        transactionCount = subItems.size,
                        subcategories = emptyList()
                    )
                }
                .sortedByDescending { it.amountCents }

            CategoryShare(
                categoryId = parent.id,
                categoryName = parent.name,
                parentCategoryId = null,
                parentCategoryName = null,
                colorHex = parent.colorHex,
                icon = parent.icon,
                amountCents = parentAmountCents,
                percentageOfIncome = parentPctIncome,
                percentageOfExpense = parentPctExpense,
                transactionCount = items.size,
                subcategories = subcategories
            )
        }.sortedByDescending { it.amountCents }
    }

    fun calculateContextShares(
        expenseTransactions: List<TransactionEntity>,
        totalIncomeCents: Long,
        totalExpenseCents: Long
    ): List<ContextShare> {
        if (expenseTransactions.isEmpty()) return emptyList()

        val contextLabelMap = DefaultSeed.CONTEXTS.associate { it.tag to it.label }

        return expenseTransactions
            .groupBy { it.contextTag }
            .map { (tag, txs) ->
                val amountCents = txs.sumOf { it.amountCents }
                val label = contextLabelMap[tag] ?: tag
                ContextShare(
                    contextTag = tag,
                    contextLabel = label,
                    amountCents = amountCents,
                    percentageOfIncome = calculateIncomeShare(amountCents, totalIncomeCents),
                    percentageOfExpense = calculateExpenseShare(amountCents, totalExpenseCents),
                    transactionCount = txs.size
                )
            }
            .sortedByDescending { it.amountCents }
    }

    fun calculateMerchantShares(
        expenseTransactions: List<TransactionEntity>,
        totalIncomeCents: Long,
        totalExpenseCents: Long
    ): List<MerchantShare> {
        if (expenseTransactions.isEmpty()) return emptyList()

        return expenseTransactions
            .filter { it.merchant.isNotBlank() }
            .groupBy { it.merchant.trim() }
            .map { (merchant, txs) ->
                val amountCents = txs.sumOf { it.amountCents }
                val count = txs.size
                val avgCents = if (count > 0) amountCents / count else 0L
                MerchantShare(
                    merchant = merchant,
                    amountCents = amountCents,
                    percentageOfIncome = calculateIncomeShare(amountCents, totalIncomeCents),
                    percentageOfExpense = calculateExpenseShare(amountCents, totalExpenseCents),
                    transactionCount = count,
                    averagePerVisitCents = avgCents
                )
            }
            .sortedByDescending { it.amountCents }
    }

    fun calculateMethodShares(
        expenseTransactions: List<TransactionEntity>,
        totalIncomeCents: Long,
        totalExpenseCents: Long
    ): List<MethodShare> {
        if (expenseTransactions.isEmpty()) return emptyList()

        val methodLabelMap = buildMap {
            putAll(DefaultSeed.PAYMENT_METHODS.associate { it.code to it.label })
            put("DEBITO", "Cartão")
            put("DINHEIRO", "Dinheiro")
            put("TRANSFERENCIA", "Transferência")
            put("DEBITO_DIRETO", "Débito Direto")
        }

        return expenseTransactions
            .groupBy {
                when (it.paymentMethod.uppercase()) {
                    "DEBITO" -> "CARD"
                    "DINHEIRO" -> "CASH"
                    "TRANSFERENCIA" -> "TRANSFER"
                    "DEBITO_DIRETO" -> "DIRECT_DEBIT"
                    else -> it.paymentMethod.uppercase()
                }
            }
            .map { (method, txs) ->
                val amountCents = txs.sumOf { it.amountCents }
                val label = methodLabelMap[method] ?: method
                MethodShare(
                    paymentMethod = method,
                    methodLabel = label,
                    amountCents = amountCents,
                    percentageOfIncome = calculateIncomeShare(amountCents, totalIncomeCents),
                    percentageOfExpense = calculateExpenseShare(amountCents, totalExpenseCents),
                    transactionCount = txs.size
                )
            }
            .sortedByDescending { it.amountCents }
    }
}
