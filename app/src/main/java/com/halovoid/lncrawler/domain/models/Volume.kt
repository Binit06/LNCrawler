package com.halovoid.lncrawler.domain.models

import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.db.entities.VolumeEntity

data class Volume (
    val id: String,
    val volumeIndex: Int,
    val novelUrl: String
)

fun VolumeEntity.toDomain(): Volume = Volume(
    id = id,
    volumeIndex = volumeIndex,
    novelUrl = novelUrl
)
fun Volume.toEntity(): VolumeEntity = VolumeEntity(
    id = id,
    volumeIndex = volumeIndex,
    novelUrl = novelUrl
)