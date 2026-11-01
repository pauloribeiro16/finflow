package com.financas.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.financas.app.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE yearMonth = :yearMonth ORDER BY epochDay DESC, id DESC")
    fun getByMonth(yearMonth: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE yearMonth = :yearMonth ORDER BY epochDay DESC, id DESC")
    suspend fun getByMonthSync(yearMonth: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE yearMonth = :yearMonth AND type = :type ORDER BY epochDay DESC, id DESC")
    fun getByMonthAndType(yearMonth: Int, type: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE yearMonth BETWEEN :startYearMonth AND :endYearMonth ORDER BY yearMonth ASC, epochDay ASC")
    fun getByYearMonthRange(startYearMonth: Int, endYearMonth: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE yearMonth BETWEEN :startYearMonth AND :endYearMonth ORDER BY yearMonth ASC, epochDay ASC")
    suspend fun getByYearMonthRangeSync(startYearMonth: Int, endYearMonth: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY epochDay DESC, id DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY epochDay DESC, id DESC")
    suspend fun getAllSync(): List<TransactionEntity>

    @Query("SELECT DISTINCT merchant FROM transactions WHERE merchant != '' ORDER BY merchant ASC")
    fun getDistinctMerchants(): Flow<List<String>>

    @Query("SELECT * FROM transactions WHERE LOWER(merchant) = LOWER(:merchant) ORDER BY epochDay DESC, id DESC LIMIT 1")
    suspend fun getLatestByMerchant(merchant: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE yearMonth = :yearMonth AND isFixed = 1")
    suspend fun getFixedTransactionsForMonth(yearMonth: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE linkedTransactionId = :parentTxId")
    suspend fun getRefundsForTransaction(parentTxId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE type = 'EXPENSE' ORDER BY epochDay DESC LIMIT :limit")
    suspend fun getRecentExpensesSync(limit: Int = 20): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int
}
