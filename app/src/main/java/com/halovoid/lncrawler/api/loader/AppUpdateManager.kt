package com.halovoid.lncrawler.api.loader

import com.halovoid.lncrawler.api.core.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class AppUpdateManager {
    private val client = NetworkClient.okHttpClient
    private val GITHUB_API_URL = "https://api.github.com/repos/Binit06/LNCrawler/releases/latest"

    data class AppReleaseInfo(val tagName: String, val releaseUrl: String, val apkDownloadUrl: String?)

    suspend fun fetchLatestAppRelease(): AppReleaseInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(GITHUB_API_URL).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to fetch app release info: $response")
            
            val body = response.body?.string() ?: throw Exception("Empty response body")
            val json = JSONObject(body)
            
            val assets = json.optJSONArray("assets")
            var apkDownloadUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.getString("name").endsWith(".apk")) {
                        apkDownloadUrl = asset.getString("browser_download_url")
                        break
                    }
                }
            }

            AppReleaseInfo(
                tagName = json.getString("tag_name"),
                releaseUrl = json.getString("html_url"),
                apkDownloadUrl = apkDownloadUrl
            )
        }
    }

    companion object {
        fun isUpdateAvailable(current: String, latest: String): Boolean {
            return VersionUtils.isUpdateAvailable(current, latest)
        }
    }
}
