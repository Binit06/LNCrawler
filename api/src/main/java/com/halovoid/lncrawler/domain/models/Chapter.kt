package com.halovoid.lncrawler.domain.models

data class Chapter(
    val id: Int,
    val url: String,
    val title: String,
    val index: Int,
    val novelUrl: String,
    val volumeId: String,
    val fileLocation: String?
)