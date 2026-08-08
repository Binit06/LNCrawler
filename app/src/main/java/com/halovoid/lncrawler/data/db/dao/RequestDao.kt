package com.halovoid.lncrawler.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.db.entities.RequestStatus
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

    @Query("""
        UPDATE requests
        SET
            progressSuccess = (SELECT COUNT(*) FROM requests WHERE dependsOn = :parentId AND status = 'SUCCESS'),
            progressFailed = (SELECT COUNT(*) FROM requests WHERE dependsOn = :parentId AND status = 'FAILED'),
            progressCancelled = (SELECT COUNT(*) FROM requests WHERE dependsOn = :parentId AND status = 'CANCELLED')
        WHERE id = :parentId
    """)
    suspend fun syncProgress(parentId: String)

    @Query("UPDATE requests SET progressTotal = :total WHERE id = :id")
    suspend fun updateProgressTotal(id: String, total: Int)

    @Transaction
    suspend fun cancelRequest(requestId: String) {
        val request = getRequestById(requestId) ?: return

        if (request.status == RequestStatus.PENDING || request.status == RequestStatus.RUNNING) {
            updateRequest(request.copy(
                status = RequestStatus.CANCELLED,
                updatedAt = System.currentTimeMillis()
            ))
            propagateProgress(request.id)
        }

        val dependents = getRequestsByDependence(requestId)
        for (child in dependents) {
            cancelRequest(child.id)
        }
    }

    /**
     * Propagates the progress down the tree so that each data node has its own correct progress bar
     */
    @Transaction
    suspend fun propagateProgress(childRequestId: String) {
        val child = getRequestById(childRequestId) ?: return
        val parentId = child.dependsOn ?: return

        syncProgress(parentId)

        propagateProgress(parentId)
    }

    @Transaction
    suspend fun replayRequest(requestId: String) {
        // 1. Reset the request
        val request = getRequestById(requestId) ?: return
        updateRequest(request.copy(
            status = RequestStatus.PENDING,
            progressSuccess = 0,
            progressFailed = 0,
            progressCancelled = 0,
            error = null,
            completedAt = null,
            updatedAt = System.currentTimeMillis()
        ))

        // 2. Reset all children that depend on this request
        val dependents = getRequestsByDependence(requestId)
        for(dep in dependents) {
            replayRequest(dep.id)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequests(request: List<RequestEntity>)

    @Update
    suspend fun updateRequest(request: RequestEntity)
}