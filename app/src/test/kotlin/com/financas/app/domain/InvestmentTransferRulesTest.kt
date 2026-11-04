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
    fun suggestAssetId_xtbAlwaysHolding_optimizeOnlyWhenAggressiveAssetExists() {
        val assets = listOf(
            asset("a", "Optimize Conservador", 100),
            asset("b", "Optimize Agressivo", 100)
        )
        assertEquals(InvestmentTransferRules.XTB_HOLDING_ASSET_ID, InvestmentTransferRules.suggestAssetId(InvestmentPlatform.XTB, assets))
        assertEquals("b", InvestmentTransferRules.suggestAssetId(InvestmentPlatform.OPTIMIZE, assets))
        assertNull(InvestmentTransferRules.suggestAssetId(InvestmentPlatform.OPTIMIZE, listOf(asset("a", "Optimize Conservador", 1))))
        assertNull(InvestmentTransferRules.suggestAssetId(InvestmentPlatform.OPTIMIZE, listOf(asset("b", "Optimize Agressivo", 1, archived = true))))
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
