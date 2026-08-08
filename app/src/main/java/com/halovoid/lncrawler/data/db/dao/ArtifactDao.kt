package com.halovoid.lncrawler.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.halovoid.lncrawler.data.db.entities.ArtifactEntity

@Dao
interface ArtifactDao {
    @Query("SELECT * FROM artifacts WHERE requestId = :id")
    fun getArtifactForRequest(id: String): ArtifactEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertArtifact(artifact: ArtifactEntity)

    @Delete
    fun removeArtifact(artifact: ArtifactEntity)
}