package com.halovoid.lncrawler.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "export_history")
data class ExportRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val novelUrl: String,
    val novelTitle: String,
    val status: String, //PENDING, SUCCESS, FAILURE
    val destinationUri: String?,
    val errorLog: String?,
    val crawlerName: String,
    val timestamp: Long = System.currentTimeMillis()
)
