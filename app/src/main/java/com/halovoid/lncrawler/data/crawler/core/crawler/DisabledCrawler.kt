package com.halovoid.lncrawler.data.crawler.core.crawler

/**
 * Represents a crawler that is currently disabled and the reason why.
 */
data class DisabledCrawler(
    val crawler: Crawler,
    val reason: String
)
