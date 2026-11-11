package com.financas.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder

data class YahooQuote(
    val ticker: String,
    val price: Double,
    val currency: String,
    val changePercent: Double,
    val name: String? = null
)

interface QuoteService {
    suspend fun fetchQuotes(tickers: List<String>): Map<String, YahooQuote>

    /** Unidades de [currency] por 1 EUR, ou null se não for possível obter a taxa. */
    suspend fun fetchEurRate(currency: String): Double?
}

object TickerPolicy {
    private val TICKER_REGEX = Regex("^[A-Z0-9^][A-Z0-9.=^-]{0,19}$")

    fun normalize(raw: String?): String? {
        val trimmed = raw?.trim()?.uppercase() ?: return null
        return trimmed.takeIf { TICKER_REGEX.matches(it) }
    }

    fun isValid(ticker: String): Boolean = TICKER_REGEX.matches(ticker)
}

class YahooFinanceQuoteService(
    private val httpExecutor: (String) -> String = { url -> SafeHttp.getText(url, accept = "application/json") }
) : QuoteService {

    override suspend fun fetchEurRate(currency: String): Double? = withContext(Dispatchers.IO) {
        if (currency == "EUR") return@withContext 1.0
        if (!CURRENCY_REGEX.matches(currency)) return@withContext null
        try {
            val symbol = "EUR$currency=X"
            parseQuoteFromJson(symbol, httpExecutor(chartUrl(symbol)))
                ?.price
                ?.takeIf { it.isFinite() && it > 0.0 }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun fetchQuotes(tickers: List<String>): Map<String, YahooQuote> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, YahooQuote>()
        for (ticker in tickers.distinct()) {
            if (!TickerPolicy.isValid(ticker)) continue
            try {
                val quote = parseQuoteFromJson(ticker, httpExecutor(chartUrl(ticker)))
                if (quote != null && quote.price.isFinite() && quote.price > 0.0) {
                    results[ticker] = quote
                }
            } catch (_: Exception) {
            }
        }
        results
    }

    companion object {
        private val CURRENCY_REGEX = Regex("^[A-Z]{3}$")

        fun chartUrl(symbol: String): String =
            "https://query1.finance.yahoo.com/v8/finance/chart/" +
                URLEncoder.encode(symbol, Charsets.UTF_8.name()) +
                "?interval=1d&range=1d"

        fun parseQuoteFromJson(ticker: String, jsonStr: String): YahooQuote? {
            return try {
                val root = JSONObject(jsonStr)
                val chart = root.getJSONObject("chart")
                val results = chart.optJSONArray("result") ?: return null
                if (results.length() == 0) return null
                val meta = results.getJSONObject(0).getJSONObject("meta")

                val price = meta.optDouble("regularMarketPrice", 0.0)
                val currency = meta.optString("currency", "").trim()
                if (currency.isEmpty()) return null
                val changePercent = meta.optDouble("regularMarketChangePercent", 0.0)
                val name = meta.optString("shortName", meta.optString("symbol", ticker)).take(120)

                YahooQuote(
                    ticker = ticker,
                    price = price,
                    currency = currency,
                    changePercent = if (changePercent.isFinite()) changePercent else 0.0,
                    name = name
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
