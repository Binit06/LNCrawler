package com.halovoid.lncrawler.api.loader

import android.content.Context
import android.util.Log
import com.halovoid.lncrawler.api.core.crawler.Crawler
import com.halovoid.lncrawler.api.core.crawler.CrawlerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * Manages loading crawlers from a remote DEX file.
 */
class SourceLoader(private val context: Context) {
    private val dexLoader = DexLoader(context)
    private val client = OkHttpClient()

    private val DEX_URL = "https://github.com/Binit06/LNCrawlerSources/releases/download/latest/classes.dex"
    private val AGGREGATOR_CLASS = "com.halovoid.lncrawlersources.CrawlerSourceAggregator"

    /**
     * Downloads the latest DEX file and loads crawlers into the Factory.
     */
    suspend fun loadSources(onProgress: (String) -> Unit = {}) = withContext(Dispatchers.IO) {
        try {
            onProgress("Starting source synchronization...")
            Log.i("SourceLoader", "Starting to load sources...")
            
            onProgress("Downloading latest crawler DEX bundle...")
            val dexFile = downloadDex()
            
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
            
        } catch (e: Exception) {
            onProgress("Error: ${e.message ?: "Sync failed"}")
            Log.e("SourceLoader", "Failed to load sources", e)
        }
    }

    private fun downloadDex(): File {
        val dexDir = File(context.filesDir, "sources")
        if (!dexDir.exists()) dexDir.mkdirs()
        
        val targetFile = File(dexDir, "sources.dex")
        
        val request = Request.Builder().url(DEX_URL).build()
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
