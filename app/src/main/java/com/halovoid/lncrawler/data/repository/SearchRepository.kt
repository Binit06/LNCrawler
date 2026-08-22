package com.halovoid.lncrawler.data.repository

import com.halovoid.lncrawler.domain.models.SearchResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

class SearchRepository(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun search(query: String): SearchResponse = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://lncs.up.railway.app/api/search?q=$encodedQuery"
        
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Search failed: ${response.code}")
            val body = response.body?.string() ?: throw Exception("Empty response body")
            json.decodeFromString<SearchResponse>(body)
        }
    }
}
