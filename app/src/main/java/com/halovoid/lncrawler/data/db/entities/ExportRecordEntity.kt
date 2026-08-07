package com.halovoid.lncrawler.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an export request in the database.
 * Supports a parent-child relationship where a request can be linked to another.
 */
@Entity(
    tableName = "export_history",
    foreignKeys = [
        ForeignKey(
            entity = ExportRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentId")]
)
data class ExportRecordEntity(
    /** The unique database ID of the export record. */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    /** Optional reference to a parent request ID. NULL for root requests. */
    val parentId: Int? = null,
    /** The landing page URL of the novel. */
    val novelUrl: String,
    /** The title of the novel. */
    val novelTitle: String,
    /** The current status: PENDING, SUCCESS, FAILED, or CANCELLED. */
    val status: String,
    /** The target URI for the exported file. */
    val destinationUri: String?,
    /** Log information or error messages if the export failed. */
    val errorLog: String?,
    /** The name of the crawler used. */
    val crawlerName: String,
    /** Timestamp when the record was created. */
    val timestamp: Long = System.currentTimeMillis()
)
