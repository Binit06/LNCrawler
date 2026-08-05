package com.halovoid.lncrawler.data.crawler.core

import android.util.Log
import com.halovoid.lncrawler.domain.models.Novel
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Base abstract class for all source crawlers in the Data layer.
 * Inspired by the logic in lightnovel-crawler (Python).
 * 
 * Provides utility methods for fetching HTML, resolving absolute URLs, 
 * and defines the interface for site-specific implementations using Jsoup for scraping.
 */
abstract class Crawler {
    /** The display name of the source (e.g., "NovelBin") */
    abstract val name: String
    
    /** The base URL of the source (e.g., "https://novelbins.com") */
    abstract val baseUrl: String

    /** Language of the novels on this site (e.g., "en") */
    open val language: String = "en"
    
    /** Request rate limit in seconds between requests */
    open val requestRateLimit: Double = 1.0

    /** Internal HTTP client with session (cookie) support */
    protected val client = OkHttpClient.Builder()
        .cookieJar(object : okhttp3.CookieJar {
            private val cookieStore = mutableMapOf<String, List<okhttp3.Cookie>>()
            
            override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<okhttp3.Cookie>) {
                cookieStore[url.host] = cookies
            }
            
            override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> {
                return cookieStore[url.host] ?: emptyList()
            }
        })
        .build()

    /**
     * Determines if this crawler can handle the given URL.
     * @param url The URL to check.
     * @return true if the URL belongs to this source.
     */
    abstract fun canHandle(url: String): Boolean

    /**
     * Scrapes the novel details (metadata and chapter list) from the source.
     * @param novelUrl The URL of the novel landing page.
     * @return A [Novel] object populated with metadata and chapters.
     */
    abstract suspend fun getNovelDetails(novelUrl: String): Novel

    /**
     * Scrapes the content of a specific chapter.
     * @param chapterUrl The URL of the chapter page.
     * @return The HTML content of the chapter body.
     */
    abstract suspend fun getChapterContent(chapterUrl: String): String

    /**
     * Fetches HTML from a URL with a standard User-Agent.
     * @param url The target URL.
     * @param headers Optional headers to add to the request.
     * @param body Optional request body for POST requests.
     * @return The HTML string or null if the request fails.
     */
    protected suspend fun fetchHtml(
        url: String, 
        headers: Map<String, String> = emptyMap(),
        body: RequestBody? = null
    ): String? = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext null
        
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
        
        headers.forEach { (k, v) -> builder.header(k, v) }
        
        if (body != null) {
            builder.post(body)
        }
        
        val request = builder.build()
        
        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    if (responseBody.isNullOrEmpty()) {
                        Log.w("Crawler", "Empty successful response from $url")
                    }
                    responseBody
                } else {
                    Log.e("Crawler", "HTTP Error ${response.code} for $url. Body: $responseBody")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("Crawler", "Error fetching HTML from $url", e)
            null
        }
    }

    /**
     * Fetches and parses HTML into a Jsoup Document.
     * @param url The target URL.
     * @return A [Document] object or null if fetching fails.
     */
    protected suspend fun getDocument(url: String): Document? {
        val html = fetchHtml(url) ?: return null
        return Jsoup.parse(html, url)
    }

    /**
     * Utility to resolve a relative URL to an absolute one.
     */
    protected fun absoluteUrl(relativeUrl: String, base: String = baseUrl): String {
        if (relativeUrl.startsWith("http")) return relativeUrl
        return if (relativeUrl.startsWith("/")) {
            base.trimEnd('/') + relativeUrl
        } else {
            base.trimEnd('/') + "/" + relativeUrl
        }
    }

    /**
     * Clean chapter content by removing scripts, styles, and ads.
     */
    protected fun cleanHtml(doc: Document, selector: String): String {
        val content = doc.select(selector).first() ?: return ""
        
        // Generic cleaning logic
        content.select("script, style, ins, .adsbygoogle, .hidden, [style*='display:none']").remove()
        content.select("div:not(:has(p))").remove()
        
        return content.html().trim()
    }
}
