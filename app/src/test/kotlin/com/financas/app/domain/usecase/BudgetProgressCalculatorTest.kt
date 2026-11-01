package com.financas.app.domain.usecase

import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BudgetProgressCalculatorTest {

    private val calculator = BudgetProgressCalculator()

    private val categories = listOf(
        CategoryEntity("exp_food", null, "Alimentação", "restaurant", 0xFFFF9800, "EXPENSE"),
        CategoryEntity("exp_food_supermarket", "exp_food", "Supermercado", "shopping_cart", 0xFFFF9800, "EXPENSE"),
        CategoryEntity("exp_food_lunch", "exp_food", "Almoços / Trabalho", "lunch_dining", 0xFFFFA726, "EXPENSE"),
        CategoryEntity("exp_food_restaurant", "exp_food", "Restaurantes", "dinner_dining", 0xFFFFB74D, "EXPENSE"),
        CategoryEntity("exp_housing", null, "Habitação & Contas", "home", 0xFF1976D2, "EXPENSE"),
        CategoryEntity("exp_transport", null, "Transportes", "directions_car", 0xFF009688, "EXPENSE"),
        CategoryEntity("exp_health", null, "Saúde & Bem-Estar", "favorite", 0xFFE91E63, "EXPENSE")
    )

    private fun expense(
        id: Long,
        amountCents: Long,
        categoryId: String,
        yearMonth: Int = 202610,
        epochDay: Long = LocalDate.of(2026, 10, 15).toEpochDay()
    ) = TransactionEntity(
        id = id,
        type = "EXPENSE",
        amountCents = amountCents,
        yearMonth = yearMonth,
        epochDay = epochDay,
        categoryId = categoryId,
        contextTag = "CASA",
        merchant = "Loja",
        paymentMethod = "CARD",
        isFixed = false
    )

    private fun income(id: Long, amountCents: Long) = TransactionEntity(
        id = id,
        type = "INCOME",
        amountCents = amountCents,
        yearMonth = 202610,
        epochDay = LocalDate.of(2026, 10, 1).toEpochDay(),
        categoryId = "inc_base_salary",
        contextTag = "TRABALHO",
        merchant = "Salário",
        paymentMethod = "TRANSFER",
        isFixed = true
    )

    @Test
    fun criterion2_parentBudgetSumsSubcategoryExpenses_exactCents() {
        val transactions = listOf(
            income(1, 150000L),
            expense(2, 12345L, "exp_food_supermarket"),
            expense(3, 7655L, "exp_food_lunch"),
            expense(4, 5000L, "exp_food_restaurant"),
            expense(5, 1000L, "exp_food")
        )

        val report = calculator.calculate(
            budgets = listOf(
                BudgetEntity("budget_exp_food", "exp_food", 50000L),
                BudgetEntity("budget_exp_food_supermarket", "exp_food_supermarket", 10000L)
            ),
            monthTransactions = transactions,
            categories = categories
        )

        assertEquals(2, report.items.size)

        val subBudget = report.items[0]
        assertEquals("budget_exp_food_supermarket", subBudget.budgetId)
        assertEquals(12345L, subBudget.spentCents)
        assertEquals(10000L, subBudget.limitCents)
        assertEquals(-2345L, subBudget.remainingCents)
        assertEquals(123.45, subBudget.percentUsed, 0.001)
        assertEquals(BudgetStatus.EXCEEDED, subBudget.status)

        val parentBudget = report.items[1]
        assertEquals("budget_exp_food", parentBudget.budgetId)
        assertEquals("Alimentação", parentBudget.categoryName)
        assertEquals(26000L, parentBudget.spentCents)
        assertEquals(24000L, parentBudget.remainingCents)
        assertEquals(52.0, parentBudget.percentUsed, 0.001)
        assertEquals(BudgetStatus.OK, parentBudget.status)

        assertEquals(60000L, report.totalLimitCents)
        assertEquals(26000L, report.totalSpentCents)
        assertEquals(34000L, report.totalRemainingCents)
    }

    @Test
    fun criterion2_aggregateCountsSharedTransactionsOnceAcrossParentAndSubBudgets() {
        val transactions = listOf(
            income(1, 150000L),
            expense(2, 12345L, "exp_food_supermarket"),
            expense(3, 7655L, "exp_food_lunch"),
            expense(4, 5000L, "exp_housing"),
            expense(5, 2500L, "exp_transport")
        )

        val report = calculator.calculate(
            budgets = listOf(
                BudgetEntity("budget_exp_food", "exp_food", 50000L),
                BudgetEntity("budget_exp_food_supermarket", "exp_food_supermarket", 10000L),
                BudgetEntity("budget_exp_housing", "exp_housing", 20000L),
                BudgetEntity("budget_exp_transport", "exp_transport", 30000L)
            ),
            monthTransactions = transactions,
            categories = categories
        )

        assertEquals(4, report.items.size)
        assertEquals(20000L, report.items.first { it.budgetId == "budget_exp_food" }.spentCents)
        assertEquals(12345L, report.items.first { it.budgetId == "budget_exp_food_supermarket" }.spentCents)
        assertEquals(5000L, report.items.first { it.budgetId == "budget_exp_housing" }.spentCents)
        assertEquals(2500L, report.items.first { it.budgetId == "budget_exp_transport" }.spentCents)

        assertEquals(110000L, report.totalLimitCents)
        assertEquals(27500L, report.totalSpentCents)
        assertEquals(82500L, report.totalRemainingCents)
    }

    @Test
    fun criterion2_aggregateSumsBudgetsOnDisjointCategories() {
        val transactions = listOf(
            expense(1, 7990L, "exp_housing"),
            expense(2, 10010L, "exp_transport")
        )

        val report = calculator.calculate(
            budgets = listOf(
                BudgetEntity("budget_exp_housing", "exp_housing", 10000L),
                BudgetEntity("budget_exp_transport", "exp_transport", 10000L)
            ),
            monthTransactions = transactions,
            categories = categories
        )

        assertEquals(20000L, report.totalLimitCents)
        assertEquals(18000L, report.totalSpentCents)
        assertEquals(2000L, report.totalRemainingCents)
    }

    @Test
    fun criterion2_aggregateWithoutIdUsesFieldIdentity() {
        val day1 = LocalDate.of(2026, 10, 1).toEpochDay()
        val day5 = LocalDate.of(2026, 10, 5).toEpochDay()

        val transactions = listOf(
            expense(0L, 5000L, "exp_food_supermarket", epochDay = day1),
            expense(0L, 5000L, "exp_food_supermarket", epochDay = day1),
            expense(0L, 5000L, "exp_food_supermarket", epochDay = day5)
        )

        val report = calculator.calculate(
            budgets = listOf(
                BudgetEntity("budget_exp_food_supermarket", "exp_food_supermarket", 10000L)
            ),
            monthTransactions = transactions,
            categories = categories
        )

        assertEquals(15000L, report.items[0].spentCents)
        assertEquals(10000L, report.totalLimitCents)
        assertEquals(10000L, report.totalSpentCents)
        assertEquals(0L, report.totalRemainingCents)
    }

    @Test
    fun criterion2_statusBoundaries_79_9ok_80warning_100warning_100_1exceeded_orderedDesc() {
        val transactions = listOf(
            expense(1, 7990L, "exp_housing"),
            expense(2, 8000L, "exp_food"),
            expense(3, 10000L, "exp_transport"),
            expense(4, 10010L, "exp_health")
        )

        val report = calculator.calculate(
            budgets = listOf(
                BudgetEntity("budget_exp_housing", "exp_housing", 10000L),
                BudgetEntity("budget_exp_food", "exp_food", 10000L),
                BudgetEntity("budget_exp_transport", "exp_transport", 10000L),
                BudgetEntity("budget_exp_health", "exp_health", 10000L)
            ),
            monthTransactions = transactions,
            categories = categories
        )

        assertEquals(4, report.items.size)
        assertEquals(
            listOf("budget_exp_health", "budget_exp_transport", "budget_exp_food", "budget_exp_housing"),
            report.items.map { it.budgetId }
        )
        assertEquals(
            listOf(
                BudgetStatus.EXCEEDED,
                BudgetStatus.WARNING,
                BudgetStatus.WARNING,
                BudgetStatus.OK
            ),
            report.items.map { it.status }
        )
        assertEquals(100.1, report.items[0].percentUsed, 0.001)
        assertEquals(100.0, report.items[1].percentUsed, 0.001)
        assertEquals(80.0, report.items[2].percentUsed, 0.001)
        assertEquals(79.9, report.items[3].percentUsed, 0.001)
    }

    @Test
    fun criterion2_unknownCategory_spendsZeroAndKeepsBudget() {
        val transactions = listOf(
            expense(1, 5000L, "exp_food_supermarket")
        )

        val report = calculator.calculate(
            budgets = listOf(
                BudgetEntity("budget_exp_unknown", "exp_unknown_xyz", 20000L)
            ),
            monthTransactions = transactions,
            categories = categories
        )

        assertEquals(1, report.items.size)
        val item = report.items[0]
        assertEquals(0L, item.spentCents)
        assertEquals(20000L, item.remainingCents)
        assertEquals(0.0, item.percentUsed, 0.0)
        assertEquals(BudgetStatus.OK, item.status)
        assertEquals("exp_unknown_xyz", item.categoryName)
        assertEquals(20000L, report.totalLimitCents)
        assertEquals(0L, report.totalSpentCents)
        assertEquals(20000L, report.totalRemainingCents)
    }

    @Test
    fun criterion2_emptyMonth_budgetsReportedWithZeroSpend() {
        val report = calculator.calculate(
            budgets = listOf(
                BudgetEntity("budget_exp_food", "exp_food", 30000L)
            ),
            monthTransactions = listOf(income(1, 100000L)),
            categories = categories
        )

        assertEquals(1, report.items.size)
        assertEquals(0L, report.items[0].spentCents)
        assertEquals(0.0, report.items[0].percentUsed, 0.0)
        assertEquals(BudgetStatus.OK, report.items[0].status)
        assertEquals(30000L, report.totalLimitCents)
        assertEquals(0L, report.totalSpentCents)
        assertEquals(30000L, report.totalRemainingCents)
    }

    @Test
    fun criterion2_emptyBudgets_returnsEmptyReportWithoutExpensesProcessed() {
        val report = calculator.calculate(
            budgets = emptyList(),
            monthTransactions = listOf(expense(1, 5000L, "exp_food")),
            categories = categories
        )

        assertTrue(report.items.isEmpty())
        assertEquals(0L, report.totalLimitCents)
        assertEquals(0L, report.totalSpentCents)
        assertEquals(0L, report.totalRemainingCents)
    }

    @Test
    fun criterion10_zeroLimit_keepsTheBudgetWithZeroPercentAndNegativeRemaining() {
        val report = calculator.calculate(
            budgets = listOf(
                BudgetEntity("budget_exp_food", "exp_food", 0L)
            ),
            monthTransactions = listOf(expense(1, 5000L, "exp_food_supermarket")),
            categories = categories
        )

        assertEquals(1, report.items.size)
        val item = report.items[0]
        assertEquals(5000L, item.spentCents)
        assertEquals(0L, item.limitCents)
        assertEquals(-5000L, item.remainingCents)
        assertEquals(0.0, item.percentUsed, 0.0)
        assertEquals(BudgetStatus.OK, item.status)
        assertEquals(0L, report.totalLimitCents)
        assertEquals(5000L, report.totalSpentCents)
        assertEquals(-5000L, report.totalRemainingCents)
    }

    @Test
    fun criterion10_negativeLimit_isTreatedAsZeroPercent() {
        val report = calculator.calculate(
            budgets = listOf(
                BudgetEntity("budget_exp_housing", "exp_housing", -100L)
            ),
            monthTransactions = listOf(expense(1, 2500L, "exp_housing")),
            categories = categories
        )

        val item = report.items[0]
        assertEquals(2500L, item.spentCents)
        assertEquals(-100L, item.limitCents)
        assertEquals(-2600L, item.remainingCents)
        assertEquals(0.0, item.percentUsed, 0.0)
        assertEquals(BudgetStatus.OK, item.status)
        assertEquals(-100L, report.totalLimitCents)
        assertEquals(2500L, report.totalSpentCents)
        assertEquals(-2600L, report.totalRemainingCents)
    }

    @Test
    fun criterion10_parentBudgetRollsUpOwnAndAllSubcategorySpend() {
        val report = calculator.calculate(
            budgets = listOf(
                BudgetEntity("budget_exp_food", "exp_food", 10000L)
            ),
            monthTransactions = listOf(
                expense(1, 1000L, "exp_food"),
                expense(2, 2000L, "exp_food_supermarket"),
                expense(3, 3000L, "exp_food_lunch")
            ),
            categories = categories
        )

        assertEquals(6000L, report.items[0].spentCents)
        assertEquals(4000L, report.items[0].remainingCents)
        assertEquals(60.0, report.items[0].percentUsed, 0.001)
        assertEquals(BudgetStatus.OK, report.items[0].status)
    }

    @Test
    fun criterion10_subcategoryBudgetDoesNotIncludeSiblingSpend() {
        val report = calculator.calculate(
            budgets = listOf(
                BudgetEntity("budget_exp_food_supermarket", "exp_food_supermarket", 10000L)
            ),
            monthTransactions = listOf(
                expense(1, 2000L, "exp_food_supermarket"),
                expense(2, 3000L, "exp_food_lunch"),
                expense(3, 1000L, "exp_food")
            ),
            categories = categories
        )

        assertEquals(2000L, report.items[0].spentCents)
        assertEquals(8000L, report.items[0].remainingCents)
        assertEquals(20.0, report.items[0].percentUsed, 0.001)
        assertEquals(2000L, report.totalSpentCents)
    }

    @Test
    fun criterion10_aggregateCountsZeroIdTransactionsOnceEvenWithParentAndSubBudgets() {
        val day1 = LocalDate.of(2026, 10, 1).toEpochDay()

        val report = calculator.calculate(
            budgets = listOf(
                BudgetEntity("budget_exp_food", "exp_food", 50000L),
                BudgetEntity("budget_exp_food_supermarket", "exp_food_supermarket", 10000L)
            ),
            monthTransactions = listOf(
                expense(0L, 5000L, "exp_food_supermarket", epochDay = day1),
                expense(0L, 5000L, "exp_food_supermarket", epochDay = day1)
            ),
            categories = categories
        )

        assertEquals(10000L, report.items.first { it.budgetId == "budget_exp_food" }.spentCents)
        assertEquals(10000L, report.items.first { it.budgetId == "budget_exp_food_supermarket" }.spentCents)
        assertEquals(5000L, report.totalSpentCents)
        assertEquals(55000L, report.totalRemainingCents)
    }

    @Test
    fun criterion10_aggregateCountsSameFieldTransactionsWithDifferentIdsTwice() {
        val day1 = LocalDate.of(2026, 10, 1).toEpochDay()

        val report = calculator.calculate(
            budgets = listOf(
                BudgetEntity("budget_exp_food_supermarket", "exp_food_supermarket", 10000L)
            ),
            monthTransactions = listOf(
                expense(11L, 5000L, "exp_food_supermarket", epochDay = day1),
                expense(12L, 5000L, "exp_food_supermarket", epochDay = day1)
            ),
            categories = categories
        )

        assertEquals(10000L, report.items[0].spentCents)
        assertEquals(10000L, report.totalSpentCents)
        assertEquals(0L, report.totalRemainingCents)
    }

    @Test
    fun criterion10_equalPercentBudgetsKeepTheInsertionOrder() {
        val report = calculator.calculate(
            budgets = listOf(
                BudgetEntity("budget_exp_housing", "exp_housing", 10000L),
                BudgetEntity("budget_exp_food", "exp_food", 10000L),
                BudgetEntity("budget_exp_transport", "exp_transport", 10000L)
            ),
            monthTransactions = listOf(
                expense(1, 5000L, "exp_housing"),
                expense(2, 5000L, "exp_food"),
                expense(3, 5000L, "exp_transport")
            ),
            categories = categories
        )

        assertEquals(50.0, report.items[0].percentUsed, 0.001)
        assertEquals(
            listOf("budget_exp_housing", "budget_exp_food", "budget_exp_transport"),
            report.items.map { it.budgetId }
        )
    }

    @Test
    fun criterion10_incomeTransactionsAreNeverCountedAsSpend() {
        val report = calculator.calculate(
            budgets = listOf(
                BudgetEntity("budget_exp_food", "exp_food", 10000L)
            ),
            monthTransactions = listOf(
                income(1, 150000L),
                expense(2, 3000L, "exp_food_supermarket")
            ),
            categories = categories
        )

        assertEquals(3000L, report.items[0].spentCents)
        assertEquals(3000L, report.totalSpentCents)
        assertEquals(7000L, report.totalRemainingCents)
    }

    @Test
    fun criterion10_budgetOnUnknownCategoryCountsInTheLimitButNotInTheSpend() {
        val report = calculator.calculate(
            budgets = listOf(
                BudgetEntity("budget_exp_unknown", "exp_unknown_xyz", 20000L),
                BudgetEntity("budget_exp_housing", "exp_housing", 10000L)
            ),
            monthTransactions = listOf(
                expense(1, 5000L, "exp_housing")
            ),
            categories = categories
        )

        assertEquals(30000L, report.totalLimitCents)
        assertEquals(5000L, report.totalSpentCents)
        assertEquals(25000L, report.totalRemainingCents)
    }
}
