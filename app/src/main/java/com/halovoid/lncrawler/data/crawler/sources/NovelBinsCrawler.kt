package com.halovoid.lncrawler.data.crawler.sources

import android.util.Log
import com.halovoid.lncrawler.data.crawler.core.Crawler
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.Novel
import okhttp3.FormBody
import org.jsoup.Jsoup
import java.io.IOException

/**
 * Crawler implementation for NovelBin (novelbins.com) in the Data layer.
 * This class handles the specific HTML structure and AJAX endpoints of the site
 * using Jsoup for static parsing and custom logic for paginated chapter lists.
 */
class NovelBinsCrawler : Crawler() {
    override val name: String = "NovelBin"
    override val baseUrl: String = "https://novelbins.com"

    override val requestRateLimit: Double
        get() = 1.0

    override val chapterBatchSize: Int
        get() = 2

    override fun canHandle(url: String): Boolean {
        return url.contains("novelbins.com") || url.contains("novelbin.com")
    }

    override suspend fun getNovelDetails(novelUrl: String): Novel {
        val doc = getDocument(novelUrl) ?: throw IOException("Failed to fetch novel details from $novelUrl")
        Log.i(name, "Scraping novel: $novelUrl")
        
        val title = doc.select(".novel-short-info h1").first()?.text()?.split("\n")?.first() ?: ""
        val author = doc.select(".novel-short-info p:contains(Author:)").text().replace("Author: ", "").trim()
        val coverUrl = doc.select("img.novel-photo").attr("abs:src")
        val description = doc.select(".novel-short-info p")[7]?.text() ?: ""
        
        // Novel ID extraction for AJAX chapter list
        // Try getting it from the URL slug first (e.g., solo-leveling-2750127 -> 2750127)
        val permalink = novelUrl.removeSuffix("/").split("/").last()
        var novelId = permalink.split("-").lastOrNull { it.all { c -> c.isDigit() } } ?: ""
        
        // Fallback: extract from bookmark link: javascript:bookmark('107187','1')
        if (novelId.isEmpty()) {
            val bookmarkLink = doc.select("a[href^='javascript:bookmark']").attr("href")
            novelId = bookmarkLink.substringAfter("'").substringBefore("'")
        }
        
        Log.i(name, "Internal ID: $novelId, Permalink: $permalink")
        
        if (novelId.isEmpty()) {
            Log.e(name, "Could not extract Novel ID from $novelUrl")
        }

        val chapters = mutableListOf<Chapter>()
        val tabLinks = doc.select("a.ch[data-toggle='tab']")
        Log.i("TAB", "${tabLinks.size}")
        
        if (tabLinks.isEmpty()) {
            // Fallback for simple pages
            doc.select(".chapters .mt-card-item h3.mt-card-name a").forEachIndexed { index, element ->
                chapters.add(Chapter(url = element.attr("abs:href"), title = element.text(), index = index))
            }
        } else {
            // Paginated chapter lists via AJAX
            tabLinks.forEach { tabLink ->
                val tabIndex = tabLink.attr("href").replace("#", "")
                val ajaxChapters = fetchChaptersViaAjax(novelId, tabIndex, permalink, novelUrl)
                chapters.addAll(ajaxChapters)
            }
        }

        // Clean up duplicate entries and set final indices
        val finalChapters = chapters.distinctBy { it.url }.mapIndexed { index, chapter ->
            chapter.copy(index = index)
        }

        return Novel(
            url = novelUrl,
            title = title,
            author = author,
            coverUrl = coverUrl,
            description = description,
            chapters = finalChapters,
            crawlerName = name
        )
    }

    override suspend fun getChapterContent(chapterUrl: String): String {
        val doc = getDocument(chapterUrl) ?: return ""
        Log.i(name, "Scraping chapter: $chapterUrl")
        
        // Use base class cleaning with site-specific selectors
        val content = cleanHtml(doc, ".reader, #chr-content, #chapter-content")
        
        // Extra site-specific cleaning for sharing links
        return Jsoup.parse(content).apply {
            select("a[href*='novelbin'], a[href*='facebook'], a[href*='twitter']").remove()
        }.body().html().trim()
    }

    /**
     * Fetches chapter data from NovelBin's internal AJAX API.
     * Used because the main novel page only shows a subset of chapters.
     * 
     * @param novelId Internal numeric ID used by the site.
     * @param tab The tab index or pagination identifier.
     * @param permalink The URL-friendly name of the novel.
     * @param refererUrl The URL of the novel landing page to be used as Referer.
     * @return A list of [Chapter]s fetched from the API.
     */
    private suspend fun fetchChaptersViaAjax(novelId: String, tab: String, permalink: String, refererUrl: String): List<Chapter> {
        val url = "$baseUrl/ajax/"
        val requestBody = FormBody.Builder()
            .add("action", "get_chapters")
            .add("id", novelId)
            .add("tab", tab)
            .build()

        val html = fetchHtml(
            url = url, 
            headers = mapOf(
                "X-Requested-With" to "XMLHttpRequest",
                "Referer" to refererUrl,
                "Origin" to baseUrl,
                "Accept" to "application/json, text/javascript, */*; q=0.01",
                "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8"
            ),
            body = requestBody
        ) ?: return emptyList()

        Log.i("AJAX", html)
        
        val chapters = mutableListOf<Chapter>()
        try {
            val jsonArray = org.json.JSONArray(html)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val chapterNum = obj.getString("chapter")
                val title = obj.getString("title")
                chapters.add(Chapter(
                    url = "$baseUrl/novel/$permalink/chapter/$chapterNum/",
                    title = title,
                    index = 0 
                ))
            }
        } catch (e: Exception) {
            Log.e(name, "Error parsing AJAX response", e)
        }
        return chapters
    }
}
