package com.financas.app.data.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YahooFinanceQuoteServiceTest {

    private val sampleLqqJson = """
        {
          "chart": {
            "result": [
              {
                "meta": {
                  "currency": "EUR",
                  "symbol": "LQQ.PA",
                  "exchangeName": "PAR",
                  "instrumentType": "ETF",
                  "regularMarketPrice": 8.53,
                  "chartPreviousClose": 8.53,
                  "regularMarketChangePercent": 0.0,
                  "shortName": "Amundi Nasdaq-100 Daily (2x) Lev"
                }
              }
            ],
            "error": null
          }
        }
    """.trimIndent()

    private val sampleUsdJson = """
        {
          "chart": {
            "result": [
              {
                "meta": {
                  "currency": "USD",
                  "symbol": "CSPX.L",
                  "regularMarketPrice": 800.0,
                  "regularMarketChangePercent": 1.25,
                  "shortName": "ISHARES CORE S&P 500"
                }
              }
            ]
          }
        }
    """.trimIndent()

    private val sampleEurUsdJson = """
        {
          "chart": {
            "result": [
              {
                "meta": {
                  "currency": "USD",
                  "symbol": "EURUSD=X",
                  "regularMarketPrice": 1.10
                }
              }
            ]
          }
        }
    """.trimIndent()

    @Test
    fun parseQuoteFromJson_extractsPriceAndMetadata() {
        val quote = YahooFinanceQuoteService.parseQuoteFromJson("LQQ.PA", sampleLqqJson)

        assertNotNull(quote)
        assertEquals("LQQ.PA", quote?.ticker)
        assertEquals(8.53, quote?.price ?: 0.0, 0.001)
        assertEquals("EUR", quote?.currency)
        assertEquals("Amundi Nasdaq-100 Daily (2x) Lev", quote?.name)
    }

    @Test
    fun parseQuoteFromJson_handlesMalformedJsonGracefully() {
        assertNull(YahooFinanceQuoteService.parseQuoteFromJson("ERR", "{invalid json}"))
        assertNull(YahooFinanceQuoteService.parseQuoteFromJson("ERR", "{\"chart\":{\"result\":[]}}"))
    }

    @Test
    fun fetchQuotes_and_fetchEurUsdRate_executesWithCustomFetcher() = runTest {
        val mockFetcher: (String) -> String = { url ->
            when {
                url.contains("EURUSD") -> sampleEurUsdJson
                url.contains("LQQ.PA") -> sampleLqqJson
                url.contains("CSPX.L") -> sampleUsdJson
                else -> "{}"
            }
        }

        val service = YahooFinanceQuoteService(httpExecutor = mockFetcher)

        val fxRate = service.fetchEurRate("USD")
        assertEquals(1.10, fxRate ?: 0.0, 0.001)
        assertEquals(1.0, service.fetchEurRate("EUR") ?: 0.0, 0.0)

        val quotes = service.fetchQuotes(listOf("LQQ.PA", "CSPX.L"))
        assertEquals(2, quotes.size)
        assertEquals(8.53, quotes["LQQ.PA"]?.price ?: 0.0, 0.001)
        assertEquals("EUR", quotes["LQQ.PA"]?.currency)
        assertEquals(800.0, quotes["CSPX.L"]?.price ?: 0.0, 0.001)
        assertEquals("USD", quotes["CSPX.L"]?.currency)
    }

    @Test
    fun fetchEurRate_returnsNullOnFailureInsteadOfFixedRate() = runTest {
        val service = YahooFinanceQuoteService(httpExecutor = { throw java.io.IOException("offline") })
        assertNull(service.fetchEurRate("USD"))
        assertNull(service.fetchEurRate("usd"))
    }

    @Test
    fun fetchQuotes_skipsInvalidTickersWithoutRequest() = runTest {
        val requested = mutableListOf<String>()
        val service = YahooFinanceQuoteService(httpExecutor = { url -> requested.add(url); sampleLqqJson })
        val quotes = service.fetchQuotes(listOf("LQQ.PA/../x", "A?b=c", "LQQ.PA"))
        assertEquals(setOf("LQQ.PA"), quotes.keys)
        assertEquals(1, requested.size)
    }

    @Test
    fun chartUrl_encodesSymbolAndStaysOnAllowedHost() {
        val url = YahooFinanceQuoteService.chartUrl("EURUSD=X")
        assertEquals("https://query1.finance.yahoo.com/v8/finance/chart/EURUSD%3DX?interval=1d&range=1d", url)
        assertTrue(SafeHttp.isAllowedUrl(url))
    }

    @Test
    fun tickerPolicy_acceptsRealTickersAndRejectsInjection() {
        assertEquals("LQQ.PA", TickerPolicy.normalize(" lqq.pa "))
        assertEquals("^GSPC", TickerPolicy.normalize("^GSPC"))
        assertEquals("BRK-B", TickerPolicy.normalize("brk-b"))
        assertNull(TickerPolicy.normalize("LQQ.PA?x=1"))
        assertNull(TickerPolicy.normalize("../etc"))
        assertNull(TickerPolicy.normalize("A B"))
        assertNull(TickerPolicy.normalize(""))
    }

    @Test
    fun safeHttp_rejectsOtherHostsSchemesAndPorts() {
        assertFalse(SafeHttp.isAllowedUrl("http://query1.finance.yahoo.com/x"))
        assertFalse(SafeHttp.isAllowedUrl("https://evil.example/x"))
        assertFalse(SafeHttp.isAllowedUrl("https://query1.finance.yahoo.com:8443/x"))
        assertFalse(SafeHttp.isAllowedUrl("https://user@query1.finance.yahoo.com/x"))
        assertTrue(SafeHttp.isAllowedUrl("https://api.github.com/repos/x"))
    }

    @Test(expected = java.io.IOException::class)
    fun safeHttp_readLimitedRejectsOversizedBody() {
        SafeHttp.readLimited(java.io.ByteArrayInputStream(ByteArray(2048)), 1024)
    }
}
