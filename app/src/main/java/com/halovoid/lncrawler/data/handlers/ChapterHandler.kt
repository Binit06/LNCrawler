package com.halovoid.lncrawler.data.handlers

import android.net.Uri
import com.halovoid.lncrawler.api.core.crawler.Crawler
import com.halovoid.lncrawler.api.core.crawler.CrawlerFactory
import com.halovoid.lncrawler.api.core.scrapper.Scrapper
import com.halovoid.lncrawler.data.db.dao.RequestDao
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.handlers.utility.parsedMetadata
import com.halovoid.lncrawler.data.repository.ChapterRepository
import com.halovoid.lncrawler.data.repository.StorageRepository
import com.halovoid.lncrawler.data.scheduler.jobs.JobHandler
import com.halovoid.lncrawler.data.scheduler.jobs.JobResult
import com.halovoid.lncrawler.domain.models.Chapter

class ChapterHandler(
    private val requestDao: RequestDao,
    private val scrapper: Scrapper,
    private val chapterRepository: ChapterRepository,
    private val storageRepository: StorageRepository,
    private val crawlerFactory: CrawlerFactory
) : JobHandler {
    override suspend fun handle(request: RequestEntity): JobResult {
        val metadata = request.parsedMetadata
        if (metadata.chapterId == null || request.url == null) {
            return JobResult.Failure(Exception("Failure to complete request"))
        }
        if (metadata.crawlerName == null) {
            return JobResult.Failure(Exception("No Crawler Found"))
        }
        val crawler = crawlerFactory.getCrawler(metadata.crawlerName)
            ?: return JobResult.Failure(Exception("No Crawler Found"))

        val chapter = chapterRepository.getChapterById(metadata.chapterId)

        // 1. Load the Chapter and Save it
        val fileLocation = loadAndSaveFile(request.url, crawler, chapter)
            ?: return JobResult.Failure(Exception("Failed to Load Content"))

        // 2. Update the file Location in the Chapter Database
        chapterRepository.updateChapter(chapter = chapter.copy(
            fileLocation = fileLocation.toString()
        ))

        return JobResult.Success
    }

    suspend fun loadAndSaveFile(url: String, crawler: Crawler, chapter: Chapter): Uri? {
        if (!url.startsWith("content://")) {
            try {
                val chapterContent = crawler.getChapterContent(url)
                if (chapterContent != null) {
                    val novelKey = crawler.getNovelKey(chapter.novelUrl)
                    val fileName = "${chapter.index.toString().padStart(4, '0')}.html"
                    val relativePath = "novels/$novelKey/chapters"

                    val localUri = storageRepository.saveText(
                        relativePath = relativePath,
                        fileName = fileName,
                        mimeType = "text/html",
                        content = chapterContent
                    )

                    return localUri
                } else {
                    return null
                }
            } catch (e: Exception) {
                return null
            }
        }
        return null
    }
}