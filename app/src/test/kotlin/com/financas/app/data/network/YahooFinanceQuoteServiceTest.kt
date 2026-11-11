package com.financas.app.data.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
                url.contains("EURUSD=X") -> sampleEurUsdJson
                url.contains("LQQ.PA") -> sampleLqqJson
                url.contains("CSPX.L") -> sampleUsdJson
                else -> "{}"
            }
        }

        val service = YahooFinanceQuoteService(httpExecutor = mockFetcher)

        val fxRate = service.fetchEurUsdRate()
        assertEquals(1.10, fxRate, 0.001)

        val quotes = service.fetchQuotes(listOf("LQQ.PA", "CSPX.L"))
        assertEquals(2, quotes.size)
        assertEquals(8.53, quotes["LQQ.PA"]?.price ?: 0.0, 0.001)
        assertEquals("EUR", quotes["LQQ.PA"]?.currency)
        assertEquals(800.0, quotes["CSPX.L"]?.price ?: 0.0, 0.001)
        assertEquals("USD", quotes["CSPX.L"]?.currency)
    }
}
