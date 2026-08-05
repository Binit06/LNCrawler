package com.halovoid.lncrawler.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a novel in the database.
 * Part of the Data layer local storage schema.
 */
@Entity(tableName = "novels")
data class NovelEntity(
    /** The unique landing page URL of the novel, used as the primary key. */
    @PrimaryKey val url: String,
    /** The title of the novel. */
    val title: String,
    /** The author of the novel. */
    val author: String?,
    /** URL to the cover image. */
    val coverUrl: String?,
    /** Summary of the novel. */
    val description: String?,
    /** The name of the crawler used to fetch this novel. */
    val crawlerName: String
)
