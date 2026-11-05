package com.financas.app.domain

import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.domain.usecase.InvestmentPlatform
import com.financas.app.domain.usecase.InvestmentTransferRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InvestmentTransferRulesTest {

    private fun asset(id: String, name: String, invested: Long, current: Long = invested, archived: Boolean = false) =
        InvestmentAssetEntity(
            id = id, name = name, type = "ETF", institution = "X",
            investedCents = invested, currentValueCents = current,
            lastUpdatedEpochDay = 1L, archived = archived
        )

    @Test
    fun detectPlatform_recognisesXtbAndOptimizeIgnoringCase() {
        assertEquals(InvestmentPlatform.XTB, InvestmentTransferRules.detectPlatform("Enviado a XTB"))
        assertEquals(InvestmentPlatform.XTB, InvestmentTransferRules.detectPlatform("xtb s.a."))
        assertEquals(InvestmentPlatform.OPTIMIZE, InvestmentTransferRules.detectPlatform("OPTIMIZE Invest"))
        assertNull(InvestmentTransferRules.detectPlatform("Continente"))
        assertNull(InvestmentTransferRules.detectPlatform("Maxtbar"))
    }

    @Test
    fun contributeAndRevert_adjustInvestedAndCurrentValue_neverBelowZero() {
        val a = asset("a", "A", 1000, 1200)
        val up = InvestmentTransferRules.contribute(a, 500, 9L)
        assertEquals(1500L, up.investedCents)
        assertEquals(1700L, up.currentValueCents)
        assertEquals(9L, up.lastUpdatedEpochDay)
        val down = InvestmentTransferRules.revert(a, 5000, 9L)
        assertEquals(0L, down.investedCents)
        assertEquals(0L, down.currentValueCents)
    }

    @Test
    fun investmentCategory_isRecognisedByIdOrName() {
        assertTrue(InvestmentTransferRules.isInvestmentCategory("exp_financial_savings", null))
        assertTrue(InvestmentTransferRules.isInvestmentCategory("x", "Poupança / Investimento"))
        assertTrue(!InvestmentTransferRules.isInvestmentCategory("exp_food", "Alimentação"))
        assertTrue(InvestmentTransferRules.isInvestmentTransfer("Continente", "exp_financial_savings", null))
        assertTrue(InvestmentTransferRules.isInvestmentTransfer("Enviado a XTB", "exp_food", "Alimentação"))
        assertTrue(!InvestmentTransferRules.isInvestmentTransfer("Continente", "exp_food", "Alimentação"))
    }

    private fun monthly(id: String, name: String, institution: String, cents: Long, archived: Boolean = false) =
        asset(id, name, 1, archived = archived).copy(institution = institution, monthlyContributionCents = cents)

    @Test
    fun matchMonthlyAsset_suggestsOnlyWhenAmountMatchesExactlyOneMonthlyContribution() {
        val assets = listOf(
            monthly("x", "XTB – por alocar", "XTB", 20_000L),
            monthly("o", "Optimize Agressivo", "Optimize", 10_000L),
            monthly("old", "Antigo", "Optimize", 10_000L, archived = true)
        )
        assertEquals("x", InvestmentTransferRules.matchMonthlyAsset(20_000L, null, assets))
        assertEquals("o", InvestmentTransferRules.matchMonthlyAsset(10_000L, InvestmentPlatform.OPTIMIZE, assets))
        assertNull(InvestmentTransferRules.matchMonthlyAsset(15_000L, null, assets))
        assertNull(InvestmentTransferRules.matchMonthlyAsset(0L, null, assets))
    }

    @Test
    fun matchMonthlyAsset_ambiguousAmountIsNarrowedByPlatformOrLeftEmpty() {
        val assets = listOf(
            monthly("x", "XTB – por alocar", "XTB", 20_000L),
            monthly("o", "Optimize Agressivo", "Optimize", 20_000L)
        )
        assertEquals("x", InvestmentTransferRules.matchMonthlyAsset(20_000L, InvestmentPlatform.XTB, assets))
        assertEquals("o", InvestmentTransferRules.matchMonthlyAsset(20_000L, InvestmentPlatform.OPTIMIZE, assets))
        assertNull(InvestmentTransferRules.matchMonthlyAsset(20_000L, null, assets))
    }

    @Test
    fun allocate_movesValueBetweenAssets_andRejectsInvalidAmounts() {
        val from = asset(InvestmentTransferRules.XTB_HOLDING_ASSET_ID, "XTB", 10_000)
        val to = asset("t", "VWCE", 5_000, 6_000)
        val result = InvestmentTransferRules.allocate(from, to, 4_000, 3L)!!
        assertEquals(6_000L, result.first.investedCents)
        assertEquals(9_000L, result.second.investedCents)
        assertEquals(10_000L, result.second.currentValueCents)

        assertNull(InvestmentTransferRules.allocate(from, to, 0, 3L))
        assertNull(InvestmentTransferRules.allocate(from, to, 10_001, 3L))
        assertNull(InvestmentTransferRules.allocate(from, from, 100, 3L))
        assertTrue(InvestmentTransferRules.isHoldingAsset(from))
    }
}
