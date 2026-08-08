package com.halovoid.lncrawler.data.handlers

import android.net.Uri
import com.halovoid.lncrawler.data.crawler.core.crawler.Crawler
import com.halovoid.lncrawler.data.crawler.core.crawler.CrawlerFactory
import com.halovoid.lncrawler.data.db.dao.RequestDao
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.data.handlers.utility.parsedMetadata
import com.halovoid.lncrawler.data.repository.ChapterRepository
import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.data.repository.StorageRepository
import com.halovoid.lncrawler.data.repository.VolumeRepository
import com.halovoid.lncrawler.data.scheduler.jobs.JobHandler
import com.halovoid.lncrawler.data.scheduler.jobs.JobResult
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONObject
import java.util.UUID

class NovelHandler(
    private val crawlerFactory: CrawlerFactory,
    private val novelRepository: NovelRepository,
    private val chapterRepository: ChapterRepository,
    private val volumeRepository: VolumeRepository,
    private val storageRepository: StorageRepository,
    private val requestDao: RequestDao
) : JobHandler {
    override suspend fun handle(request: RequestEntity): JobResult {
        val metadata = request.parsedMetadata
        if (metadata.crawlerName == null) {
            return JobResult.Failure(Exception("No Crawler Provided"))
        }
        val crawler = crawlerFactory.getCrawler(metadata.crawlerName)
            ?: return JobResult.Failure(Exception("No Crawler Found"))

        val novel = crawler.getNovelDetails(request.novelUrl)

        requestDao.updateProgressTotal(request.id, novel.chapters.size) // total progress is just the count of the leaf requests

        // 1. Download and Save Cover Image
        val coverFilePath = downloadAndSaveCover(novel.coverUrl, crawler)

        // 2. Create the updateNovel with all the data
        val updatedNovel = crawler.prepareNovel(novel).copy(
            coverUrl = coverFilePath.toString()
        )

        // 2. Persist Novel Metadata
        novelRepository.saveNovelMetadata(updatedNovel)
        chapterRepository.insertChapters(updatedNovel.chapters)
        volumeRepository.insertVolumes(updatedNovel.volumes)

        // 3. Create Follow Up Requests
        val volumeRequests = updatedNovel.volumes.map { volume ->
            currentCoroutineContext().ensureActive()
            val volumeMetadata = JSONObject(request.metadata ?: "{}").apply {
                put("volumeId", volume.id)
            }.toString()

            RequestEntity(
                id = "${novel.url}_vol_${volume.volumeIndex}",
                type = RequestType.VOLUME,
                parentNovel = novel.url,
                novelUrl = volume.novelUrl,
                priority = request.priority,
                name = "Volume ${volume.volumeIndex} Request",
                dependsOn = request.id,
                completedAt = null,
                metadata = volumeMetadata,
                url = null,
                progressTotal = novel.chapters.filter { it.volumeId == volume.id }.size, // total count of leaf requests from this point in the tree
                progressSuccess = 0
            )
        }
        requestDao.insertRequests(volumeRequests)

        return JobResult.Success
    }

    suspend fun downloadAndSaveCover(url: String?, crawler: Crawler) : Uri? {
        if (url != null && !url.startsWith("content://")) {
            try {
                val bytes = crawler.downloadCover(url)
                if (bytes != null) {
                    val extension = if (url.contains(".png", ignoreCase = true)) "png" else "jpg"
                    val novelKey = crawler.getNovelKey(url)
                    val fileName = "cover.$extension"
                    val localUri = storageRepository.saveFile("novels/$novelKey/covers", fileName, "image/$extension", bytes)
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