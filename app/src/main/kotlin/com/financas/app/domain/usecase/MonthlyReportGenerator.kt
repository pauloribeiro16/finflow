package com.financas.app.domain.usecase

import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.seed.DefaultSeed
import com.financas.app.domain.model.CategoryShare
import com.financas.app.domain.model.ContextShare
import com.financas.app.domain.model.MerchantShare
import com.financas.app.domain.model.MoneyCents
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Gera o relatório textual mensal partilhável (PT-PT).
 *
 * A saída é determinística: ordenações estáveis por valor decrescente e, em
 * caso de empate, por nome crescente. Os blocos de orçamentos e de metas só
 * são emitidos quando existem dados para os sustentar.
 */
class MonthlyReportGenerator(
    private val monthlyAnalyticsCalculator: MonthlyAnalyticsCalculator = MonthlyAnalyticsCalculator()
) {

    fun generate(
        yearMonth: Int,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity> = emptyList(),
        budgetProgress: BudgetProgressReport? = null,
        goalProgress: List<GoalProgress>? = null,
        topCategoriesLimit: Int = 3,
        topContextsLimit: Int = 3,
        topMerchantsLimit: Int = 5
    ): String {
        val effectiveCategories = categories.ifEmpty { DefaultSeed.getDefaultCategories() }
        val summary = monthlyAnalyticsCalculator.calculateMonthlySummary(
            yearMonth = yearMonth,
            transactions = transactions,
            categories = effectiveCategories
        )

        val lines = mutableListOf<String>()
        lines += "Relatório Financeiro — ${formatMonthTitle(yearMonth)}"
        lines += "Total recebido: ${MoneyCents.formatCents(summary.totalIncomeCents)}"
        lines += "Total gasto: ${MoneyCents.formatCents(summary.totalExpenseCents)}"
        lines += "Poupança líquida: ${MoneyCents.formatCents(summary.netSavingsCents)}"
        lines += "Taxa de poupança: ${MoneyCents.formatPercent(summary.savingsRatePercent)}"
        lines += "% do gasto sobre o rendimento: ${MoneyCents.formatPercent(summary.expenseIncomeRatioPercent)}"

        lines += ""
        lines += "Top categorias:"
        val topCategories = summary.categoryShares
            .sortedWith(compareByDescending<CategoryShare> { it.amountCents }.thenBy { it.categoryName })
            .take(topCategoriesLimit)
        if (topCategories.isEmpty()) {
            lines += "Sem dados."
        } else {
            topCategories.forEachIndexed { index, share ->
                lines += "${index + 1}. ${share.categoryName} — ${MoneyCents.formatCents(share.amountCents)} (${MoneyCents.formatPercent(share.percentageOfExpense)} dos gastos)"
            }
        }

        lines += ""
        lines += "Top contextos:"
        val topContexts = summary.contextShares
            .sortedWith(compareByDescending<ContextShare> { it.amountCents }.thenBy { it.contextLabel })
            .take(topContextsLimit)
        if (topContexts.isEmpty()) {
            lines += "Sem dados."
        } else {
            topContexts.forEachIndexed { index, share ->
                lines += "${index + 1}. ${share.contextLabel} — ${MoneyCents.formatCents(share.amountCents)} (${MoneyCents.formatPercent(share.percentageOfExpense)} dos gastos)"
            }
        }

        lines += ""
        lines += "Top estabelecimentos:"
        val topMerchants = summary.topMerchants
            .sortedWith(compareByDescending<MerchantShare> { it.amountCents }.thenBy { it.merchant })
            .take(topMerchantsLimit)
        if (topMerchants.isEmpty()) {
            lines += "Sem dados."
        } else {
            topMerchants.forEachIndexed { index, share ->
                lines += "${index + 1}. ${share.merchant} — ${MoneyCents.formatCents(share.amountCents)} (${movementCountLabel(share.transactionCount)})"
            }
        }

        val budgets = budgetProgress?.items.orEmpty()
        if (budgets.isNotEmpty()) {
            lines += ""
            lines += "Orçamentos:"
            budgets.forEach { item ->
                lines += "- ${item.categoryName}: ${MoneyCents.formatCents(item.spentCents)} de ${MoneyCents.formatCents(item.limitCents)} (${MoneyCents.formatPercent(item.percentUsed)}) — ${budgetStatusLabel(item.status)}"
            }
        }

        val goals = goalProgress.orEmpty()
        if (goals.isNotEmpty()) {
            lines += ""
            lines += "Metas:"
            goals.forEach { item ->
                lines += "- ${item.name}: ${MoneyCents.formatCents(item.accumulatedSavingsCents)} de ${MoneyCents.formatCents(item.targetCents)} (${MoneyCents.formatPercent(item.percentComplete)})"
            }
        }

        return lines.joinToString("\n")
    }

    private fun budgetStatusLabel(status: BudgetStatus): String = when (status) {
        BudgetStatus.OK -> "OK"
        BudgetStatus.WARNING -> "Atenção"
        BudgetStatus.EXCEEDED -> "Excedido"
    }

    private fun movementCountLabel(count: Int): String =
        if (count == 1) "1 movimento" else "$count movimentos"

    companion object {
        fun formatMonthTitle(yearMonth: Int): String {
            val ym = YearMonth.of(yearMonth / 100, yearMonth % 100)
            val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("pt", "PT"))
            val formatted = ym.format(formatter)
            return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "PT")) else it.toString() }
        }
    }
}
