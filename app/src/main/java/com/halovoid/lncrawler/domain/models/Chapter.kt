package com.halovoid.lncrawler.domain.models

/**
 * Domain model representing a single chapter of a novel.
 */
data class Chapter(
    /** The source URL of the chapter page. */
    val url: String,
    /** The title of the chapter (e.g., "Chapter 1: The Beginning"). */
    val title: String,
    /** The zero-based index of the chapter in the novel's sequence. */
    val index: Int
)
