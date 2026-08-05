package com.halovoid.lncrawler.domain.models

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
    /** Unique identifier, typically used for database operations. */
    val id: String? = null,
    /** The name of the [com.halovoid.lncrawler.data.crawler.core.Crawler] that handled this novel. */
    val crawlerName: String? = null,
    /** Alternative names or titles for the novel. */
    val alternativeNames: String? = null
)
