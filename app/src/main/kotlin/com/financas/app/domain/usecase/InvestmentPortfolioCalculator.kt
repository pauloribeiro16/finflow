package com.financas.app.domain.usecase

import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.domain.model.AssetAllocationItem
import com.financas.app.domain.model.InvestmentType
import com.financas.app.domain.model.PortfolioSummary

class InvestmentPortfolioCalculator {

    fun calculate(assets: List<InvestmentAssetEntity>): PortfolioSummary {
        val activeAssets = assets.filter { !it.archived }
        if (activeAssets.isEmpty()) {
            return PortfolioSummary()
        }

        val totalInvested = activeAssets.sumOf { it.investedCents }
        val totalCurrentValue = activeAssets.sumOf { it.currentValueCents }
        val totalProfitLoss = totalCurrentValue - totalInvested
        val totalProfitLossPct = if (totalInvested <= 0L) {
            0.0
        } else {
            (totalProfitLoss.toDouble() / totalInvested.toDouble()) * 100.0
        }

        val totalMonthlyContribution = activeAssets.sumOf { it.monthlyContributionCents }

        val allocations = activeAssets
            .groupBy { InvestmentType.fromKey(it.type) }
            .map { (type, typeAssets) ->
                val typeTotal = typeAssets.sumOf { it.currentValueCents }
                val pct = if (totalCurrentValue <= 0L) {
                    0.0
                } else {
                    (typeTotal.toDouble() / totalCurrentValue.toDouble()) * 100.0
                }
                AssetAllocationItem(
                    type = type,
                    totalValueCents = typeTotal,
                    percentage = pct
                )
            }
            .sortedByDescending { it.totalValueCents }

        return PortfolioSummary(
            totalInvestedCents = totalInvested,
            totalCurrentValueCents = totalCurrentValue,
            totalProfitLossCents = totalProfitLoss,
            totalProfitLossPercentage = totalProfitLossPct,
            totalMonthlyContributionCents = totalMonthlyContribution,
            allocations = allocations,
            totalAssetsCount = activeAssets.size
        )
    }
}
