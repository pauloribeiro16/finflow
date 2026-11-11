package com.financas.app.domain.usecase

import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.data.network.QuoteService
import com.financas.app.data.network.YahooQuote
import com.financas.app.ui.fakes.FakeInvestmentAssetDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PortfolioMarketSyncUseCaseTest {

    private lateinit var dao: FakeInvestmentAssetDao
    private lateinit var fakeQuoteService: FakeQuoteService
    private lateinit var useCase: PortfolioMarketSyncUseCase

    class FakeQuoteService : QuoteService {
        val quotes = mutableMapOf<String, YahooQuote>()
        val rates = mutableMapOf("EUR" to 1.0, "USD" to 1.10, "GBP" to 0.85)
        var eurUsdRate: Double?
            get() = rates["USD"]
            set(value) { if (value == null) rates.remove("USD") else rates["USD"] = value }

        override suspend fun fetchQuotes(tickers: List<String>): Map<String, YahooQuote> {
            return quotes.filterKeys { tickers.contains(it) }
        }

        override suspend fun fetchEurRate(currency: String): Double? = rates[currency]
    }

    @Before
    fun setup() {
        dao = FakeInvestmentAssetDao()
        fakeQuoteService = FakeQuoteService()
        useCase = PortfolioMarketSyncUseCase(dao, fakeQuoteService)
    }

    @Test
    fun syncAllActiveAssets_updatesCurrentValueCents_preservesInvestedCents() = runTest {
        // Setup initial position: Nasdaq-100 Daily Lev (2x)
        // Bought 26.2636 units for 249.45 EUR (investedCents = 24945)
        val lqqAsset = InvestmentAssetEntity(
            id = "asset_lqq",
            name = "Nasdaq-100 Daily Lev (2x) ETF",
            type = "ETF",
            institution = "XTB",
            investedCents = 24945L,
            currentValueCents = 24945L,
            lastUpdatedEpochDay = 19000L,
            ticker = "LQQ.PA",
            units = 26.2636
        )
        dao.insert(lqqAsset)

        // Setup second position in USD: Core S&P 500 (CSPX.L)
        // Bought 0.2423 units for 152.74 EUR (investedCents = 15274)
        val cspxAsset = InvestmentAssetEntity(
            id = "asset_cspx",
            name = "Core S&P 500 ETF",
            type = "ETF",
            institution = "XTB",
            investedCents = 15274L,
            currentValueCents = 15274L,
            lastUpdatedEpochDay = 19000L,
            ticker = "CSPX.L",
            units = 0.2423
        )
        dao.insert(cspxAsset)

        // Configure Yahoo quotes
        fakeQuoteService.quotes["LQQ.PA"] = YahooQuote("LQQ.PA", 8.53, "EUR", -1.2)
        fakeQuoteService.quotes["CSPX.L"] = YahooQuote("CSPX.L", 800.0, "USD", 0.5)
        fakeQuoteService.eurUsdRate = 1.10 // 800 USD / 1.10 = 727.27 EUR per unit

        val todayEpochDay = 20500L
        val summary = useCase.syncAllActiveAssets(todayEpochDay)

        assertEquals(2, summary.updatedCount)
        assertTrue(summary.failedTickers.isEmpty())

        val updatedLqq = dao.getById("asset_lqq")!!
        // 26.2636 * 8.53 = 224.0285 EUR = 22403 cents
        assertEquals(22403L, updatedLqq.currentValueCents)
        // Purchase price preserved!
        assertEquals(24945L, updatedLqq.investedCents)
        assertEquals(todayEpochDay, updatedLqq.lastUpdatedEpochDay)
        // Profit / Loss = 22403 - 24945 = -2542 cents (-25.42 EUR)
        assertEquals(-2542L, updatedLqq.profitLossCents)

        val updatedCspx = dao.getById("asset_cspx")!!
        // 0.2423 * (800 / 1.10 = 727.2727) = 176.218 EUR = 17622 cents
        assertEquals(17622L, updatedCspx.currentValueCents)
        assertEquals(15274L, updatedCspx.investedCents)
        assertEquals(todayEpochDay, updatedCspx.lastUpdatedEpochDay)
    }

    @Test
    fun syncAllActiveAssets_skipsAssetsWithoutTickerOrUnits() = runTest {
        // PPR asset without ticker or units
        val pprAsset = InvestmentAssetEntity(
            id = "asset_ppr",
            name = "PPR Alves Ribeiro",
            type = "PPR",
            institution = "Banco Invest",
            investedCents = 200000L,
            currentValueCents = 210000L,
            lastUpdatedEpochDay = 19000L,
            ticker = null,
            units = null
        )
        dao.insert(pprAsset)

        val summary = useCase.syncAllActiveAssets(20500L)
        assertEquals(0, summary.updatedCount)

        val unchanged = dao.getById("asset_ppr")!!
        assertEquals(210000L, unchanged.currentValueCents)
    }

    @Test
    fun syncAllActiveAssets_skipsUsdAssetWhenFxRateUnavailable() = runTest {
        dao.insert(asset("asset_usd", "CSPX.L", 0.5, 30000L))
        fakeQuoteService.quotes["CSPX.L"] = YahooQuote("CSPX.L", 800.0, "USD", 0.0)
        fakeQuoteService.eurUsdRate = null

        val summary = useCase.syncAllActiveAssets(20500L)

        assertEquals(0, summary.updatedCount)
        assertEquals(listOf("CSPX.L"), summary.failedTickers)
        assertEquals(30000L, dao.getById("asset_usd")!!.currentValueCents)
        assertEquals(19000L, dao.getById("asset_usd")!!.lastUpdatedEpochDay)
    }

    @Test
    fun syncAllActiveAssets_convertsPenceQuotesToPounds() = runTest {
        dao.insert(asset("asset_isf", "ISF.L", 100.0, 95000L))
        fakeQuoteService.quotes["ISF.L"] = YahooQuote("ISF.L", 850.0, "GBp", 0.0)

        val summary = useCase.syncAllActiveAssets(20500L)

        assertEquals(1, summary.updatedCount)
        // 100 un. * 8,50 GBP / 0,85 = 1000,00 EUR
        assertEquals(100000L, dao.getById("asset_isf")!!.currentValueCents)
    }

    @Test
    fun syncAllActiveAssets_failsOnUnknownCurrencyInsteadOfAssumingEur() = runTest {
        dao.insert(asset("asset_chf", "NESN.SW", 10.0, 90000L))
        fakeQuoteService.quotes["NESN.SW"] = YahooQuote("NESN.SW", 90.0, "CHF", 0.0)

        val summary = useCase.syncAllActiveAssets(20500L)

        assertEquals(0, summary.updatedCount)
        assertEquals(listOf("NESN.SW"), summary.failedTickers)
    }

    @Test
    fun syncAllActiveAssets_rejectsImplausibleJump() = runTest {
        dao.insert(asset("asset_jump", "LQQ.PA", 10.0, 8500L))
        fakeQuoteService.quotes["LQQ.PA"] = YahooQuote("LQQ.PA", 853.0, "EUR", 0.0)

        val summary = useCase.syncAllActiveAssets(20500L)

        assertEquals(0, summary.updatedCount)
        assertEquals(listOf("LQQ.PA"), summary.suspiciousTickers)
        assertEquals(8500L, dao.getById("asset_jump")!!.currentValueCents)
    }

    @Test
    fun syncAllActiveAssets_ignoresInvalidTickersAndNonFiniteUnits() = runTest {
        dao.insert(asset("asset_bad", "LQQ.PA?x=1", 1.0, 1000L))
        dao.insert(asset("asset_inf", "LQQ.PA", Double.POSITIVE_INFINITY, 1000L))

        val summary = useCase.syncAllActiveAssets(20500L)

        assertEquals(0, summary.updatedCount)
        assertTrue(fakeQuoteService.quotes.isEmpty())
    }

    private fun asset(id: String, ticker: String, units: Double, valueCents: Long) = InvestmentAssetEntity(
        id = id,
        name = id,
        type = "ETF",
        institution = "XTB",
        investedCents = valueCents,
        currentValueCents = valueCents,
        lastUpdatedEpochDay = 19000L,
        ticker = ticker,
        units = units
    )
}
