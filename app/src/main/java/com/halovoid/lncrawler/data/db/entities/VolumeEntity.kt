package com.halovoid.lncrawler.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "volumes",
    foreignKeys = [
        ForeignKey(
            entity = NovelEntity::class,
            parentColumns = ["url"],
            childColumns = ["novelUrl"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("novelUrl")
    ]
)
data class VolumeEntity (
    @PrimaryKey
    val id: String,

    val volumeIndex: Int,
    val novelUrl: String
)