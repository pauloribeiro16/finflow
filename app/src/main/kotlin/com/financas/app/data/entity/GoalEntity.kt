package com.financas.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val targetCents: Long,
    val startYearMonth: Int,
    val deadlineYearMonth: Int? = null,
    val archived: Boolean = false
) {
    companion object {
        fun deterministicId(name: String, startYearMonth: Int): String {
            val slug = name.trim()
                .lowercase()
                .replace(" ", "_")
                .filter { it.isLetterOrDigit() || it == '_' }
                .trim('_')
            return "goal_${if (slug.isEmpty()) "meta" else slug}_$startYearMonth"
        }
    }
}
