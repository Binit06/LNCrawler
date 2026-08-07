package com.halovoid.lncrawler.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ArtifactStatus {
    PENDING, SUCCESS, FAILED, CANCELLED
}

/**
 * Room entity representing an export request in the database.
 * Supports a parent-child relationship where a request can be linked to another.
 */
@Entity(
    tableName = "export_history",
    foreignKeys = [
        ForeignKey(
            entity = ArtifactRequestEntity::class,
            parentColumns = ["id"],
            childColumns = ["requestId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("requestId")]
)
data class ArtifactRequestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val requestId: Int? = null,
    val status: ArtifactStatus = ArtifactStatus.PENDING,
    val destinationUri: String?,
    val timestamp: Long = System.currentTimeMillis()
)
