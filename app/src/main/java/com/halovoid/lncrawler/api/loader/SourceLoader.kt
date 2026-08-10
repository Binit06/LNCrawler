package com.halovoid.lncrawler.api.loader

import android.content.Context
import android.util.Log
import com.halovoid.lncrawler.api.core.crawler.Crawler
import com.halovoid.lncrawler.api.core.crawler.CrawlerFactory
import com.halovoid.lncrawler.data.repository.PreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Manages loading crawlers from a remote DEX file.
 */
class SourceLoader(private val context: Context) {
    private val dexLoader = DexLoader(context)
    private val preferenceRepository = PreferenceRepository(context)
    private val client = OkHttpClient()

    private val GITHUB_API_URL = "https://api.github.com/repos/Binit06/LNCrawlerSources/releases/latest"
    private val AGGREGATOR_CLASS = "com.halovoid.lncrawlersources.CrawlerSourceAggregator"

    data class ReleaseInfo(val tagName: String, val downloadUrl: String)

    /**
     * Downloads the latest DEX file and loads crawlers into the Factory.
     */
    suspend fun loadSources(
        releaseInfo: ReleaseInfo? = null,
        onProgress: (String) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        try {
            onProgress("Starting source synchronization...")
            Log.i("SourceLoader", "Starting to load sources...")

            val info = releaseInfo ?: fetchLatestReleaseInfo()
            
            onProgress("Downloading crawler DEX bundle (${info.tagName})...")
            val dexFile = downloadDex(info.downloadUrl)
            
            onProgress("Initializing DexClassLoader...")
            val aggregatorClass = dexLoader.load(dexFile, AGGREGATOR_CLASS)
            
            onProgress("Instantiating Crawler Aggregator...")
            val aggregator = aggregatorClass.getDeclaredConstructor().newInstance()
            val getCrawlersMethod = aggregatorClass.getMethod("getCrawlers")
            
            onProgress("Extracting crawler definitions...")
            val rawCrawlers = getCrawlersMethod.invoke(aggregator) as? List<*>
            val validCrawlers = mutableListOf<Crawler>()

            rawCrawlers?.forEach { item ->
                if (item is Crawler) {
                    onProgress("Loaded Crawler: ${item.name}")
                    validCrawlers.add(item)
                } else {
                    val className = item?.javaClass?.name ?: "null"
                    onProgress("Skipping incompatible source: $className")
                    Log.e("SourceLoader", "Incompatible crawler found: $className")
                }
            }
            
            onProgress("Finalizing: ${validCrawlers.size} crawlers registered.")
            CrawlerFactory.registerCrawlers(validCrawlers)
            
            // Save the tag name upon successful sync
            preferenceRepository.setCurrentDexTag(info.tagName)
            
        } catch (e: Exception) {
            onProgress("Error: ${e.message ?: "Sync failed"}")
            Log.e("SourceLoader", "Failed to load sources", e)
        }
    }

    suspend fun fetchLatestReleaseInfo(): ReleaseInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(GITHUB_API_URL).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to fetch release info: $response")
            
            val body = response.body?.string() ?: throw Exception("Empty response body")
            val json = JSONObject(body)
            val tagName = json.getString("tag_name")
            val assets = json.getJSONArray("assets")
            
            var downloadUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name") == "classes.dex") {
                    downloadUrl = asset.getString("browser_download_url")
                    break
                }
            }
            
            ReleaseInfo(
                tagName = tagName,
                downloadUrl = downloadUrl ?: throw Exception("classes.dex not found in latest release")
            )
        }
    }

    private fun downloadDex(url: String): File {
        val dexDir = File(context.filesDir, "sources")
        if (!dexDir.exists()) dexDir.mkdirs()
        
        val targetFile = File(dexDir, "sources.dex")
        
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to download DEX: $response")
            
            response.body?.byteStream()?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        
        return targetFile
    }
}
