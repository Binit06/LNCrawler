package com.halovoid.lncrawler.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artifact")
data class ArtifactEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val novelUrl: String,
    val requestId: Int,
    val artifactDestination: String,
    val artifactName: String
)