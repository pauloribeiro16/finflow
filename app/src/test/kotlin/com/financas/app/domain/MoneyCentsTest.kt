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

    @Test
    fun testFormatPercent_tinyNegativeDoesNotShowMinusZero() {
        assertEquals("0,0%", MoneyCents.formatPercent(-0.01))
        assertEquals("0,0%", MoneyCents.formatPercent(-0.0))
        assertEquals("-0,1%", MoneyCents.formatPercent(-0.1))
        assertEquals("44%", MoneyCents.formatPercent(44.2, 0))
    }

    @Test
    fun testPrivacyMode_explicitMaskParam() {
        assertEquals(MoneyCents.MASKED_STRING, MoneyCents.formatCents(1250L, masked = true))
        assertEquals(MoneyCents.MASKED_STRING, MoneyCents(150000L).formatPt(masked = true))
        assertEquals("12,50 €", MoneyCents.formatCents(1250L, masked = false))
        assertEquals("1.500,00 €", MoneyCents(150000L).formatPt(masked = false))
    }

    @Test
    fun testPrivacyMode_globalToggleAffectsDefaultFormatting() {
        try {
            MoneyCents.isPrivacyModeEnabled = true
            assertEquals(MoneyCents.MASKED_STRING, MoneyCents.formatCents(245000L))
            assertEquals(MoneyCents.MASKED_STRING, MoneyCents(245000L).formatPt())
            assertEquals(MoneyCents.MASKED_STRING, MoneyCents.formatCents(-500L))
            assertEquals(MoneyCents.MASKED_STRING, MoneyCents.formatCents(0L))

            // Percentages must remain intact even in privacy mode
            assertEquals("24,5%", MoneyCents.formatPercent(24.5))
        } finally {
            MoneyCents.isPrivacyModeEnabled = false
        }

        // After restoring, normal formatting works
        assertEquals("2.450,00 €", MoneyCents.formatCents(245000L))
        assertEquals("2.450,00 €", MoneyCents(245000L).formatPt())
    }

    @org.junit.Test
    fun formatForInput_isNeverMaskedAndRoundTripsThroughParser() {
        val previous = MoneyCents.isPrivacyModeEnabled
        try {
            MoneyCents.isPrivacyModeEnabled = true
            org.junit.Assert.assertEquals("1.234,56", MoneyCents.formatForInput(123456L))
            org.junit.Assert.assertEquals(
                123456L,
                com.financas.app.ui.screens.budgets.parseEuroToCents(MoneyCents.formatForInput(123456L))
            )
        } finally {
            MoneyCents.isPrivacyModeEnabled = previous
        }
    }
}
