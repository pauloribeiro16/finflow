package com.financas.app.domain.usecase

import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.domain.model.InvestmentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InvestmentPortfolioCalculatorTest {

    private val calculator = InvestmentPortfolioCalculator()

    @Test
    fun calculate_emptyAssets_returnsZeroSummary() {
        val summary = calculator.calculate(emptyList())

        assertEquals(0L, summary.totalInvestedCents)
        assertEquals(0L, summary.totalCurrentValueCents)
        assertEquals(0L, summary.totalProfitLossCents)
        assertEquals(0.0, summary.totalProfitLossPercentage, 0.001)
        assertEquals(0L, summary.totalMonthlyContributionCents)
        assertTrue(summary.allocations.isEmpty())
        assertEquals(0, summary.totalAssetsCount)
    }

    @Test
    fun calculate_singleAssetWithProfit_calculatesCorrectProfitAndPercentage() {
        val asset = InvestmentAssetEntity(
            id = "ppr_1",
            name = "PPR Alves Ribeiro",
            type = "PPR",
            institution = "Banco Invest",
            investedCents = 200000L, // 2000.00 €
            currentValueCents = 250000L, // 2500.00 € (+500.00 € / +25.0%)
            monthlyContributionCents = 15000L, // 150.00 €
            lastUpdatedEpochDay = 20000L
        )

        val summary = calculator.calculate(listOf(asset))

        assertEquals(200000L, summary.totalInvestedCents)
        assertEquals(250000L, summary.totalCurrentValueCents)
        assertEquals(50000L, summary.totalProfitLossCents)
        assertEquals(25.0, summary.totalProfitLossPercentage, 0.001)
        assertEquals(15000L, summary.totalMonthlyContributionCents)
        assertEquals(1, summary.totalAssetsCount)
        assertEquals(1, summary.allocations.size)
        assertEquals(InvestmentType.PPR, summary.allocations[0].type)
        assertEquals(250000L, summary.allocations[0].totalValueCents)
        assertEquals(100.0, summary.allocations[0].percentage, 0.001)
    }

    @Test
    fun calculate_singleAssetWithLoss_calculatesNegativeProfit() {
        val asset = InvestmentAssetEntity(
            id = "etf_1",
            name = "ETF Tech",
            type = "ETF",
            institution = "Degiro",
            investedCents = 100000L, // 1000.00 €
            currentValueCents = 80000L, // 800.00 € (-200.00 € / -20.0%)
            lastUpdatedEpochDay = 20000L
        )

        val summary = calculator.calculate(listOf(asset))

        assertEquals(100000L, summary.totalInvestedCents)
        assertEquals(80000L, summary.totalCurrentValueCents)
        assertEquals(-20000L, summary.totalProfitLossCents)
        assertEquals(-20.0, summary.totalProfitLossPercentage, 0.001)
    }

    @Test
    fun calculate_multiAssetPortfolio_aggregatesAndRanksAllocations() {
        val ppr1 = InvestmentAssetEntity(
            id = "ppr_1",
            name = "PPR Alves Ribeiro",
            type = "PPR",
            institution = "Banco Invest",
            investedCents = 300000L,
            currentValueCents = 360000L,
            monthlyContributionCents = 10000L,
            lastUpdatedEpochDay = 20000L
        )
        val ppr2 = InvestmentAssetEntity(
            id = "ppr_2",
            name = "PPR Stoik",
            type = "PPR",
            institution = "Banco BiG",
            investedCents = 100000L,
            currentValueCents = 140000L,
            monthlyContributionCents = 5000L,
            lastUpdatedEpochDay = 20000L
        )
        val etf = InvestmentAssetEntity(
            id = "etf_1",
            name = "Vanguard FTSE All-World (VWCE)",
            type = "ETF",
            institution = "Interactive Brokers",
            investedCents = 200000L,
            currentValueCents = 300000L,
            monthlyContributionCents = 20000L,
            lastUpdatedEpochDay = 20000L
        )
        val deposito = InvestmentAssetEntity(
            id = "dep_1",
            name = "Depósito Prazo 3.5%",
            type = "DEPOSITO_PRAZO",
            institution = "Trade Republic",
            investedCents = 200000L,
            currentValueCents = 200000L,
            lastUpdatedEpochDay = 20000L
        )

        // Total Invested: 3000 + 1000 + 2000 + 2000 = 8000.00 € (800000L)
        // Total Current: 3600 + 1400 + 3000 + 2000 = 10000.00 € (1000000L)
        // Profit: 2000.00 € (+25.0%)
        // Total Monthly Contribution: 100 + 50 + 200 = 350.00 € (35000L)
        // PPR Total: 5000.00 € (50.0%)
        // ETF Total: 3000.00 € (30.0%)
        // Depósito Total: 2000.00 € (20.0%)

        val summary = calculator.calculate(listOf(ppr1, ppr2, etf, deposito))

        assertEquals(800000L, summary.totalInvestedCents)
        assertEquals(1000000L, summary.totalCurrentValueCents)
        assertEquals(200000L, summary.totalProfitLossCents)
        assertEquals(25.0, summary.totalProfitLossPercentage, 0.001)
        assertEquals(35000L, summary.totalMonthlyContributionCents)
        assertEquals(4, summary.totalAssetsCount)

        assertEquals(3, summary.allocations.size)
        assertEquals(InvestmentType.PPR, summary.allocations[0].type)
        assertEquals(500000L, summary.allocations[0].totalValueCents)
        assertEquals(50.0, summary.allocations[0].percentage, 0.001)

        assertEquals(InvestmentType.ETF, summary.allocations[1].type)
        assertEquals(300000L, summary.allocations[1].totalValueCents)
        assertEquals(30.0, summary.allocations[1].percentage, 0.001)

        assertEquals(InvestmentType.DEPOSITO_PRAZO, summary.allocations[2].type)
        assertEquals(200000L, summary.allocations[2].totalValueCents)
        assertEquals(20.0, summary.allocations[2].percentage, 0.001)
    }

    @Test
    fun calculate_archivedAssets_areIgnored() {
        val activeAsset = InvestmentAssetEntity(
            id = "act_1",
            name = "ETF VWCE",
            type = "ETF",
            institution = "XTB",
            investedCents = 100000L,
            currentValueCents = 120000L,
            archived = false,
            lastUpdatedEpochDay = 20000L
        )
        val archivedAsset = InvestmentAssetEntity(
            id = "arch_1",
            name = "PPR Antigo",
            type = "PPR",
            institution = "CGD",
            investedCents = 500000L,
            currentValueCents = 600000L,
            archived = true,
            lastUpdatedEpochDay = 19000L
        )

        val summary = calculator.calculate(listOf(activeAsset, archivedAsset))

        assertEquals(100000L, summary.totalInvestedCents)
        assertEquals(120000L, summary.totalCurrentValueCents)
        assertEquals(1, summary.totalAssetsCount)
    }

    @Test
    fun calculate_zeroInvested_avoidsDivisionByZero() {
        val asset = InvestmentAssetEntity(
            id = "gift_1",
            name = "Ativo Recebido",
            type = "OUTRO",
            institution = "Oferta",
            investedCents = 0L,
            currentValueCents = 10000L,
            lastUpdatedEpochDay = 20000L
        )

        val summary = calculator.calculate(listOf(asset))

        assertEquals(0L, summary.totalInvestedCents)
        assertEquals(10000L, summary.totalCurrentValueCents)
        assertEquals(10000L, summary.totalProfitLossCents)
        assertEquals(0.0, summary.totalProfitLossPercentage, 0.001)
    }

    @Test
    fun parseEuroToCents_variousPortugueseAndStandardFormats_parsesAccurately() {
        assertEquals(950000L, com.financas.app.ui.screens.budgets.parseEuroToCents("9500"))
        assertEquals(950000L, com.financas.app.ui.screens.budgets.parseEuroToCents("9.500"))
        assertEquals(950000L, com.financas.app.ui.screens.budgets.parseEuroToCents("9.500,00"))
        assertEquals(240000L, com.financas.app.ui.screens.budgets.parseEuroToCents("2.400"))
        assertEquals(240000L, com.financas.app.ui.screens.budgets.parseEuroToCents("2400"))
        assertEquals(243921L, com.financas.app.ui.screens.budgets.parseEuroToCents("2439.21"))
        assertEquals(243921L, com.financas.app.ui.screens.budgets.parseEuroToCents("2439,21"))
        assertEquals(508379L, com.financas.app.ui.screens.budgets.parseEuroToCents("5.083,79"))
        assertEquals(508379L, com.financas.app.ui.screens.budgets.parseEuroToCents("5 083,79 €"))
        assertEquals(20000L, com.financas.app.ui.screens.budgets.parseEuroToCents("200"))
        assertEquals(20000L, com.financas.app.ui.screens.budgets.parseEuroToCents("200,00"))
        assertEquals(1000000L, com.financas.app.ui.screens.budgets.parseEuroToCents("10.000"))
        assertEquals(0L, com.financas.app.ui.screens.budgets.parseEuroToCents(""))
        assertEquals(0L, com.financas.app.ui.screens.budgets.parseEuroToCents("abc"))
    }
}
