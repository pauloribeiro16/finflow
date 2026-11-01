package com.financas.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.financas.app.data.entity.InvestmentAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentAssetDao {

    @Query("SELECT * FROM investment_assets ORDER BY currentValueCents DESC, name ASC")
    fun getAll(): Flow<List<InvestmentAssetEntity>>

    @Query("SELECT * FROM investment_assets WHERE archived = 0 ORDER BY currentValueCents DESC, name ASC")
    fun getActive(): Flow<List<InvestmentAssetEntity>>

    @Query("SELECT * FROM investment_assets ORDER BY name ASC")
    suspend fun getAllSync(): List<InvestmentAssetEntity>

    @Query("SELECT * FROM investment_assets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): InvestmentAssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: InvestmentAssetEntity)

    @Update
    suspend fun update(asset: InvestmentAssetEntity)

    @Query("UPDATE investment_assets SET currentValueCents = :newValueCents, lastUpdatedEpochDay = :epochDay WHERE id = :id")
    suspend fun updateCurrentValue(id: String, newValueCents: Long, epochDay: Long)

    @Query("DELETE FROM investment_assets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM investment_assets")
    suspend fun count(): Int
}
