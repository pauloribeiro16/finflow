package com.financas.app.domain.usecase

import com.financas.app.data.db.InvestmentAssetDao
import com.financas.app.data.network.QuoteService
import java.time.LocalDate
import kotlin.math.roundToLong

data class PortfolioSyncSummary(
    val updatedCount: Int,
    val totalCount: Int,
    val failedTickers: List<String> = emptyList(),
    val totalEstimatedValueCents: Long = 0L,
    val totalProfitCents: Long = 0L
)

class PortfolioMarketSyncUseCase(
    private val investmentAssetDao: InvestmentAssetDao,
    private val quoteService: QuoteService
) {

    suspend fun syncAllActiveAssets(todayEpochDay: Long = LocalDate.now().toEpochDay()): PortfolioSyncSummary {
        val allAssets = investmentAssetDao.getAllSync()
        val syncableAssets = allAssets.filter { !it.archived && !it.ticker.isNullOrBlank() && (it.units ?: 0.0) > 0.0 }

        if (syncableAssets.isEmpty()) {
            return PortfolioSyncSummary(
                updatedCount = 0,
                totalCount = allAssets.count { !it.archived }
            )
        }

        val tickers = syncableAssets.mapNotNull { it.ticker }
        val quotes = quoteService.fetchQuotes(tickers)

        val hasUsd = quotes.values.any { it.currency == "USD" }
        val eurUsdRate = if (hasUsd) quoteService.fetchEurUsdRate() else 1.0

        var updatedCount = 0
        val failedTickers = mutableListOf<String>()

        for (asset in syncableAssets) {
            val ticker = asset.ticker ?: continue
            val quote = quotes[ticker]

            if (quote == null || quote.price <= 0.0) {
                failedTickers.add(ticker)
                continue
            }

            val priceInEur = if (quote.currency == "USD") {
                quote.price / eurUsdRate
            } else {
                quote.price
            }

            val units = asset.units ?: continue
            val estimatedCurrentValueCents = (units * priceInEur * 100.0).roundToLong()

            // Preserves investedCents (the purchase price) and updates estimated market value
            val updated = asset.copy(
                currentValueCents = estimatedCurrentValueCents,
                lastUpdatedEpochDay = todayEpochDay
            )

            investmentAssetDao.update(updated)
            updatedCount++
        }

        val refreshedAssets = investmentAssetDao.getAllSync().filter { !it.archived }
        val totalVal = refreshedAssets.sumOf { it.currentValueCents }
        val totalProfit = refreshedAssets.sumOf { it.profitLossCents }

        return PortfolioSyncSummary(
            updatedCount = updatedCount,
            totalCount = syncableAssets.size,
            failedTickers = failedTickers,
            totalEstimatedValueCents = totalVal,
            totalProfitCents = totalProfit
        )
    }
}
