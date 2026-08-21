package com.halovoid.lncrawler.domain.models

/**
 * Data model for a chapter.
 * 
 * NOTE: To maintain binary compatibility with external crawler DEX bundles,
 * the primary constructor must keep its original 7 parameters.
 * New fields like [sourceUrl] are added as regular properties.
 */
data class Chapter(
    val id: Int,
    val url: String,
    val title: String,
    val index: Int,
    val novelUrl: String,
    val volumeId: String,
    val fileLocation: String?
) {
    var sourceUrl: String? = null
}
