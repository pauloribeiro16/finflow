package com.financas.app.domain.usecase

import com.financas.app.data.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class MonthProjectionCalculatorTest {

    private val calculator = MonthProjectionCalculator()

    private fun tx(
        id: Long,
        type: String,
        amountCents: Long,
        yearMonth: Int,
        day: Int
    ) = TransactionEntity(
        id = id,
        type = type,
        amountCents = amountCents,
        yearMonth = yearMonth,
        epochDay = LocalDate.of(yearMonth / 100, yearMonth % 100, day).toEpochDay(),
        categoryId = if (type == "INCOME") "inc_base_salary" else "exp_food_lunch",
        contextTag = if (type == "INCOME") "TRABALHO" else "TRABALHO",
        merchant = if (type == "INCOME") "Salário" else "Café Central",
        paymentMethod = "TRANSFER",
        isFixed = true
    )

    @Test
    fun criterion4_currentMonth_exactDailyAverageProjectionAndSavingsRate() {
        val yearMonth = 202610
        val today = LocalDate.of(2026, 10, 10).toEpochDay()

        val transactions = listOf(
            tx(1, "INCOME", 200000L, yearMonth, 1),
            tx(2, "EXPENSE", 50000L, yearMonth, 3),
            tx(3, "EXPENSE", 30000L, yearMonth, 8)
        )

        val projection = calculator.calculate(transactions, yearMonth, today)

        assertEquals(yearMonth, projection.yearMonth)
        assertEquals(10, projection.daysElapsed)
        assertEquals(31, projection.daysInMonth)
        assertEquals(8000L, projection.dailyAverageExpenseCents)
        assertEquals(248000L, projection.projectedMonthEndExpenseCents)
        assertEquals(-48000L, projection.projectedRemainingCents)
        assertEquals(-24.0, projection.projectedSavingsRatePercent, 0.001)
    }

    @Test
    fun criterion4_emptyMonth_zerosWithoutExceptionOrNaN() {
        val yearMonth = 202611
        val today = LocalDate.of(2026, 11, 15).toEpochDay()

        val projection = calculator.calculate(emptyList(), yearMonth, today)

        assertEquals(15, projection.daysElapsed)
        assertEquals(30, projection.daysInMonth)
        assertEquals(0L, projection.dailyAverageExpenseCents)
        assertEquals(0L, projection.projectedMonthEndExpenseCents)
        assertEquals(0L, projection.projectedRemainingCents)
        assertEquals(0.0, projection.projectedSavingsRatePercent, 0.0)
        assertFalse(projection.projectedSavingsRatePercent.isNaN())
    }

    @Test
    fun criterion4_pastMonth_projectionEqualsActualTotal() {
        val yearMonth = 202609
        val today = LocalDate.of(2026, 10, 26).toEpochDay()

        val transactions = listOf(
            tx(1, "INCOME", 150000L, yearMonth, 1),
            tx(2, "EXPENSE", 20000L, yearMonth, 4),
            tx(3, "EXPENSE", 30000L, yearMonth, 12),
            tx(4, "EXPENSE", 10000L, yearMonth, 28)
        )

        val projection = calculator.calculate(transactions, yearMonth, today)

        assertEquals(30, projection.daysElapsed)
        assertEquals(30, projection.daysInMonth)
        assertEquals(2000L, projection.dailyAverageExpenseCents)
        assertEquals(60000L, projection.projectedMonthEndExpenseCents)
        assertEquals(90000L, projection.projectedRemainingCents)
        assertEquals(60.0, projection.projectedSavingsRatePercent, 0.001)
    }

    @Test
    fun criterion4_dayOne_averageOverSingleDay() {
        val yearMonth = 202610
        val today = LocalDate.of(2026, 10, 1).toEpochDay()

        val transactions = listOf(
            tx(1, "INCOME", 100000L, yearMonth, 1),
            tx(2, "EXPENSE", 20000L, yearMonth, 1)
        )

        val projection = calculator.calculate(transactions, yearMonth, today)

        assertEquals(1, projection.daysElapsed)
        assertEquals(31, projection.daysInMonth)
        assertEquals(20000L, projection.dailyAverageExpenseCents)
        assertEquals(620000L, projection.projectedMonthEndExpenseCents)
        assertEquals(-520000L, projection.projectedRemainingCents)
        assertEquals(-520.0, projection.projectedSavingsRatePercent, 0.001)
    }

    @Test
    fun criterion4_withoutIncome_savingsRateIsZeroWithoutNaN() {
        val yearMonth = 202610
        val today = LocalDate.of(2026, 10, 10).toEpochDay()

        val transactions = listOf(
            tx(1, "EXPENSE", 50000L, yearMonth, 5)
        )

        val projection = calculator.calculate(transactions, yearMonth, today)

        assertEquals(5000L, projection.dailyAverageExpenseCents)
        assertEquals(155000L, projection.projectedMonthEndExpenseCents)
        assertEquals(-155000L, projection.projectedRemainingCents)
        assertEquals(0.0, projection.projectedSavingsRatePercent, 0.0)
        assertFalse(projection.projectedSavingsRatePercent.isNaN())
    }

    @Test
    fun criterion4_futureMonth_fullProjectionEqualsActualTotal() {
        val yearMonth = 202612
        val today = LocalDate.of(2026, 10, 26).toEpochDay()

        val transactions = listOf(
            tx(1, "EXPENSE", 10000L, yearMonth, 5)
        )

        val projection = calculator.calculate(transactions, yearMonth, today)

        assertEquals(31, projection.daysElapsed)
        assertEquals(31, projection.daysInMonth)
        assertEquals(322L, projection.dailyAverageExpenseCents)
        assertEquals(10000L, projection.projectedMonthEndExpenseCents)
        assertEquals(-10000L, projection.projectedRemainingCents)
        assertEquals(0.0, projection.projectedSavingsRatePercent, 0.0)
    }

    @Test
    fun criterion10_lastDayOfTheCurrentMonth_ProjectsTheActualTotal() {
        val yearMonth = 202610
        val today = LocalDate.of(2026, 10, 31).toEpochDay()

        val transactions = listOf(
            tx(1, "INCOME", 200000L, yearMonth, 1),
            tx(2, "EXPENSE", 50000L, yearMonth, 12),
            tx(3, "EXPENSE", 30000L, yearMonth, 31)
        )

        val projection = calculator.calculate(transactions, yearMonth, today)

        assertEquals(31, projection.daysElapsed)
        assertEquals(31, projection.daysInMonth)
        assertEquals(2580L, projection.dailyAverageExpenseCents)
        assertEquals(80000L, projection.projectedMonthEndExpenseCents)
        assertEquals(120000L, projection.projectedRemainingCents)
        assertEquals(60.0, projection.projectedSavingsRatePercent, 0.001)
    }

    @Test
    fun criterion10_leapFebruaryHas29DaysAndNonLeapFebruaryHas28() {
        val leap = calculator.calculate(
            listOf(tx(1, "EXPENSE", 10000L, 202402, 3)),
            202402,
            LocalDate.of(2024, 2, 15).toEpochDay()
        )
        assertEquals(29, leap.daysInMonth)
        assertEquals(15, leap.daysElapsed)

        val nonLeap = calculator.calculate(
            listOf(tx(1, "EXPENSE", 10000L, 202602, 3)),
            202602,
            LocalDate.of(2026, 2, 15).toEpochDay()
        )
        assertEquals(28, nonLeap.daysInMonth)
        assertEquals(15, nonLeap.daysElapsed)
    }

    @Test
    fun criterion10_dailyAverageIsTruncatedToWholeCents() {
        val yearMonth = 202610
        val today = LocalDate.of(2026, 10, 3).toEpochDay()

        val transactions = listOf(
            tx(1, "EXPENSE", 10000L, yearMonth, 1)
        )

        val projection = calculator.calculate(transactions, yearMonth, today)

        assertEquals(3, projection.daysElapsed)
        assertEquals(3333L, projection.dailyAverageExpenseCents)
        assertEquals(103323L, projection.projectedMonthEndExpenseCents)
    }

    @Test
    fun criterion10_pastMonthCountsEveryDayAsElapsedEvenWhenTodayIsLater() {
        val yearMonth = 202609
        val today = LocalDate.of(2026, 11, 15).toEpochDay()

        val transactions = listOf(
            tx(1, "INCOME", 150000L, yearMonth, 1),
            tx(2, "EXPENSE", 60000L, yearMonth, 28)
        )

        val projection = calculator.calculate(transactions, yearMonth, today)

        assertEquals(30, projection.daysElapsed)
        assertEquals(30, projection.daysInMonth)
        assertEquals(2000L, projection.dailyAverageExpenseCents)
        assertEquals(60000L, projection.projectedMonthEndExpenseCents)
        assertEquals(90000L, projection.projectedRemainingCents)
        assertEquals(60.0, projection.projectedSavingsRatePercent, 0.001)
    }

    @Test
    fun criterion10_emptyMonthWithZeroIncomeGivesZeroProjectionWithoutNaN() {
        val projection = calculator.calculate(
            emptyList(),
            202610,
            LocalDate.of(2026, 10, 10).toEpochDay()
        )

        assertEquals(0L, projection.dailyAverageExpenseCents)
        assertEquals(0L, projection.projectedMonthEndExpenseCents)
        assertEquals(0L, projection.projectedRemainingCents)
        assertEquals(0.0, projection.projectedSavingsRatePercent, 0.0)
        assertFalse(projection.projectedSavingsRatePercent.isNaN())
    }

    @Test
    fun criterion10_projectedSavingsRateIsNegativeWhenExpensesProjectAboveIncome() {
        val yearMonth = 202610
        val today = LocalDate.of(2026, 10, 10).toEpochDay()

        val transactions = listOf(
            tx(1, "INCOME", 100000L, yearMonth, 1),
            tx(2, "EXPENSE", 50000L, yearMonth, 5)
        )

        val projection = calculator.calculate(transactions, yearMonth, today)

        assertEquals(5000L, projection.dailyAverageExpenseCents)
        assertEquals(155000L, projection.projectedMonthEndExpenseCents)
        assertEquals(-55000L, projection.projectedRemainingCents)
        assertEquals(-55.0, projection.projectedSavingsRatePercent, 0.001)
    }

    @Test
    fun criterion10_onlyIncomeInTheMonth_GivesFullSavingsProjection() {
        val yearMonth = 202610
        val today = LocalDate.of(2026, 10, 10).toEpochDay()

        val transactions = listOf(
            tx(1, "INCOME", 100000L, yearMonth, 1)
        )

        val projection = calculator.calculate(transactions, yearMonth, today)

        assertEquals(0L, projection.dailyAverageExpenseCents)
        assertEquals(0L, projection.projectedMonthEndExpenseCents)
        assertEquals(100000L, projection.projectedRemainingCents)
        assertEquals(100.0, projection.projectedSavingsRatePercent, 0.001)
    }
}
