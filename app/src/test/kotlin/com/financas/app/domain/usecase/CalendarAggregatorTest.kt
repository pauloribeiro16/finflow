package com.financas.app.domain.usecase

import com.financas.app.data.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalendarAggregatorTest {

    private val aggregator = CalendarAggregator()

    private fun tx(
        id: Long,
        type: String,
        amountCents: Long,
        epochDay: Long,
        yearMonth: Int = 202610
    ): TransactionEntity = TransactionEntity(
        id = id,
        type = type,
        amountCents = amountCents,
        yearMonth = yearMonth,
        epochDay = epochDay,
        categoryId = "exp_food_supermarket",
        contextTag = "CASA",
        merchant = "Continente",
        paymentMethod = "CARD",
        isFixed = false
    )

    @Test
    fun aggregatesExactTotalsAndCountsPerDay() {
        val oct10 = LocalDate.of(2026, 10, 10).toEpochDay()
        val oct12 = LocalDate.of(2026, 10, 12).toEpochDay()

        val days = aggregator.aggregate(
            yearMonth = 202610,
            transactions = listOf(
                tx(1, "EXPENSE", 5000L, oct10),
                tx(2, "EXPENSE", 2500L, oct10),
                tx(3, "EXPENSE", 12000L, oct12)
            ),
            todayEpochDay = oct12
        )

        assertEquals(31, days.size)

        val day10 = days[9]
        assertEquals(10, day10.dayOfMonth)
        assertEquals(oct10, day10.epochDay)
        assertEquals(7500L, day10.expenseCents)
        assertEquals(2, day10.transactionCount)

        val day12 = days[11]
        assertEquals(12, day12.dayOfMonth)
        assertEquals(oct12, day12.epochDay)
        assertEquals(12000L, day12.expenseCents)
        assertEquals(1, day12.transactionCount)
    }

    @Test
    fun incomeIsNotCountedAsSpending() {
        val oct10 = LocalDate.of(2026, 10, 10).toEpochDay()

        val days = aggregator.aggregate(
            yearMonth = 202610,
            transactions = listOf(
                tx(1, "INCOME", 200000L, oct10),
                tx(2, "EXPENSE", 5000L, oct10)
            ),
            todayEpochDay = oct10
        )

        val day10 = days[9]
        assertEquals(5000L, day10.expenseCents)
        assertEquals(1, day10.transactionCount)
    }

    @Test
    fun daysWithoutExpensesAreIncludedWithZeros() {
        val oct10 = LocalDate.of(2026, 10, 10).toEpochDay()

        val days = aggregator.aggregate(
            yearMonth = 202610,
            transactions = listOf(tx(1, "EXPENSE", 5000L, oct10)),
            todayEpochDay = oct10
        )

        assertEquals(31, days.size)
        assertEquals(30, days.count { it.expenseCents == 0L })
        val emptyDay = days.first { it.dayOfMonth == 1 }
        assertEquals(0L, emptyDay.expenseCents)
        assertEquals(0, emptyDay.transactionCount)
        assertEquals(5000L, days.sumOf { it.expenseCents })
    }

    @Test
    fun monthWithoutTransactionsReturnsZeroFilledDays() {
        val days = aggregator.aggregate(yearMonth = 202610, transactions = emptyList())

        assertEquals(31, days.size)
        assertTrue(days.all { it.expenseCents == 0L })
        assertTrue(days.all { it.transactionCount == 0 })
        assertEquals(0L, days.sumOf { it.expenseCents })
    }

    @Test
    fun daysAreSortedByEpochDayAndCoverEveryDayOfTheMonth() {
        val days = aggregator.aggregate(yearMonth = 202610, transactions = emptyList())

        assertEquals((1..31).toList(), days.map { it.dayOfMonth })
        assertEquals(days.map { it.epochDay }.sorted(), days.map { it.epochDay })
        assertEquals(days.map { it.epochDay }.distinct().size, days.size)
        assertEquals(LocalDate.of(2026, 10, 1).toEpochDay(), days.first().epochDay)
        assertEquals(LocalDate.of(2026, 10, 31).toEpochDay(), days.last().epochDay)
    }

    @Test
    fun isTodayUsesTheInjectedDate() {
        val oct10 = LocalDate.of(2026, 10, 10).toEpochDay()
        val oct11 = LocalDate.of(2026, 10, 11).toEpochDay()

        val days = aggregator.aggregate(
            yearMonth = 202610,
            transactions = emptyList(),
            todayEpochDay = oct10
        )

        assertTrue(days.single { it.epochDay == oct10 }.isToday)
        assertFalse(days.single { it.epochDay == oct11 }.isToday)
        assertEquals(1, days.count { it.isToday })

        val otherMonth = aggregator.aggregate(
            yearMonth = 202611,
            transactions = emptyList(),
            todayEpochDay = oct10
        )
        assertTrue(otherMonth.none { it.isToday })
    }

    @Test
    fun monthLengthFollowsTheCalendar() {
        assertEquals(28, aggregator.aggregate(202602, emptyList()).size)
        assertEquals(29, aggregator.aggregate(202402, emptyList()).size)
        assertEquals(31, aggregator.aggregate(202612, emptyList()).size)
    }

    @Test
    fun criterion10_everyDayMapsToItsExactEpochDay() {
        val days = aggregator.aggregate(202610, emptyList(), todayEpochDay = 0L)

        assertEquals((1..31).toList(), days.map { it.dayOfMonth })
        days.forEach { day ->
            assertEquals(
                "Dia ${day.dayOfMonth} tem de mapear para a data correta",
                LocalDate.of(2026, 10, day.dayOfMonth).toEpochDay(),
                day.epochDay
            )
        }
    }

    @Test
    fun criterion10_leapFebruaryHas29DaysWithCorrectEpochDays() {
        val days = aggregator.aggregate(202402, emptyList(), todayEpochDay = 0L)

        assertEquals(29, days.size)
        assertEquals(LocalDate.of(2024, 2, 1).toEpochDay(), days.first().epochDay)
        assertEquals(LocalDate.of(2024, 2, 29).toEpochDay(), days.last().epochDay)
    }

    @Test
    fun criterion10_transactionsOutsideTheMonthAreIgnored() {
        val oct10 = LocalDate.of(2026, 10, 10).toEpochDay()
        val nov01 = LocalDate.of(2026, 11, 1).toEpochDay()

        val days = aggregator.aggregate(
            yearMonth = 202610,
            transactions = listOf(
                tx(1, "EXPENSE", 5000L, oct10),
                tx(2, "EXPENSE", 99000L, nov01, yearMonth = 202610)
            ),
            todayEpochDay = oct10
        )

        assertEquals(5000L, days.sumOf { it.expenseCents })
        assertEquals(1, days.sumOf { it.transactionCount })
        assertEquals(0L, days.first { it.dayOfMonth == 1 }.expenseCents)
    }

    @Test
    fun criterion10_multipleExpensesOnTheSameDayAreSummedAndCounted() {
        val oct10 = LocalDate.of(2026, 10, 10).toEpochDay()

        val days = aggregator.aggregate(
            yearMonth = 202610,
            transactions = listOf(
                tx(1, "EXPENSE", 1000L, oct10),
                tx(2, "EXPENSE", 2000L, oct10),
                tx(3, "EXPENSE", 3000L, oct10)
            ),
            todayEpochDay = oct10
        )

        val day10 = days.first { it.dayOfMonth == 10 }
        assertEquals(6000L, day10.expenseCents)
        assertEquals(3, day10.transactionCount)
    }

    @Test
    fun criterion10_incomeOfTheSameDayDoesNotChangeTheExpenseTotal() {
        val oct10 = LocalDate.of(2026, 10, 10).toEpochDay()

        val days = aggregator.aggregate(
            yearMonth = 202610,
            transactions = listOf(
                tx(1, "INCOME", 200000L, oct10),
                tx(2, "EXPENSE", 5000L, oct10)
            ),
            todayEpochDay = oct10
        )

        val day10 = days.first { it.dayOfMonth == 10 }
        assertEquals(5000L, day10.expenseCents)
        assertEquals(1, day10.transactionCount)
        assertEquals(5000L, days.sumOf { it.expenseCents })
    }

    @Test
    fun criterion10_noDayIsTodayWhenTheInjectedTodayIsOutsideTheMonth() {
        val days = aggregator.aggregate(
            yearMonth = 202610,
            transactions = emptyList(),
            todayEpochDay = LocalDate.of(2026, 11, 1).toEpochDay()
        )

        assertEquals(0, days.count { it.isToday })
    }

    @Test
    fun criterion10_sumOfAllDaysEqualsTheTotalExpenseOfTheMonth() {
        val oct01 = LocalDate.of(2026, 10, 1).toEpochDay()
        val oct15 = LocalDate.of(2026, 10, 15).toEpochDay()
        val oct31 = LocalDate.of(2026, 10, 31).toEpochDay()

        val transactions = listOf(
            tx(1, "EXPENSE", 1000L, oct01),
            tx(2, "EXPENSE", 2000L, oct15),
            tx(3, "EXPENSE", 3000L, oct31)
        )

        val days = aggregator.aggregate(202610, transactions, todayEpochDay = oct15)

        assertEquals(6000L, days.sumOf { it.expenseCents })
        assertEquals(3, days.sumOf { it.transactionCount })
    }
}
