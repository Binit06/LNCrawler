package com.halovoid.lncrawler.data.scheduler

data class RequestMetadata(
    val crawlerName: String? = null,
    val artifactFormat: String? = null,
    val volumeId: Int? = null,
    val chapterId: Int? = null,
    val format: String? = null
)