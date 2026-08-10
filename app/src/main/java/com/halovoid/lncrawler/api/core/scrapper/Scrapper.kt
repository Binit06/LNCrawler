package com.halovoid.lncrawler.api.core.scrapper

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Handles the generic mechanics of communicating with websites.
 * Responsible for HTTP requests, session management (cookies), and HTML parsing.
 */
class Scrapper {
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

    private val cookieJar = object : CookieJar {
        private val cookieStore = mutableMapOf<String, List<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .build()

    /**
     * Fetches the content of a URL as a String.
     * @param url The target URL.
     * @param headers Optional headers to add to the request.
     * @param body Optional request body for POST requests.
     * @return The response body as a String, or null if the request fails.
     */
    suspend fun fetch(
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: RequestBody? = null
    ): String? = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext null

        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)

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
                        Log.w("Scrapper", "Empty successful response from $url")
                    }
                    responseBody
                } else {
                    Log.e("Scrapper", "HTTP Error ${response.code} for $url. Body: $responseBody")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("Scrapper", "Error fetching from $url", e)
            null
        }
    }

    /**
     * Fetches and parses a URL into a Jsoup Document.
     * @param url The target URL.
     * @param headers Optional headers to add to the request.
     * @return A Jsoup Document or null if the request fails.
     */
    suspend fun document(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): Document? {
        val html = fetch(url, headers) ?: return null
        return Jsoup.parse(html, url)
    }

    /**
     * Downloads a resource from a URL as a ByteArray.
     * @param url The target URL.
     * @return The resource bytes, or null if the download fails.
     */
    suspend fun download(url: String): ByteArray? = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext null

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else {
                    Log.e("Scrapper", "HTTP Error ${response.code} downloading $url")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("Scrapper", "Error downloading from $url", e)
            null
        }
    }
}