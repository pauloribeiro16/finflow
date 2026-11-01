package com.financas.app.domain.usecase

import com.financas.app.data.entity.TransactionEntity
import java.time.LocalDate
import java.time.YearMonth

data class MonthProjection(
    val yearMonth: Int,
    val daysElapsed: Int,
    val daysInMonth: Int,
    val dailyAverageExpenseCents: Long,
    val projectedMonthEndExpenseCents: Long,
    val projectedRemainingCents: Long,
    val projectedSavingsRatePercent: Double
)

class MonthProjectionCalculator {

    fun calculate(
        transactions: List<TransactionEntity>,
        yearMonth: Int,
        todayEpochDay: Long
    ): MonthProjection {
        val month = YearMonth.of(yearMonth / 100, yearMonth % 100)
        val daysInMonth = month.lengthOfMonth()

        val today = LocalDate.ofEpochDay(todayEpochDay)
        val isCurrentRealMonth = YearMonth.from(today) == month

        val daysElapsed = if (isCurrentRealMonth) {
            today.dayOfMonth.coerceIn(1, daysInMonth)
        } else {
            daysInMonth
        }

        val totalIncomeCents = transactions
            .filter { it.type.equals("INCOME", ignoreCase = true) }
            .sumOf { it.amountCents }
        val totalExpenseCents = transactions
            .filter { it.type.equals("EXPENSE", ignoreCase = true) }
            .sumOf { it.amountCents }

        val dailyAverageExpenseCents = if (daysElapsed <= 0) 0L else totalExpenseCents / daysElapsed

        val projectedMonthEndExpenseCents =
            if (daysElapsed >= daysInMonth) totalExpenseCents
            else dailyAverageExpenseCents * daysInMonth

        val projectedRemainingCents = totalIncomeCents - projectedMonthEndExpenseCents

        return MonthProjection(
            yearMonth = yearMonth,
            daysElapsed = daysElapsed,
            daysInMonth = daysInMonth,
            dailyAverageExpenseCents = dailyAverageExpenseCents,
            projectedMonthEndExpenseCents = projectedMonthEndExpenseCents,
            projectedRemainingCents = projectedRemainingCents,
            projectedSavingsRatePercent = MonthlyAnalyticsCalculator.calculateSavingsRate(
                projectedRemainingCents,
                totalIncomeCents
            )
        )
    }
}
