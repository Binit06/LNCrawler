package com.halovoid.lncrawler.api.core.crawler

object CrawlerFactory {
    private var dynamicCrawlers = mutableListOf<Crawler>()

    private val staticCrawlers = listOf<Crawler>(
        // Add built-in crawlers here if any
    )

    private val disabledCrawlers = listOf<DisabledCrawler>(
        // Add disabled crawlers here
    )

    fun getCrawlers(): List<Crawler> = staticCrawlers + dynamicCrawlers

    fun getDisabledCrawlers(): List<DisabledCrawler> = disabledCrawlers

    fun getCrawler(name: String): Crawler? = getCrawlers().find { it.name == name }

    fun getCrawlerByUrl(url: String): Crawler? = getCrawlers().find { it.canHandle(url) }

    fun registerCrawlers(newCrawlers: List<Crawler>) {
        dynamicCrawlers.clear()
        dynamicCrawlers.addAll(newCrawlers)
    }
}
