package com.financas.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_transactions",
    indices = [
        Index("receivedTimestamp")
    ]
)
data class PendingTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountCents: Long,
    val rawMerchant: String,
    val suggestedType: String = "EXPENSE",
    val suggestedCategoryId: String,
    val suggestedContextTag: String,
    val suggestedPaymentMethod: String,
    val isFixed: Boolean = false,
    val sourceApp: String,
    val detectedEpochDay: Long,
    val receivedTimestamp: Long,
    val notes: String = ""
)
