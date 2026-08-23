package com.halovoid.lncrawler.data.repository

import android.content.Context
import androidx.core.net.toUri
import com.halovoid.lncrawler.api.core.crawler.CrawlerFactory
import com.halovoid.lncrawler.domain.models.Chapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class ReaderRepository(
    private val context: Context
) {
    suspend fun getChapterContent(chapter: Chapter, crawlerName: String): List<String> =
        withContext(Dispatchers.IO) {
            val html = readDownloaded(chapter.fileLocation) ?: fetchLive(chapter, crawlerName)
            html?.let { extractParagraphs(it) }
                ?:listOf("Couldn't load this chapter. Check your connection and try again")
        }
    private fun readDownloaded(fileLocation: String?): String? {
        if (fileLocation.isNullOrBlank() || !fileLocation.startsWith("content://")) return null
        return try {
            context.contentResolver.openInputStream(fileLocation.toUri())
                ?.bufferedReader()
                ?.use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchLive(chapter: Chapter, crawlerName: String): String? {
        val crawler = CrawlerFactory.getCrawler(crawlerName) ?: return null
        val url = chapter.sourceUrl?.takeIf { it.isNotBlank() } ?: chapter.url
        return crawler.getChapterContent(url)
    }

    private fun extractParagraphs(html: String): List<String> {
        val body = Jsoup.parseBodyFragment(html).body()

        val paragraph = body.select("p")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }

        if (paragraph.isNotEmpty()) return paragraph

        body.select("br").forEach { it.after("\n") }
        return body.text()
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}