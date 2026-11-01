package com.financas.app.domain

import com.financas.app.domain.model.MoneyCents
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyCentsTest {

    @Test
    fun testFormattingEurosPt() {
        val amount = MoneyCents(1250L) // 12,50 €
        assertEquals("12,50 €", amount.formatPt())

        val largeAmount = MoneyCents(150000L) // 1.500,00 €
        assertEquals("1.500,00 €", largeAmount.formatPt())

        val zero = MoneyCents(0L)
        assertEquals("0,00 €", zero.formatPt())
    }

    @Test
    fun testFormattingPercentagePt() {
        val pct = 24.5
        assertEquals("24,5%", MoneyCents.formatPercent(pct))

        val zero = 0.0
        assertEquals("0,0%", MoneyCents.formatPercent(zero))
    }

    @Test
    fun testFromEuros_roundsHalfUpWithinDoublePrecision() {
        assertEquals(1234L, MoneyCents.fromEuros(12.34).cents)
        assertEquals(1235L, MoneyCents.fromEuros(12.345).cents)
        assertEquals(1L, MoneyCents.fromEuros(0.005).cents)
        assertEquals(100L, MoneyCents.fromEuros(1.005).cents)
        assertEquals(268L, MoneyCents.fromEuros(2.675).cents)
        assertEquals(-1234L, MoneyCents.fromEuros(-12.345).cents)
    }

    @Test
    fun testFormatCents_negativeAmountKeepsMinusSign() {
        assertEquals("-12,50 €", MoneyCents.formatCents(-1250L))
        assertEquals("-0,05 €", MoneyCents.formatCents(-5L))
    }

    @Test
    fun testFormatCents_largeAmountGroupsThousands() {
        assertEquals("1.234.567,89 €", MoneyCents.formatCents(123456789L))
        assertEquals("1.000,00 €", MoneyCents.formatCents(100000L))
    }

    @Test
    fun testFormatPercent_negativeAndOneDecimalCase() {
        assertEquals("-12,4%", MoneyCents.formatPercent(-12.4))
        assertEquals("59,2%", MoneyCents.formatPercent(59.2))
        assertEquals("100,0%", MoneyCents.formatPercent(100.0))
        assertEquals("33,3%", MoneyCents.formatPercent(100.0 / 3.0))
    }
}
