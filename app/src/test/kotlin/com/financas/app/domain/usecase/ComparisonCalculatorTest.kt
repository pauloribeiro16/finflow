package com.financas.app.domain.usecase

import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.seed.DefaultSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ComparisonCalculatorTest {

    private val categories = DefaultSeed.getDefaultCategories()
    private val calculator = ComparisonCalculator()

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
    fun calculatePercent_exactMathAndZeroBaselineGuard() {
        assertEquals(25.0, ComparisonCalculator.calculatePercent(50000L, 40000L)!!, 0.01)
        assertEquals(-50.0, ComparisonCalculator.calculatePercent(20000L, 40000L)!!, 0.01)
        assertEquals(0.0, ComparisonCalculator.calculatePercent(40000L, 40000L)!!, 0.01)
        assertNull(ComparisonCalculator.calculatePercent(40000L, 0L))
        assertNull(ComparisonCalculator.calculatePercent(40000L, -1L))
    }

    @Test
    fun calculateMonthComparison_totalsAndDeltas() {
        val current = listOf(
            tx(1, "INCOME", 200000L, 202610, "inc_base_salary"),
            tx(2, "EXPENSE", 80000L, 202610, "exp_food_supermarket")
        )
        val baseline = listOf(
            tx(3, "INCOME", 180000L, 202609, "inc_base_salary"),
            tx(4, "EXPENSE", 60000L, 202609, "exp_food_supermarket")
        )

        val result = calculator.calculateMonthComparison(current, baseline, categories)

        assertEquals(80000L, result.currentExpenseCents)
        assertEquals(60000L, result.baselineExpenseCents)
        assertEquals(20000L, result.expenseDeltaCents)
        assertEquals(33.33, result.expenseDeltaPercent!!, 0.01)

        assertEquals(200000L, result.currentIncomeCents)
        assertEquals(180000L, result.baselineIncomeCents)
        assertEquals(20000L, result.incomeDeltaCents)
        assertEquals(11.11, result.incomeDeltaPercent!!, 0.01)

        assertEquals(120000L, result.currentSavingsCents)
        assertEquals(120000L, result.baselineSavingsCents)
        assertEquals(0L, result.savingsDeltaCents)
        assertEquals(0.0, result.savingsDeltaPercent!!, 0.01)
    }

    @Test
    fun calculateCategoryDeltas_rollupSubcategoriesToParentAndSortDescending() {
        val current = listOf(
            tx(1, "EXPENSE", 30000L, 202610, "exp_food_supermarket"),
            tx(2, "EXPENSE", 10000L, 202610, "exp_food_restaurant"),
            tx(3, "EXPENSE", 5000L, 202610, "exp_unknown_custom")
        )
        val baseline = listOf(
            tx(4, "EXPENSE", 20000L, 202609, "exp_food_supermarket"),
            tx(5, "EXPENSE", 25000L, 202609, "exp_housing_rent")
        )

        val deltas = calculator.calculateCategoryDeltas(current, baseline, categories)

        assertEquals(3, deltas.size)

        val food = deltas[0]
        assertEquals("exp_food", food.categoryId)
        assertEquals("Alimentação", food.categoryName)
        assertEquals(40000L, food.currentCents)
        assertEquals(20000L, food.baselineCents)
        assertEquals(20000L, food.deltaCents)
        assertEquals(100.0, food.deltaPercent!!, 0.01)

        val unknown = deltas[1]
        assertEquals("exp_unknown_custom", unknown.categoryId)
        assertEquals(0xFF888888, unknown.colorHex)
        assertEquals(5000L, unknown.currentCents)
        assertEquals(0L, unknown.baselineCents)
        assertEquals(5000L, unknown.deltaCents)
        assertNull(unknown.deltaPercent)

        val housing = deltas[2]
        assertEquals("exp_housing", housing.categoryId)
        assertEquals(0L, housing.currentCents)
        assertEquals(25000L, housing.baselineCents)
        assertEquals(-25000L, housing.deltaCents)
        assertEquals(-100.0, housing.deltaPercent!!, 0.01)
    }

    @Test
    fun calculateCategoryDeltas_ignoresIncomeTransactions() {
        val current = listOf(
            tx(1, "INCOME", 200000L, 202610, "inc_base_salary"),
            tx(2, "EXPENSE", 30000L, 202610, "exp_food_supermarket")
        )
        val baseline = listOf(
            tx(3, "INCOME", 180000L, 202609, "inc_base_salary"),
            tx(4, "INCOME", 50000L, 202609, "inc_bonus")
        )

        val deltas = calculator.calculateCategoryDeltas(current, baseline, categories)

        assertEquals(1, deltas.size)
        assertEquals("exp_food", deltas[0].categoryId)
        assertEquals(30000L, deltas[0].currentCents)
        assertEquals(0L, deltas[0].baselineCents)
    }

    @Test
    fun emptyTransactionLists_yieldZeroTotalsNullPercentsAndNoDeltas() {
        val comparison = calculator.calculateMonthComparison(emptyList(), emptyList(), categories)

        assertEquals(0L, comparison.currentExpenseCents)
        assertEquals(0L, comparison.baselineExpenseCents)
        assertEquals(0L, comparison.currentIncomeCents)
        assertEquals(0L, comparison.baselineIncomeCents)
        assertEquals(0L, comparison.currentSavingsCents)
        assertEquals(0L, comparison.baselineSavingsCents)
        assertEquals(0L, comparison.expenseDeltaCents)
        assertEquals(0L, comparison.incomeDeltaCents)
        assertEquals(0L, comparison.savingsDeltaCents)
        assertNull(comparison.expenseDeltaPercent)
        assertNull(comparison.incomeDeltaPercent)
        assertNull(comparison.savingsDeltaPercent)
        assertTrue(comparison.categoryDeltas.isEmpty())

        assertTrue(calculator.calculateCategoryDeltas(emptyList(), emptyList(), categories).isEmpty())
    }
}
