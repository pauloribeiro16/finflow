package com.financas.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "budgets", indices = [Index("categoryId")])
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    val categoryId: String,
    val limitCents: Long,
    val isActive: Boolean = true
)
