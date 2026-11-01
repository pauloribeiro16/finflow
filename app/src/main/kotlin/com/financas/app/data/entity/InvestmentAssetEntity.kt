package com.financas.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "investment_assets",
    indices = [
        Index("type"),
        Index("archived")
    ]
)
data class InvestmentAssetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    val institution: String,
    val investedCents: Long,
    val currentValueCents: Long,
    val monthlyContributionCents: Long = 0L,
    val targetCents: Long? = null,
    val notes: String = "",
    val lastUpdatedEpochDay: Long,
    val archived: Boolean = false
) {
    val profitLossCents: Long
        get() = currentValueCents - investedCents

    val profitLossPercentage: Double
        get() = if (investedCents <= 0L) {
            0.0
        } else {
            ((currentValueCents - investedCents).toDouble() / investedCents.toDouble()) * 100.0
        }
}
