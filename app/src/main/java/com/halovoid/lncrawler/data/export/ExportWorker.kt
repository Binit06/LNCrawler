package com.halovoid.lncrawler.data.export

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.halovoid.lncrawler.data.crawler.core.CrawlerFactory
import com.halovoid.lncrawler.data.repository.NovelRepository
import androidx.core.net.toUri
import com.halovoid.lncrawler.data.db.AppDatabase
import com.halovoid.lncrawler.data.db.entities.ExportRecordEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Background worker for fetching novel chapters and generating an EPUB file.
 * Ensures the process continues even if the app is minimized.
 * Updates history records upon success, failure, or cancellation.
 */
class ExportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val novelUrl = inputData.getString("novelUrl") ?: return Result.failure()
        val crawlerName = inputData.getString("crawlerName") ?: return Result.failure()
        val destinationUriString = inputData.getString("destinationUri") ?: return Result.failure()
        val destinationUri = destinationUriString.toUri()
        
        val database = AppDatabase.getDatabase(applicationContext)
        val exportRecordDao = database.exportRecordDao()
        val repository = NovelRepository(applicationContext)
        val crawler = CrawlerFactory.getCrawler(crawlerName) ?: return Result.failure()
        
        val novel = repository.getNovelDetails(crawlerName, novelUrl) ?: return Result.failure()
        
        val initialRecord = ExportRecordEntity(
            novelUrl = novelUrl,
            novelTitle = novel.title,
            status = "PENDING",
            destinationUri = destinationUriString,
            errorLog = null,
            crawlerName = crawlerName
        )
        val recordId = exportRecordDao.insert(initialRecord)
        val epubExporter = EpubExporter()

        return try {
            applicationContext.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                epubExporter.export(
                    novel = novel,
                    crawler = crawler,
                    outputStream = outputStream,
                    onProgress = { current, total, status ->
                        ExportProgressManager.updateProgress(
                            recordId, 
                            novelUrl,
                            ExportProgress(current, total, status)
                        )
                    }
                )
            }

            withContext(NonCancellable) {
                exportRecordDao.getRecordByIdOnce(recordId)?.let { record ->
                    exportRecordDao.update(record.copy(status = "SUCCESS"))
                }
            }
            Result.success()
        } catch (e: CancellationException) {
            Log.i("ExportWorker", "Export cancelled for $novelUrl : $e")
            withContext(NonCancellable) {
                exportRecordDao.getRecordByIdOnce(recordId)?.let { record ->
                    exportRecordDao.update(record.copy(status = "CANCELLED"))
                }
            }
            applicationContext.contentResolver.delete(destinationUri, null, null) // fix: remove the file that has been created
            Result.failure()
        } catch (e: Exception) {
            Log.e("ExportWorker", "Export failed for $novelUrl", e)
            withContext(NonCancellable) {
                exportRecordDao.getRecordByIdOnce(recordId)?.let { record ->
                    exportRecordDao.update(
                        record.copy(
                            status = "FAILED",
                            errorLog = e.message ?: "Unknown error"
                        )
                    )
                }
            }
            applicationContext.contentResolver.delete(destinationUri, null, null) // fix: remove the file that has been created
            Result.failure()
        } finally {
            // Small delay to ensure DB status updates propagate to UI before clearing progress
            kotlinx.coroutines.delay(500L.milliseconds)
            ExportProgressManager.updateProgress(recordId, novelUrl, null)
        }
    }
}
