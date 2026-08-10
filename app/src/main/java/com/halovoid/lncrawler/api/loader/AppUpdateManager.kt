package com.halovoid.lncrawler.api.loader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class AppUpdateManager {
    private val client = OkHttpClient()
    private val GITHUB_API_URL = "https://api.github.com/repos/Binit06/LNCrawler/releases/latest"

    data class AppReleaseInfo(val tagName: String, val releaseUrl: String)

    suspend fun fetchLatestAppRelease(): AppReleaseInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(GITHUB_API_URL).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to fetch app release info: $response")
            
            val body = response.body?.string() ?: throw Exception("Empty response body")
            val json = JSONObject(body)
            
            AppReleaseInfo(
                tagName = json.getString("tag_name"),
                releaseUrl = json.getString("html_url")
            )
        }
    }

    companion object {
        fun isUpdateAvailable(current: String, latest: String): Boolean {
            val currentSegments = normalize(current)
            val latestSegments = normalize(latest)

            val maxSize = maxOf(currentSegments.size, latestSegments.size)
            
            for (i in 0 until maxSize) {
                val curr = currentSegments.getOrElse(i) { 0 }
                val late = latestSegments.getOrElse(i) { 0 }
                
                if (late > curr) return true
                if (curr > late) return false
            }
            
            return false
        }

        private fun normalize(version: String): List<Int> {
            return version.lowercase()
                .replace("v", "")
                .split("-")[0] // Ignore suffixes like -beta
                .split(".")
                .mapNotNull { it.toIntOrNull() }
        }
    }
}
