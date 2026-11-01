package com.financas.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

class ChartScaleTest {

    @Test
    fun computeTicks_returnsEmptyListForZeroOrNegativeMaxValue() {
        assertEquals(emptyList<Double>(), ChartScale.computeTicks(0.0))
        assertEquals(emptyList<Double>(), ChartScale.computeTicks(-1560.0))
    }

    @Test
    fun computeTicks_returnsEmptyListForNonPositiveTargetTickCount() {
        assertEquals(emptyList<Double>(), ChartScale.computeTicks(1560.0, 0))
    }

    @Test
    fun computeTicks_1560UsesNiceStepWithLastTickAboveMaxValue() {
        val ticks = ChartScale.computeTicks(1560.0, 3)
        assertEquals(listOf(500.0, 1000.0, 1500.0, 2000.0), ticks)
        assertTrue(ticks.last() >= 1560.0)
    }

    @Test
    fun computeTicks_largeCentsValueUsesNiceStepAndCoversMaxValue() {
        val ticks = ChartScale.computeTicks(85000.0, 3)
        assertEquals(listOf(50000.0, 100000.0), ticks)
        assertTrue(ticks.last() >= 85000.0)
    }

    @Test
    fun computeTicks_percentScaleValuesHaveCleanDecimals() {
        val ticks = ChartScale.computeTicks(0.24, 3)
        assertEquals(3, ticks.size)
        assertEquals(0.1, ticks[0], 1e-9)
        assertEquals(0.2, ticks[1], 1e-9)
        assertEquals(0.3, ticks[2], 1e-9)
        assertTrue(ticks.last() >= 0.24)
    }

    @Test
    fun computeTicks_fiveUsesStepOfTwo() {
        val ticks = ChartScale.computeTicks(5.0, 3)
        assertEquals(listOf(2.0, 4.0, 6.0), ticks)
        assertTrue(ticks.last() >= 5.0)
    }

    @Test
    fun computeTicks_ticksAreAscendingMultiplesOfNiceStepWithinTargetBound() {
        val ticks = ChartScale.computeTicks(1560.0, 3)
        assertTrue(ticks.size in 1..4)
        assertEquals(ticks, ticks.sorted())
        val step = ticks.first()
        val magnitude = floor(log10(step)).toInt()
        val normalized = step / 10.0.pow(magnitude.toDouble())
        assertTrue(listOf(1.0, 2.0, 5.0).contains(normalized))
        ticks.forEach { assertEquals(0.0, it % step, 1e-9) }
    }

    @Test
    fun formatCompactCents_zeroFormatsWithoutDecimals() {
        assertEquals("0 €", ChartScale.formatCompactCents(0L))
    }

    @Test
    fun formatCompactCents_belowOneThousandEurosFormatsWholeEuros() {
        assertEquals("870 €", ChartScale.formatCompactCents(87_000L))
        assertEquals("999 €", ChartScale.formatCompactCents(99_900L))
    }

    @Test
    fun formatCompactCents_thousandsFormatsWithOneDecimalAndComma() {
        assertEquals("1,2k €", ChartScale.formatCompactCents(120_000L))
        assertEquals("1,0k €", ChartScale.formatCompactCents(100_000L))
    }

    @Test
    fun formatCompactCents_millionsFormatsWithOneDecimalAndComma() {
        assertEquals("1,3M €", ChartScale.formatCompactCents(1_250_000_00L))
        assertEquals("1,0M €", ChartScale.formatCompactCents(100_000_000L))
    }

    @Test
    fun formatCompactCents_negativeValuesUseMinusPrefix() {
        assertEquals("-870 €", ChartScale.formatCompactCents(-87_000L))
        assertEquals("-1,2k €", ChartScale.formatCompactCents(-120_000L))
    }

    @Test
    fun formatCompactPercent_integerValueFormatsWithoutDecimals() {
        assertEquals("24%", ChartScale.formatCompactPercent(24.0))
        assertEquals("0%", ChartScale.formatCompactPercent(0.0))
    }

    @Test
    fun formatCompactPercent_fractionalValueRoundsToOneDecimalWithComma() {
        assertEquals("24,6%", ChartScale.formatCompactPercent(24.56))
        assertEquals("7,3%", ChartScale.formatCompactPercent(7.25))
    }

    @Test
    fun formatCompactPercent_negativeValueKeepsSign() {
        assertEquals("-3,2%", ChartScale.formatCompactPercent(-3.24))
    }
}
