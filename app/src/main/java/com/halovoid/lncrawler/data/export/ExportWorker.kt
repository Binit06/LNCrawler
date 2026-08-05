package com.halovoid.lncrawler.data.export

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.halovoid.lncrawler.data.crawler.core.CrawlerFactory
import com.halovoid.lncrawler.data.repository.NovelRepository
import java.io.OutputStream

/**
 * Background worker for fetching novel chapters and generating an EPUB file.
 * Ensures the process continues even if the app is minimized.
 */
class ExportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val novelUrl = inputData.getString("novelUrl") ?: return Result.failure()
        val crawlerName = inputData.getString("crawlerName") ?: return Result.failure()
        val destinationUriString = inputData.getString("destinationUri") ?: return Result.failure()
        val destinationUri = Uri.parse(destinationUriString)
        
        val repository = NovelRepository(applicationContext)
        val crawler = CrawlerFactory.getCrawler(crawlerName) ?: return Result.failure()
        
        // Fetch full novel details including chapters from DB/Network
        val novel = repository.getNovelDetails(crawlerName, novelUrl) ?: return Result.failure()
        val epubExporter = EpubExporter()

        return try {
            applicationContext.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                epubExporter.export(
                    novel = novel,
                    crawler = crawler,
                    outputStream = outputStream,
                    onProgress = { current, total, status ->
                        ExportProgressManager.updateProgress(
                            novelUrl, 
                            ExportProgress(current, total, status)
                        )
                    }
                )
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("ExportWorker", "Export failed for $novelUrl", e)
            Result.failure()
        } finally {
            ExportProgressManager.updateProgress(novelUrl, null)
        }
    }
}
