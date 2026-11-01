package com.financas.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.financas.app.data.entity.PendingTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {

    @Query("SELECT * FROM pending_transactions ORDER BY receivedTimestamp DESC")
    fun getAll(): Flow<List<PendingTransactionEntity>>

    @Query("SELECT * FROM pending_transactions ORDER BY receivedTimestamp DESC")
    suspend fun getAllSync(): List<PendingTransactionEntity>

    @Query("SELECT * FROM pending_transactions WHERE id = :id")
    suspend fun getById(id: Long): PendingTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pending: PendingTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pending: List<PendingTransactionEntity>)

    @Update
    suspend fun update(pending: PendingTransactionEntity)

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM pending_transactions")
    fun count(): Flow<Int>

    @Query("DELETE FROM pending_transactions")
    suspend fun clearAll()
}
