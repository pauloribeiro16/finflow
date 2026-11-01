package com.financas.app.domain.model

data class XtbOpenPosition(
    val ticker: String,
    val name: String,
    val category: String,
    val volume: Double,
    val currentValueCents: Long,
    val investedCents: Long,
    val profitLossCents: Long,
    val profitLossPercentage: Double
)

data class XtbCashOperation(
    val id: String,
    val type: String,
    val instrument: String,
    val ticker: String,
    val category: String,
    val amountCents: Long,
    val epochDay: Long,
    val yearMonth: Int,
    val comment: String
)

data class XtbParsedReport(
    val openPositions: List<XtbOpenPosition>,
    val cashOperations: List<XtbCashOperation>,
    val reportDateEpochDay: Long? = null,
    val openPositionValueCents: Long = 0L,
    val openPositionProfitCents: Long = 0L
)

data class XtbSyncResult(
    val updatedAssetsCount: Int,
    val newAssetsCount: Int,
    val importedTransactionsCount: Int,
    val skippedTransactionsCount: Int,
    val totalPositionsCount: Int
)
