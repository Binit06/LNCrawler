package com.halovoid.lncrawler.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a chapter in the database.
 * Associated with a [NovelEntity] via a foreign key on [novelUrl].
 */
@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = NovelEntity::class,
            parentColumns = ["url"],
            childColumns = ["novelUrl"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = VolumeEntity::class,
            parentColumns = ["id"],
            childColumns = ["volumeId"]
        )
    ],
    indices = [
        Index("novelUrl"),
        Index("volumeId")
    ]
)
data class ChapterEntity(
    /** Primary key for the chapter entity, auto-generated. */
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The URL of the parent novel, used as a foreign key. */
    val novelUrl: String,
    /** The Volume ID of the Volume it will be part of*/
    val volumeId: Int,
    /** The unique source URL of the chapter. */
    val url: String,
    /** The title of the chapter. */
    val title: String,
    /** The sequence index of the chapter. */
    val index: Int,
    /** Location of the Chapter Loaded into the memory*/
    val fileLocation: String?
)
