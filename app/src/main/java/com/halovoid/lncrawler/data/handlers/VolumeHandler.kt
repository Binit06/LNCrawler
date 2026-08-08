package com.halovoid.lncrawler.data.handlers

import com.halovoid.lncrawler.data.db.dao.ChapterDao
import com.halovoid.lncrawler.data.db.dao.RequestDao
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.data.handlers.utility.parsedMetadata
import com.halovoid.lncrawler.data.scheduler.jobs.JobHandler
import com.halovoid.lncrawler.data.scheduler.jobs.JobResult
import org.json.JSONObject
import java.util.UUID

class VolumeHandler(
    private val chapterDao: ChapterDao,
    private val requestDao: RequestDao
) : JobHandler {
    override suspend fun handle(request: RequestEntity): JobResult {
        val metadata = request.parsedMetadata
        if (metadata.volumeId == null) {
            return JobResult.Failure(Exception("No Volume ID Provided in Volume Request"))
        }
        val chapters = chapterDao.getChapterFromNovelAndVolume(request.novelUrl, metadata.volumeId)

        val chapterRequests = chapters.map { chapter ->
            val chapterMetadata = JSONObject(request.metadata ?: "{}").apply {
                put("chapterId", chapter.id)
            }.toString()
            RequestEntity(
                id = UUID.randomUUID().toString(),
                type = RequestType.CHAPTER,
                parentNovel = request.parentNovel,
                dependsOn = request.id,
                priority = request.priority,
                name = "Chapter ${chapter.title} Download",
                completedAt = null,
                metadata = chapterMetadata,
                url = chapter.url,
                novelUrl = chapter.novelUrl,
                progressTotal = 1,
                progressCurrent = 0,
            )
        }

        requestDao.insertRequests(chapterRequests)

        return JobResult.Success
    }
}