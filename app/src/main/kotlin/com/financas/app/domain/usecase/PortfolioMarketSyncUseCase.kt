package com.financas.app.domain.usecase

import com.financas.app.data.db.InvestmentAssetDao
import com.financas.app.data.network.QuoteService
import com.financas.app.data.network.TickerPolicy
import com.financas.app.data.network.YahooQuote
import java.time.LocalDate
import kotlin.math.roundToLong

data class PortfolioSyncSummary(
    val updatedCount: Int,
    val totalCount: Int,
    val failedTickers: List<String> = emptyList(),
    val suspiciousTickers: List<String> = emptyList(),
    val totalEstimatedValueCents: Long = 0L,
    val totalProfitCents: Long = 0L
)

class PortfolioMarketSyncUseCase(
    private val investmentAssetDao: InvestmentAssetDao,
    private val quoteService: QuoteService
) {

    suspend fun syncAllActiveAssets(todayEpochDay: Long = LocalDate.now().toEpochDay()): PortfolioSyncSummary {
        val allAssets = investmentAssetDao.getAllSync()
        val syncableAssets = allAssets.filter { asset ->
            !asset.archived &&
                TickerPolicy.normalize(asset.ticker) != null &&
                asset.units.let { it != null && it.isFinite() && it > 0.0 && it <= MAX_UNITS }
        }

        if (syncableAssets.isEmpty()) {
            return PortfolioSyncSummary(
                updatedCount = 0,
                totalCount = allAssets.count { !it.archived }
            )
        }

        val tickers = syncableAssets.mapNotNull { TickerPolicy.normalize(it.ticker) }
        val quotes = quoteService.fetchQuotes(tickers)

        val rateCache = mutableMapOf<String, Double?>()
        suspend fun eurRate(currency: String): Double? =
            rateCache.getOrPut(currency) { quoteService.fetchEurRate(currency) }

        var updatedCount = 0
        val failedTickers = mutableListOf<String>()
        val suspiciousTickers = mutableListOf<String>()

        for (asset in syncableAssets) {
            val ticker = TickerPolicy.normalize(asset.ticker) ?: continue
            val units = asset.units ?: continue
            val priceInEur = quotes[ticker]?.let { priceInEur(it) { currency -> eurRate(currency) } }

            if (priceInEur == null) {
                failedTickers.add(ticker)
                continue
            }

            val estimatedValue = units * priceInEur * 100.0
            if (!estimatedValue.isFinite() || estimatedValue > MAX_VALUE_CENTS) {
                failedTickers.add(ticker)
                continue
            }
            val estimatedCurrentValueCents = estimatedValue.roundToLong()

            if (isImplausibleJump(asset.currentValueCents, estimatedCurrentValueCents)) {
                suspiciousTickers.add(ticker)
                continue
            }

            investmentAssetDao.update(
                asset.copy(
                    currentValueCents = estimatedCurrentValueCents,
                    lastUpdatedEpochDay = todayEpochDay
                )
            )
            updatedCount++
        }

        val refreshedAssets = investmentAssetDao.getAllSync().filter { !it.archived }
        return PortfolioSyncSummary(
            updatedCount = updatedCount,
            totalCount = syncableAssets.size,
            failedTickers = failedTickers,
            suspiciousTickers = suspiciousTickers,
            totalEstimatedValueCents = refreshedAssets.sumOf { it.currentValueCents },
            totalProfitCents = refreshedAssets.sumOf { it.profitLossCents }
        )
    }

    private suspend fun priceInEur(quote: YahooQuote, eurRate: suspend (String) -> Double?): Double? {
        if (!quote.price.isFinite() || quote.price <= 0.0) return null
        val (currency, price) = when (quote.currency) {
            "GBp", "GBX" -> "GBP" to quote.price / 100.0
            "ILA" -> "ILS" to quote.price / 100.0
            "ZAc" -> "ZAR" to quote.price / 100.0
            else -> quote.currency.uppercase() to quote.price
        }
        val rate = eurRate(currency) ?: return null
        if (!rate.isFinite() || rate <= 0.0) return null
        return price / rate
    }

    companion object {
        const val MAX_UNITS = 1_000_000_000.0
        const val MAX_VALUE_CENTS = 1e15
        const val MAX_DAILY_FACTOR = 3.0

        fun isImplausibleJump(previousCents: Long, newCents: Long): Boolean {
            if (previousCents <= 0L || newCents <= 0L) return false
            val ratio = newCents.toDouble() / previousCents.toDouble()
            return ratio > MAX_DAILY_FACTOR || ratio < 1.0 / MAX_DAILY_FACTOR
        }
    }
}
