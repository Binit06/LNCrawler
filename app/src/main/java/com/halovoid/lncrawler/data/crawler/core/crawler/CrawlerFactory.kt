package com.halovoid.lncrawler.data.crawler.core.crawler

import com.halovoid.lncrawler.data.crawler.sources.NovelBinsCrawler

/**
 * Factory and registry for all available [Crawler] implementations.
 * Part of the Data layer, responsible for managing crawler instances.
 */
object CrawlerFactory {
    /** List of all supported crawler implementations. */
    private val crawlers = listOf(
        NovelBinsCrawler()
    )

    /**
     * Returns a list of all registered crawlers.
     * @return List of available [Crawler]s.
     */
    fun getCrawlers(): List<Crawler> = crawlers

    /**
     * Finds a crawler by its display name.
     * @param name The name of the crawler to find.
     * @return The matching [Crawler] or null if not found.
     */
    fun getCrawler(name: String): Crawler? = crawlers.find { it.name == name }

    /**
     * Automatically selects a crawler that can handle the given URL.
     * Uses [Crawler.canHandle] to determine compatibility.
     * @param url The URL to match against crawlers.
     * @return A compatible [Crawler] or null if no crawler supports the URL.
     */
    fun getCrawlerByUrl(url: String): Crawler? = crawlers.find { it.canHandle(url) }
}