package com.financas.app.domain.model

data class MonthlySummary(
    val yearMonth: Int,
    val totalIncomeCents: Long,
    val baseIncomeCents: Long,
    val extraIncomeCents: Long,
    val totalExpenseCents: Long,
    val fixedExpenseCents: Long,
    val variableExpenseCents: Long,
    val netSavingsCents: Long,
    val savingsRatePercent: Double,
    val expenseIncomeRatioPercent: Double,
    val fixedExpenseIncomeRatioPercent: Double,
    val variableExpenseIncomeRatioPercent: Double,
    val deviationVsAveragePercent: Double? = null,
    val categoryShares: List<CategoryShare> = emptyList(),
    val contextShares: List<ContextShare> = emptyList(),
    val topMerchants: List<MerchantShare> = emptyList(),
    val methodShares: List<MethodShare> = emptyList()
)

data class CategoryShare(
    val categoryId: String,
    val categoryName: String,
    val parentCategoryId: String? = null,
    val parentCategoryName: String? = null,
    val colorHex: Long,
    val icon: String,
    val amountCents: Long,
    val percentageOfIncome: Double,
    val percentageOfExpense: Double,
    val transactionCount: Int,
    val subcategories: List<CategoryShare> = emptyList()
)

data class ContextShare(
    val contextTag: String,
    val contextLabel: String,
    val amountCents: Long,
    val percentageOfIncome: Double,
    val percentageOfExpense: Double,
    val transactionCount: Int
)

data class MerchantShare(
    val merchant: String,
    val amountCents: Long,
    val percentageOfIncome: Double,
    val percentageOfExpense: Double,
    val transactionCount: Int,
    val averagePerVisitCents: Long
)

data class MethodShare(
    val paymentMethod: String,
    val methodLabel: String,
    val amountCents: Long,
    val percentageOfIncome: Double,
    val percentageOfExpense: Double,
    val transactionCount: Int
)
