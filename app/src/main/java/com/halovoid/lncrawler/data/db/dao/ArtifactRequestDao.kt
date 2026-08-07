package com.halovoid.lncrawler.data.db.dao

import androidx.room.*
import com.halovoid.lncrawler.data.db.entities.ArtifactRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtifactRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ArtifactRequestEntity): Long

    @Update
    suspend fun update(record: ArtifactRequestEntity)

    @Query("SELECT * FROM export_history ORDER BY timestamp desc")
    fun getAllHistory(): Flow<List<ArtifactRequestEntity>>

    @Query("SELECT * FROM export_history WHERE id = :id LIMIT 1")
    fun getRecordById(id: Long): Flow<ArtifactRequestEntity?>

    @Query("SELECT * FROM export_history WHERE id = :id LIMIT 1")
    suspend fun getRecordByIdOnce(id: Long): ArtifactRequestEntity?

    @Query("DELETE FROM export_history")
    suspend fun clearHistory()

    @Query("DELETE FROM export_history WHERE id = :id")
    suspend fun deleteById(id: Int)
}
