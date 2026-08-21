package com.halovoid.lncrawler.data.handlers

import com.halovoid.lncrawler.data.db.dao.RequestDao
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.data.handlers.utility.parsedMetadata
import com.halovoid.lncrawler.data.repository.ChapterRepository
import com.halovoid.lncrawler.data.scheduler.jobs.JobHandler
import com.halovoid.lncrawler.data.scheduler.jobs.JobResult
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONObject

/**
 * Handler for [RequestType.RANGE_DOWNLOAD] requests.
 * Spawns CHAPTER requests directly for a specific range without using VOLUME requests.
 * This simplifies the UI and ensures chapter lists are visible in the request details.
 */
class RangeDownloadHandler(
    private val chapterRepository: ChapterRepository,
    private val requestDao: RequestDao
) : JobHandler {
    override suspend fun handle(request: RequestEntity): JobResult {
        val metadata = request.parsedMetadata
        val startIndex = metadata.startIndex ?: 1
        val endIndex = metadata.endIndex ?: Int.MAX_VALUE

        // 1. Fetch all chapters for this novel from the local database
        val chapters = chapterRepository.getChaptersByNovelUrl(request.novelUrl)
            .filter { it.index in startIndex..endIndex }
        
        if (chapters.isEmpty()) {
            return JobResult.Failure(Exception("No chapters found in the specified range ($startIndex-$endIndex)"))
        }

        // 2. Set the total progress to the number of chapters we're about to download
        requestDao.updateProgressTotal(request.id, chapters.size)

        // 3. Create CHAPTER requests directly under this range request
        val chapterRequests = chapters.map { chapter ->
            currentCoroutineContext().ensureActive()
            
            val chapterMetadata = JSONObject().apply {
                put("chapterId", chapter.id)
                put("crawlerName", metadata.crawlerName)
            }.toString()

            RequestEntity(
                id = "${request.id}_ch_${chapter.index}", // Use range request ID as prefix for uniqueness
                type = RequestType.CHAPTER,
                parentNovel = request.novelUrl,
                dependsOn = request.id,
                priority = request.priority,
                name = "Chapter ${chapter.index}: ${chapter.title}",
                completedAt = null,
                metadata = chapterMetadata,
                url = chapter.url,
                novelUrl = chapter.novelUrl,
                progressTotal = 1,
                progressSuccess = 0,
            )
        }

        // 4. Batch insert and start tracking progress
        requestDao.insertRequests(chapterRequests)
        requestDao.propagateProgress(chapterRequests.first().id)

        return JobResult.Success
    }
}
