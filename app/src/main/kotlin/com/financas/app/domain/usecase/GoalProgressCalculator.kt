package com.financas.app.domain.usecase

import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.entity.TransactionEntity
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

data class GoalProgress(
    val goalId: String,
    val name: String,
    val targetCents: Long,
    val startYearMonth: Int,
    val deadlineYearMonth: Int?,
    val accumulatedSavingsCents: Long,
    val percentComplete: Double,
    val monthsRemaining: Int?,
    val averageMonthlySavingsCents: Long,
    val projectedMonthsToComplete: Long?,
    val onTrack: Boolean?
)

class GoalProgressCalculator {

    fun calculate(
        goal: GoalEntity,
        transactions: List<TransactionEntity>,
        selectedYearMonth: Int
    ): GoalProgress {
        val start = YearMonth.of(goal.startYearMonth / 100, goal.startYearMonth % 100)
        val selected = YearMonth.of(selectedYearMonth / 100, selectedYearMonth % 100)

        val monthsInRange = ChronoUnit.MONTHS.between(start, selected) + 1

        val accumulatedSavingsCents = if (monthsInRange <= 0) 0L else {
            val monthlyNet = transactions
                .groupBy { it.yearMonth }
                .mapValues { (_, monthTxs) ->
                    val income = monthTxs
                        .filter { it.type.equals("INCOME", ignoreCase = true) }
                        .sumOf { it.amountCents }
                    val expense = monthTxs
                        .filter { it.type.equals("EXPENSE", ignoreCase = true) }
                        .sumOf { it.amountCents }
                    income - expense
                }
            var total = 0L
            var cursor = start
            while (!cursor.isAfter(selected)) {
                val yearMonthInt = cursor.year * 100 + cursor.monthValue
                total += monthlyNet[yearMonthInt] ?: 0L
                cursor = cursor.plusMonths(1)
            }
            total
        }

        val percentComplete = if (goal.targetCents <= 0L) 0.0
        else ((accumulatedSavingsCents.toDouble() / goal.targetCents.toDouble()) * 100.0)
            .coerceAtLeast(0.0)

        val averageMonthlySavingsCents =
            if (monthsInRange <= 0) 0L else accumulatedSavingsCents / monthsInRange

        val remainingCents = goal.targetCents - accumulatedSavingsCents
        val projectedMonthsToComplete: Long? = when {
            averageMonthlySavingsCents <= 0L -> null
            remainingCents <= 0L -> 0L
            else -> ceil(remainingCents.toDouble() / averageMonthlySavingsCents.toDouble()).toLong()
        }

        val monthsRemaining: Int? = goal.deadlineYearMonth?.let { deadline ->
            val deadlineYm = YearMonth.of(deadline / 100, deadline % 100)
            ChronoUnit.MONTHS.between(selected, deadlineYm).toInt()
        }

        val onTrack: Boolean? = goal.deadlineYearMonth?.let { deadline ->
            val projected = projectedMonthsToComplete ?: return@let false
            val deadlineYm = YearMonth.of(deadline / 100, deadline % 100)
            !selected.plusMonths(projected).isAfter(deadlineYm)
        }

        return GoalProgress(
            goalId = goal.id,
            name = goal.name,
            targetCents = goal.targetCents,
            startYearMonth = goal.startYearMonth,
            deadlineYearMonth = goal.deadlineYearMonth,
            accumulatedSavingsCents = accumulatedSavingsCents,
            percentComplete = percentComplete,
            monthsRemaining = monthsRemaining,
            averageMonthlySavingsCents = averageMonthlySavingsCents,
            projectedMonthsToComplete = projectedMonthsToComplete,
            onTrack = onTrack
        )
    }
}
