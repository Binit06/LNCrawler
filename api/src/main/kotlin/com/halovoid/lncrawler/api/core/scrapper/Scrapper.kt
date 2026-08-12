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
import java.util.concurrent.TimeUnit

/**
 * Interface for resolving Cloudflare challenges.
 * Implemented in the :app module to show a WebView.
 */
interface CloudflareResolver {
    /**
     * Attempts to solve Cloudflare challenge for the given URL.
     * @return true if challenge was likely solved, false otherwise.
     */
    suspend fun resolve(url: String): Boolean

    /**
     * Returns the current User-Agent used by the resolver (e.g. from WebView).
     */
    fun getUserAgent(): String
}

/**
 * Handles the generic mechanics of communicating with websites.
 * Responsible for HTTP requests, session management (cookies), and HTML parsing.
 */
class Scrapper {
    companion object {
        var globalResolver: CloudflareResolver? = null
    }

    private var userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val manager = android.webkit.CookieManager.getInstance()
            cookies.forEach { cookie ->
                manager.setCookie(url.toString(), cookie.toString())
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val cookieString = android.webkit.CookieManager.getInstance().getCookie(url.toString())
            if (cookieString.isNullOrEmpty()) return emptyList()

            return cookieString.split(";").mapNotNull {
                Cookie.parse(url, it.trim())
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Fetches the content of a URL as a String.
     * @param url The target URL.
     * @param headers Optional headers to add to the request.
     * @param body Optional request body for POST requests.
     * @param attempt Current attempt number for retries.
     * @return The response body as a String, or null if the request fails.
     */
    suspend fun fetch(
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: RequestBody? = null,
        attempt: Int = 1,
        webviewNeeded: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext null

        // Sync with global resolver's User-Agent if available
        globalResolver?.getUserAgent()?.let {
            if (it.isNotEmpty()) userAgent = it
        }

        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)

        headers.forEach { (k, v) -> builder.header(k, v) }

        if (body != null) {
            builder.post(body)
        }

        val request = builder.build()

        if (webviewNeeded && attempt == 1) {
            val resolved = globalResolver?.resolve(url) ?: false
            if (resolved) {
                return@withContext fetch(url, headers, body, attempt + 1, true)
            } else {
                Log.e("Scrapper", "Failed to bypass website protection")
                return@withContext null
            }
        }

        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                
                if (response.isSuccessful) {
                    if (responseBody.isNullOrEmpty()) {
                        Log.w("Scrapper", "Empty successful response from $url")
                    }
                    responseBody
                } else {
                    val isCloudflare = response.code == 403 || response.code == 503 || response.code == 429
                    val serverHeader = response.header("Server") ?: ""
                    val isCfHeader = serverHeader.contains("cloudflare", ignoreCase = true)
                    val bodyHasCf = responseBody?.contains("cf-ray", ignoreCase = true) == true || 
                                    responseBody?.contains("__cf_chl_opt", ignoreCase = true) == true

                    if (isCloudflare && (isCfHeader || bodyHasCf)) {
                        Log.w("Scrapper", "Cloudflare detected for $url (Attempt $attempt/3)")
                        
                        if (attempt < 3) {
                            val resolved = globalResolver?.resolve(url) ?: false
                            if (resolved) {
                                // Recursive retry with incremented attempt
                                return@withContext fetch(url, headers, body, attempt + 1, webviewNeeded)
                            }
                        }
                        Log.e("Scrapper", "Cloudflare resolution failed after $attempt attempts for $url")
                    }
                    
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
        headers: Map<String, String> = emptyMap(),
        webviewNeeded: Boolean = false
    ): Document? {
        val html = fetch(url, headers, webviewNeeded = webviewNeeded) ?: return null
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