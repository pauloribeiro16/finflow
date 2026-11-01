package com.financas.app.domain.usecase

import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalProgressCalculatorTest {

    private val calculator = GoalProgressCalculator()

    private fun tx(
        id: Long,
        type: String,
        amountCents: Long,
        yearMonth: Int
    ) = TransactionEntity(
        id = id,
        type = type,
        amountCents = amountCents,
        yearMonth = yearMonth,
        epochDay = LocalDate.of(yearMonth / 100, yearMonth % 100, 1).toEpochDay(),
        categoryId = if (type == "INCOME") "inc_base_salary" else "exp_housing_rent",
        contextTag = if (type == "INCOME") "TRABALHO" else "CASA",
        merchant = if (type == "INCOME") "Salário" else "Senhorio",
        paymentMethod = "TRANSFER",
        isFixed = true
    )

    @Test
    fun criterion3_accumulatedIncludesNegativeMonths() {
        val goal = GoalEntity(
            id = "goal_reserva",
            name = "Reserva",
            targetCents = 100000L,
            startYearMonth = 202601
        )

        val transactions = listOf(
            tx(1, "INCOME", 200000L, 202601),
            tx(2, "EXPENSE", 50000L, 202601),
            tx(3, "INCOME", 100000L, 202602),
            tx(4, "EXPENSE", 150000L, 202602),
            tx(5, "INCOME", 120000L, 202603),
            tx(6, "EXPENSE", 80000L, 202603)
        )

        val progress = calculator.calculate(goal, transactions, 202603)

        assertEquals("goal_reserva", progress.goalId)
        assertEquals("Reserva", progress.name)
        assertEquals(100000L, progress.targetCents)
        assertEquals(140000L, progress.accumulatedSavingsCents)
        assertEquals(46666L, progress.averageMonthlySavingsCents)
        assertEquals(140.0, progress.percentComplete, 0.001)
        assertEquals(0L, progress.projectedMonthsToComplete)
        assertNull(progress.monthsRemaining)
        assertNull(progress.onTrack)
    }

    @Test
    fun criterion3_negativeMonthIncludedAndOnTrackFalseWithDeadline() {
        val goal = GoalEntity(
            id = "goal_casa",
            name = "Casa",
            targetCents = 500000L,
            startYearMonth = 202601,
            deadlineYearMonth = 202612
        )

        val transactions = listOf(
            tx(1, "INCOME", 100000L, 202601),
            tx(2, "EXPENSE", 200000L, 202601),
            tx(3, "INCOME", 200000L, 202602),
            tx(4, "EXPENSE", 50000L, 202602),
            tx(5, "INCOME", 100000L, 202603),
            tx(6, "EXPENSE", 30000L, 202603)
        )

        val progress = calculator.calculate(goal, transactions, 202603)

        assertEquals(120000L, progress.accumulatedSavingsCents)
        assertEquals(40000L, progress.averageMonthlySavingsCents)
        assertEquals(24.0, progress.percentComplete, 0.001)
        assertEquals(10L, progress.projectedMonthsToComplete)
        assertEquals(9, progress.monthsRemaining)
        assertNotNull(progress.onTrack)
        assertFalse(progress.onTrack!!)
    }

    @Test
    fun criterion3_onTrackTrueWhenProjectionFitsDeadline() {
        val goal = GoalEntity(
            id = "goal_viagem",
            name = "Viagem",
            targetCents = 300000L,
            startYearMonth = 202601,
            deadlineYearMonth = 202612
        )

        val transactions = listOf(
            tx(1, "INCOME", 100000L, 202601)
        )

        val progress = calculator.calculate(goal, transactions, 202601)

        assertEquals(100000L, progress.accumulatedSavingsCents)
        assertEquals(100000L, progress.averageMonthlySavingsCents)
        assertEquals(33.333, progress.percentComplete, 0.001)
        assertEquals(2L, progress.projectedMonthsToComplete)
        assertEquals(11, progress.monthsRemaining)
        assertNotNull(progress.onTrack)
        assertTrue(progress.onTrack!!)
    }

    @Test
    fun criterion3_nonPositiveTarget_givesZeroPercent() {
        val zeroTarget = GoalEntity(
            id = "goal_zero",
            name = "Sem alvo",
            targetCents = 0L,
            startYearMonth = 202601
        )
        val negativeTarget = GoalEntity(
            id = "goal_negativo",
            name = "Alvo negativo",
            targetCents = -5000L,
            startYearMonth = 202601
        )
        val transactions = listOf(tx(1, "INCOME", 100000L, 202601))

        val zeroProgress = calculator.calculate(zeroTarget, transactions, 202601)
        val negativeProgress = calculator.calculate(negativeTarget, transactions, 202601)

        assertEquals(100000L, zeroProgress.accumulatedSavingsCents)
        assertEquals(0.0, zeroProgress.percentComplete, 0.0)
        assertEquals(0.0, negativeProgress.percentComplete, 0.0)
    }

    @Test
    fun criterion3_negativeAccumulation_clampsPercentAndNullsProjection() {
        val goal = GoalEntity(
            id = "goal_negativo",
            name = "Emolver",
            targetCents = 100000L,
            startYearMonth = 202601
        )

        val transactions = listOf(
            tx(1, "INCOME", 10000L, 202601),
            tx(2, "EXPENSE", 60000L, 202601)
        )

        val progress = calculator.calculate(goal, transactions, 202601)

        assertEquals(-50000L, progress.accumulatedSavingsCents)
        assertEquals(-50000L, progress.averageMonthlySavingsCents)
        assertEquals(0.0, progress.percentComplete, 0.0)
        assertNull(progress.projectedMonthsToComplete)
        assertNull(progress.monthsRemaining)
        assertNull(progress.onTrack)
    }

    @Test
    fun criterion3_nonPositiveAverage_projectionNullAndOnTrackFalseWithDeadline() {
        val goal = GoalEntity(
            id = "goal_sem_media",
            name = "Sem média",
            targetCents = 100000L,
            startYearMonth = 202601,
            deadlineYearMonth = 202612
        )

        val transactions = listOf(
            tx(1, "INCOME", 50000L, 202601),
            tx(2, "EXPENSE", 50000L, 202601)
        )

        val progress = calculator.calculate(goal, transactions, 202602)

        assertEquals(0L, progress.accumulatedSavingsCents)
        assertEquals(0L, progress.averageMonthlySavingsCents)
        assertEquals(0.0, progress.percentComplete, 0.0)
        assertNull(progress.projectedMonthsToComplete)
        assertEquals(10, progress.monthsRemaining)
        assertNotNull(progress.onTrack)
        assertFalse(progress.onTrack!!)
    }

    @Test
    fun criterion3_selectedBeforeStart_noAccumulationAndNegativeMonthsRemaining() {
        val goal = GoalEntity(
            id = "goal_futuro",
            name = "Futuro",
            targetCents = 100000L,
            startYearMonth = 202606,
            deadlineYearMonth = 202601
        )

        val transactions = listOf(
            tx(1, "INCOME", 100000L, 202601)
        )

        val progress = calculator.calculate(goal, transactions, 202603)

        assertEquals(0L, progress.accumulatedSavingsCents)
        assertEquals(0L, progress.averageMonthlySavingsCents)
        assertEquals(0.0, progress.percentComplete, 0.0)
        assertNull(progress.projectedMonthsToComplete)
        assertEquals(-2, progress.monthsRemaining)
        assertNotNull(progress.onTrack)
        assertFalse(progress.onTrack!!)
    }

    @Test
    fun criterion10_monthsRemainingIsZeroAtTheEndOfTheDeadlineInterval() {
        val goal = GoalEntity(
            id = "goal_casa",
            name = "Casa",
            targetCents = 500000L,
            startYearMonth = 202601,
            deadlineYearMonth = 202612
        )

        val progress = calculator.calculate(goal, listOf(tx(1, "INCOME", 100000L, 202612)), 202612)

        assertEquals(0, progress.monthsRemaining)
        assertEquals(100000L, progress.accumulatedSavingsCents)
    }

    @Test
    fun criterion10_alreadyAchievedGoal_ProjectsZeroMonthsAndKeepsPercentAbove100() {
        val goal = GoalEntity(
            id = "goal_casa",
            name = "Casa",
            targetCents = 100000L,
            startYearMonth = 202601,
            deadlineYearMonth = 202612
        )

        val progress = calculator.calculate(goal, listOf(tx(1, "INCOME", 150000L, 202601)), 202601)

        assertEquals(150000L, progress.accumulatedSavingsCents)
        assertEquals(150.0, progress.percentComplete, 0.001)
        assertEquals(0L, progress.projectedMonthsToComplete)
        assertEquals(11, progress.monthsRemaining)
        assertNotNull(progress.onTrack)
        assertTrue(progress.onTrack!!)
    }

    @Test
    fun criterion10_alreadyAchievedGoalWithoutDeadline_HasNoTrackInformation() {
        val goal = GoalEntity(
            id = "goal_casa",
            name = "Casa",
            targetCents = 100000L,
            startYearMonth = 202601,
            deadlineYearMonth = null
        )

        val progress = calculator.calculate(goal, listOf(tx(1, "INCOME", 150000L, 202601)), 202601)

        assertEquals(0L, progress.projectedMonthsToComplete)
        assertNull(progress.monthsRemaining)
        assertNull(progress.onTrack)
    }

    @Test
    fun criterion10_averageMonthlySavingsUsesTruncatedIntegerDivision() {
        val goal = GoalEntity(
            id = "goal_casa",
            name = "Casa",
            targetCents = 1000000L,
            startYearMonth = 202601
        )

        val progress = calculator.calculate(
            goal,
            listOf(
                tx(1, "INCOME", 100000L, 202601),
                tx(2, "EXPENSE", 1L, 202601)
            ),
            202601
        )

        assertEquals(99999L, progress.accumulatedSavingsCents)
        assertEquals(99999L, progress.averageMonthlySavingsCents)
    }

    @Test
    fun criterion10_monthsInRangeCountsEveryMonthFromStartToSelected() {
        val goal = GoalEntity(
            id = "goal_casa",
            name = "Casa",
            targetCents = 1000000L,
            startYearMonth = 202601
        )

        val transactions = listOf(
            tx(1, "INCOME", 10000L, 202601),
            tx(2, "INCOME", 10000L, 202602),
            tx(3, "INCOME", 10000L, 202603),
            tx(4, "INCOME", 10000L, 202604)
        )

        val progress = calculator.calculate(goal, transactions, 202604)

        assertEquals(40000L, progress.accumulatedSavingsCents)
        assertEquals(10000L, progress.averageMonthlySavingsCents)
    }

    @Test
    fun criterion10_transactionsOutsideTheRangeAreIgnored() {
        val goal = GoalEntity(
            id = "goal_casa",
            name = "Casa",
            targetCents = 1000000L,
            startYearMonth = 202602
        )

        val progress = calculator.calculate(
            goal,
            listOf(
                tx(1, "INCOME", 50000L, 202601),
                tx(2, "INCOME", 20000L, 202602),
                tx(3, "INCOME", 70000L, 202605)
            ),
            202603
        )

        assertEquals(20000L, progress.accumulatedSavingsCents)
        assertEquals(10000L, progress.averageMonthlySavingsCents)
    }

    @Test
    fun criterion10_onTrackIsTrueWhenTheProjectionFitsExactlyOnTheDeadline() {
        val goal = GoalEntity(
            id = "goal_casa",
            name = "Casa",
            targetCents = 300000L,
            startYearMonth = 202601,
            deadlineYearMonth = 202603
        )

        val progress = calculator.calculate(goal, listOf(tx(1, "INCOME", 100000L, 202601)), 202601)

        assertEquals(2L, progress.projectedMonthsToComplete)
        assertEquals(2, progress.monthsRemaining)
        assertNotNull(progress.onTrack)
        assertTrue(progress.onTrack!!)
    }

    @Test
    fun criterion10_onTrackIsFalseWhenTheProjectionOverflowsTheDeadlineByOneMonth() {
        val goal = GoalEntity(
            id = "goal_casa",
            name = "Casa",
            targetCents = 300000L,
            startYearMonth = 202601,
            deadlineYearMonth = 202602
        )

        val progress = calculator.calculate(goal, listOf(tx(1, "INCOME", 100000L, 202601)), 202601)

        assertEquals(2L, progress.projectedMonthsToComplete)
        assertEquals(1, progress.monthsRemaining)
        assertNotNull(progress.onTrack)
        assertFalse(progress.onTrack!!)
    }
}
