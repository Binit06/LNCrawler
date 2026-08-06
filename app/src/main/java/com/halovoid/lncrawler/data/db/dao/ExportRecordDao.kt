package com.halovoid.lncrawler.data.db.dao

import androidx.room.*
import com.halovoid.lncrawler.data.db.entities.ExportRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExportRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ExportRecordEntity): Long

    @Update
    suspend fun update(record: ExportRecordEntity)

    @Query("SELECT * FROM export_history ORDER BY timestamp desc")
    fun getAllHistory(): Flow<List<ExportRecordEntity>>

    @Query("SELECT * FROM export_history WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): ExportRecordEntity?

    @Query("DELETE FROM export_history")
    suspend fun clearHistory()

    @Query("DELETE FROM export_history WHERE id = :id")
    suspend fun deleteById(id: Int)
}
