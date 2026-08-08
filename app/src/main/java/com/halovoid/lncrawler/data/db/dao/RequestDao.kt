package com.halovoid.lncrawler.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RequestDao {
    @Query("SELECT * FROM requests")
    fun getAllRequests(): Flow<List<RequestEntity>>

    @Query("SELECT * FROM requests WHERE dependsOn = -1")
    fun getRootRequests(): Flow<List<RequestEntity>>

    @Query("SELECT * FROM requests WHERE id = :id")
    suspend fun getRequestById(id: String) : RequestEntity

    @Query("SELECT * FROM requests WHERE dependsOn = :id")
    suspend fun getRequestsByDependence(id: String) : List<RequestEntity>

    @Query("DELETE FROM requests WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE requests SET progressCurrent = progressCurrent + 1 WHERE id = :id")
    suspend fun incrementProgress(id: String)

    @Query("UPDATE requests SET progressTotal = :total WHERE id = :id")
    suspend fun updateProgressTotal(id: String, total: Int)

    /**
     * Propagates the progress down the tree so that each data node has its own correct progress bar
     */
    @Transaction
    suspend fun propagateProgress(childRequestId: String) {
        val child = getRequestById(childRequestId) ?: return
        val parentId = child.dependsOn ?: return

        incrementProgress(parentId)

        propagateProgress(parentId)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequests(request: List<RequestEntity>)

    @Update
    suspend fun updateRequest(request: RequestEntity)
}