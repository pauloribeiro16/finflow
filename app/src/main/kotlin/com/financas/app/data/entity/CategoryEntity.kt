package com.financas.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val parentId: String? = null,
    val name: String,
    val icon: String,
    val colorHex: Long,
    val type: String,
    val isDefaultFixed: Boolean = false,
    val sortOrder: Int = 0
)
