package com.halovoid.lncrawler.api.core.crawler

import com.halovoid.lncrawler.api.sources.NovelBins

object CrawlerFactory {
    private val crawlers = listOf(
        NovelBins()
    )

    private val disabledCrawlers = listOf<DisabledCrawler>(
        // Add disabled crawlers here
    )

    fun getCrawlers(): List<Crawler> = crawlers

    fun getDisabledCrawlers(): List<DisabledCrawler> = disabledCrawlers

    fun getCrawler(name: String): Crawler? = crawlers.find { it.name == name }

    fun getCrawlerByUrl(url: String): Crawler? = crawlers.find { it.canHandle(url) }
}