package com.financas.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.financas.app.data.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets ORDER BY categoryId ASC")
    fun getAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY categoryId ASC")
    fun getAllActive(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY categoryId ASC")
    suspend fun getAllSync(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM budgets")
    suspend fun count(): Int
}
