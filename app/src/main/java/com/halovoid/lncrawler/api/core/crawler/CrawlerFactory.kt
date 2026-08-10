package com.halovoid.lncrawler.api.core.crawler

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CrawlerFactory {
    private val staticCrawlers = listOf<Crawler>(
        // Add built-in crawlers here if any
    )

    private val disabledCrawlers = listOf<DisabledCrawler>(
        // Add disabled crawlers here
    )

    private var dynamicCrawlers = mutableListOf<Crawler>()
    
    private val _crawlersFlow = MutableStateFlow<List<Crawler>>(emptyList())
    val crawlersFlow: StateFlow<List<Crawler>> = _crawlersFlow.asStateFlow()

    init {
        _crawlersFlow.value = getCrawlers()
    }

    fun getCrawlers(): List<Crawler> = staticCrawlers + dynamicCrawlers

    fun getDisabledCrawlers(): List<DisabledCrawler> = disabledCrawlers

    fun getCrawler(name: String): Crawler? = getCrawlers().find { it.name == name }

    fun getCrawlerByUrl(url: String): Crawler? = getCrawlers().find { it.canHandle(url) }

    fun registerCrawlers(newCrawlers: List<Crawler>) {
        dynamicCrawlers.clear()
        dynamicCrawlers.addAll(newCrawlers)
        _crawlersFlow.value = getCrawlers()
    }
}
