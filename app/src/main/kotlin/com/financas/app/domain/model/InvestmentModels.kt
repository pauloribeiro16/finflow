package com.financas.app.domain.model

enum class InvestmentType(val key: String, val label: String) {
    PPR("PPR", "PPR"),
    DEPOSITO_PRAZO("DEPOSITO_PRAZO", "Depósito a Prazo"),
    ETF("ETF", "ETF"),
    CERTIFICADOS_AFORRO("CERTIFICADOS_AFORRO", "Certificados de Aforro"),
    CRYPTO("CRYPTO", "Cripto"),
    OUTRO("OUTRO", "Outro");

    companion object {
        fun fromKey(key: String): InvestmentType {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: OUTRO
        }
    }
}

data class AssetAllocationItem(
    val type: InvestmentType,
    val totalValueCents: Long,
    val percentage: Double
)

data class PortfolioSummary(
    val totalInvestedCents: Long = 0L,
    val totalCurrentValueCents: Long = 0L,
    val totalProfitLossCents: Long = 0L,
    val totalProfitLossPercentage: Double = 0.0,
    val totalMonthlyContributionCents: Long = 0L,
    val allocations: List<AssetAllocationItem> = emptyList(),
    val totalAssetsCount: Int = 0
)
