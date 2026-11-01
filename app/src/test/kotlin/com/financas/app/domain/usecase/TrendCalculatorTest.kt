package com.financas.app.domain.usecase

import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.seed.DefaultSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TrendCalculatorTest {

    private val categories = DefaultSeed.getDefaultCategories()
    private val calculator = TrendCalculator()

    private fun tx(
        id: Long,
        type: String,
        amountCents: Long,
        yearMonth: Int,
        categoryId: String
    ): TransactionEntity = TransactionEntity(
        id = id,
        type = type,
        amountCents = amountCents,
        yearMonth = yearMonth,
        epochDay = LocalDate.of(yearMonth / 100, yearMonth % 100, 10).toEpochDay(),
        categoryId = categoryId,
        contextTag = "CASA",
        merchant = "Merchant",
        paymentMethod = "CARD"
    )

    @Test
    fun calculateCategoryTrend_zeroFills12SlotsAndAveragesPositiveMonthsOnly() {
        val transactions = listOf(
            tx(1, "EXPENSE", 10000L, 202601, "exp_food_supermarket"),
            tx(2, "EXPENSE", 20000L, 202603, "exp_food"),
            tx(3, "EXPENSE", 5000L, 202604, "exp_transport_fuel"),
            tx(4, "EXPENSE", 9999L, 202501, "exp_food_supermarket"),
            tx(5, "INCOME", 100000L, 202602, "inc_base_salary")
        )

        val trend = calculator.calculateCategoryTrend("exp_food", 2026, transactions, categories)

        assertEquals("exp_food", trend.categoryId)
        assertEquals("Alimentação", trend.categoryName)
        assertEquals(12, trend.points.size)
        assertEquals(10000L, trend.points[0].expenseCents)
        assertEquals(202601, trend.points[0].yearMonth)
        assertEquals(0L, trend.points[1].expenseCents)
        assertEquals(20000L, trend.points[2].expenseCents)
        assertEquals(0L, trend.points[11].expenseCents)
        assertEquals(202612, trend.points[11].yearMonth)
        assertEquals(30000L, trend.totalExpenseCents)
        assertEquals(15000L, trend.averageExpenseCents)
    }

    @Test
    fun calculateCategoryTrend_usesCategoryColorWithGrayFallbackForUnknown() {
        val transactions = listOf(
            tx(1, "EXPENSE", 10000L, 202601, "exp_food_supermarket")
        )

        val known = calculator.calculateCategoryTrend("exp_food", 2026, transactions, categories)
        assertEquals(0xFFFF9800, known.colorHex)

        val unknown = calculator.calculateCategoryTrend("exp_unknown_parent", 2026, transactions, categories)
        assertEquals(0xFF888888, unknown.colorHex)
        assertEquals("exp_unknown_parent", unknown.categoryName)
        assertEquals(0L, unknown.totalExpenseCents)
        assertEquals(0L, unknown.averageExpenseCents)
    }

    @Test
    fun calculateSavingsRateSeries_twelvePointsWithCorrectRatesAndActivityFlags() {
        val transactions = listOf(
            tx(1, "INCOME", 100000L, 202601, "inc_base_salary"),
            tx(2, "EXPENSE", 40000L, 202601, "exp_food_supermarket"),
            tx(3, "EXPENSE", 20000L, 202602, "exp_food_supermarket")
        )

        val series = calculator.calculateSavingsRateSeries(2026, transactions)

        assertEquals(12, series.size)

        val january = series[0]
        assertEquals(202601, january.yearMonth)
        assertEquals(60.0, january.savingsRatePercent, 0.01)
        assertTrue(january.hasActivity)

        val february = series[1]
        assertEquals(0.0, february.savingsRatePercent, 0.01)
        assertTrue(february.hasActivity)

        val march = series[2]
        assertEquals(0.0, march.savingsRatePercent, 0.01)
        assertFalse(march.hasActivity)
        assertFalse(series[11].hasActivity)
    }

    @Test
    fun calculateSavingsRateAverage_ignoresInactiveMonths() {
        val transactions = listOf(
            tx(1, "INCOME", 100000L, 202601, "inc_base_salary"),
            tx(2, "EXPENSE", 40000L, 202601, "exp_food_supermarket"),
            tx(3, "INCOME", 80000L, 202602, "inc_base_salary"),
            tx(4, "EXPENSE", 80000L, 202602, "exp_food_supermarket")
        )

        val series = calculator.calculateSavingsRateSeries(2026, transactions)
        val average = calculator.calculateSavingsRateAverage(series)

        assertEquals(30.0, average, 0.01)
        assertEquals(0.0, calculator.calculateSavingsRateAverage(emptyList()), 0.01)
    }

    @Test
    fun calculateYearAggregates_groupsYearsAscendingWithExactTotalsAndTopCategories() {
        val transactions = listOf(
            tx(1, "INCOME", 300000L, 202501, "inc_base_salary"),
            tx(2, "EXPENSE", 50000L, 202501, "exp_food_supermarket"),
            tx(3, "EXPENSE", 30000L, 202503, "exp_housing_rent"),
            tx(4, "INCOME", 240000L, 202602, "inc_base_salary"),
            tx(5, "EXPENSE", 60000L, 202602, "exp_housing_rent"),
            tx(6, "EXPENSE", 20000L, 202602, "exp_food_supermarket"),
            tx(7, "EXPENSE", 10000L, 202605, "exp_transport_fuel")
        )

        val aggregates = calculator.calculateYearAggregates(transactions, categories)

        assertEquals(2, aggregates.size)

        val year2025 = aggregates[0]
        assertEquals(2025, year2025.year)
        assertEquals(300000L, year2025.incomeCents)
        assertEquals(80000L, year2025.expenseCents)
        assertEquals(220000L, year2025.netSavingsCents)
        assertEquals(73.33, year2025.savingsRatePercent, 0.01)
        assertEquals(12, year2025.monthlyExpenseCents.size)
        assertEquals(50000L, year2025.monthlyExpenseCents[0])
        assertEquals(0L, year2025.monthlyExpenseCents[1])
        assertEquals(30000L, year2025.monthlyExpenseCents[2])
        assertEquals(listOf("Alimentação", "Habitação & Contas"), year2025.topCategoryNames)

        val year2026 = aggregates[1]
        assertEquals(2026, year2026.year)
        assertEquals(240000L, year2026.incomeCents)
        assertEquals(90000L, year2026.expenseCents)
        assertEquals(150000L, year2026.netSavingsCents)
        assertEquals(62.5, year2026.savingsRatePercent, 0.01)
        assertEquals(80000L, year2026.monthlyExpenseCents[1])
        assertEquals(0L, year2026.monthlyExpenseCents[0])
        assertEquals(listOf("Habitação & Contas", "Alimentação", "Transportes"), year2026.topCategoryNames)
    }

    @Test
    fun emptyInputs_yieldZeroFilledSeriesAndNoAggregates() {
        val trend = calculator.calculateCategoryTrend("exp_food", 2026, emptyList(), categories)

        assertEquals(12, trend.points.size)
        assertTrue(trend.points.all { it.expenseCents == 0L })
        assertEquals(0L, trend.totalExpenseCents)
        assertEquals(0L, trend.averageExpenseCents)

        val emptySeries = calculator.calculateSavingsRateSeries(2026, emptyList())
        assertEquals(12, emptySeries.size)
        assertTrue(emptySeries.none { it.hasActivity })
        assertEquals(0.0, calculator.calculateSavingsRateAverage(emptySeries), 0.01)

        assertTrue(calculator.calculateYearAggregates(emptyList(), categories).isEmpty())
    }
}
