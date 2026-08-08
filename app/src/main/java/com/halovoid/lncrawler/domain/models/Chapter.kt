package com.halovoid.lncrawler.domain.models

import com.halovoid.lncrawler.data.db.entities.ChapterEntity

/**
 * Domain model representing a single chapter of a novel.
 */
data class Chapter(
    val id: Int,
    /** The source URL of the chapter page. */
    val url: String,
    /** The title of the chapter (e.g., "ChapterDao 1: The Beginning"). */
    val title: String,
    /** The zero-based index of the chapter in the novel's sequence. */
    val index: Int,

    val novelUrl: String,

    val volumeId: String,

    val fileLocation: String?
)

fun ChapterEntity.toDomain(): Chapter = Chapter(
    id = id,
    url = url,
    title = title,
    index = index,
    novelUrl = novelUrl,
    volumeId = volumeId,
    fileLocation = fileLocation
)

fun Chapter.toEntity(): ChapterEntity = ChapterEntity(
    id = id,
    url = url,
    title = title,
    index = index,
    novelUrl = novelUrl,
    volumeId = volumeId,
    fileLocation = fileLocation
)