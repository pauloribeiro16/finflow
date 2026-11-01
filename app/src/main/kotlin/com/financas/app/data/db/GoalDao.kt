package com.financas.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.financas.app.data.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals ORDER BY startYearMonth ASC, name ASC")
    fun getAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE archived = 0 ORDER BY startYearMonth ASC, name ASC")
    fun getActive(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY startYearMonth ASC, name ASC")
    suspend fun getAllSync(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM goals")
    suspend fun count(): Int
}
