package com.financas.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("yearMonth"),
        Index("categoryId"),
        Index("contextTag"),
        Index("merchant"),
        Index("linkedTransactionId")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val amountCents: Long,
    val yearMonth: Int,
    val epochDay: Long,
    val categoryId: String,
    val contextTag: String,
    val merchant: String,
    val paymentMethod: String,
    val isFixed: Boolean = false,
    val linkedTransactionId: Long? = null
)
