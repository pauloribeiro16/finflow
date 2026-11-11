package com.financas.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class YahooQuote(
    val ticker: String,
    val price: Double,
    val currency: String,
    val changePercent: Double,
    val name: String? = null
)

interface QuoteService {
    suspend fun fetchQuotes(tickers: List<String>): Map<String, YahooQuote>
    suspend fun fetchEurUsdRate(): Double
}

class YahooFinanceQuoteService(
    private val httpExecutor: (String) -> String = ::defaultHttpFetch
) : QuoteService {

    override suspend fun fetchEurUsdRate(): Double = withContext(Dispatchers.IO) {
        try {
            val jsonStr = httpExecutor("https://query1.finance.yahoo.com/v8/finance/chart/EURUSD=X?interval=1d&range=1d")
            parseQuoteFromJson("EURUSD=X", jsonStr)?.price?.takeIf { it > 0.0 } ?: 1.10
        } catch (_: Exception) {
            1.10 // Safe default fallback if fx rate fails
        }
    }

    override suspend fun fetchQuotes(tickers: List<String>): Map<String, YahooQuote> = withContext(Dispatchers.IO) {
        if (tickers.isEmpty()) return@withContext emptyMap()

        val results = mutableMapOf<String, YahooQuote>()
        for (ticker in tickers.distinct()) {
            try {
                val url = "https://query1.finance.yahoo.com/v8/finance/chart/$ticker?interval=1d&range=1d"
                val jsonStr = httpExecutor(url)
                val quote = parseQuoteFromJson(ticker, jsonStr)
                if (quote != null && quote.price > 0.0) {
                    results[ticker] = quote
                }
            } catch (_: Exception) {
                // Ignore single ticker network error and proceed
            }
        }
        results
    }

    companion object {
        fun parseQuoteFromJson(ticker: String, jsonStr: String): YahooQuote? {
            return try {
                val root = JSONObject(jsonStr)
                val chart = root.getJSONObject("chart")
                val results = chart.optJSONArray("result") ?: return null
                if (results.length() == 0) return null
                val first = results.getJSONObject(0)
                val meta = first.getJSONObject("meta")

                val price = meta.optDouble("regularMarketPrice", 0.0)
                val currency = meta.optString("currency", "EUR")
                val changePercent = meta.optDouble("regularMarketChangePercent", 0.0)
                val name = meta.optString("shortName", meta.optString("symbol", ticker))

                YahooQuote(
                    ticker = ticker,
                    price = price,
                    currency = currency.uppercase(),
                    changePercent = changePercent,
                    name = name
                )
            } catch (_: Exception) {
                null
            }
        }

        private fun defaultHttpFetch(urlString: String): String {
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
            }
            return conn.inputStream.bufferedReader().use(BufferedReader::readText)
        }
    }
}
