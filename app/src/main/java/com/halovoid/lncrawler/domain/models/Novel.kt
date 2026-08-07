package com.halovoid.lncrawler.domain.models

import com.halovoid.lncrawler.data.db.entities.NovelEntity

/**
 * Domain model representing a novel with its metadata and chapters.
 * This class is used throughout the application to pass novel information between layers.
 */
data class Novel(
    /** The landing page URL of the novel on the source site. */
    val url: String,
    /** The title of the novel. */
    val title: String,
    /** The author of the novel, if available. */
    val author: String? = null,
    /** URL to the cover image of the novel. */
    val coverUrl: String? = null,
    /** A short summary or synopsis of the novel. */
    val description: String? = null,
    /** List of [Chapter]s associated with this novel. */
    val chapters: List<Chapter> = emptyList(),
    /** List of [Volume]s associated with this novel.*/
    val volumes: List<Volume> = emptyList(),
    /** The name of the [com.halovoid.lncrawler.data.crawler.core.crawler.Crawler] that handled this novel. */
    val crawlerName: String,
    /** Alternative names or titles for the novel. */
    val alternativeNames: String? = null
)

fun NovelEntity.toDomain(): Novel = Novel(
    url = url,
    title = title,
    author = author,
    coverUrl = coverUrl,
    description = description,
    crawlerName = crawlerName,
    alternativeNames = alternativeNames,
    chapters = emptyList(),
    volumes = emptyList()
)

fun Novel.toEntity() = NovelEntity(
    url = url,
    title = title,
    author = author,
    coverUrl = coverUrl,
    description = description,
    crawlerName = crawlerName,
    alternativeNames = alternativeNames
)