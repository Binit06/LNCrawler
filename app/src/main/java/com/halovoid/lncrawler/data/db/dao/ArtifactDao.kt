package com.halovoid.lncrawler.data.db.dao

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.halovoid.lncrawler.data.db.entities.ArtifactEntity

interface ArtifactDao {
    @Query("SELECT * FROM artifact WHERE requestId = :id")
    fun getArtifactForRequest(id: Int): ArtifactEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertArtifact(artifact: ArtifactEntity)
}