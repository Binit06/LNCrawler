package com.halovoid.lncrawler.data.handlers.crawlers

import com.halovoid.lncrawler.data.crawler.core.crawler.CrawlerFactory
import com.halovoid.lncrawler.data.db.dao.RequestDao
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.data.repository.ChapterRepository
import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.data.repository.VolumeRepository
import com.halovoid.lncrawler.data.scheduler.JobHandler
import com.halovoid.lncrawler.data.scheduler.JobResult
import java.util.UUID

class CrawlNovelHandler(
    private val crawlerFactory: CrawlerFactory,
    private val novelRepository: NovelRepository,
    private val chapterRepository: ChapterRepository,
    private val volumeRepository: VolumeRepository,
    private val requestDao: RequestDao
) : JobHandler {
    override suspend fun handle(request: RequestEntity): JobResult {
        if (request.crawlerName == null) {
            return JobResult.Failure(Exception("No Crawler Provided"))
        }
        val crawler = crawlerFactory.getCrawler(request.crawlerName)
            ?: return JobResult.Failure(Exception("No Crawler Found"))

        val novel = crawler.getNovelDetails(request.novelUrl)

        val updatedNovel = crawler.prepareNovel(novel)

        novelRepository.saveNovelMetadata(updatedNovel)
        chapterRepository.insertChapters(updatedNovel.chapters)
        volumeRepository.insertVolumes(updatedNovel.volumes)

        // Create Follow Up Requests
        val chapterRequests = updatedNovel.chapters.map { chapter ->
            RequestEntity(
                id = UUID.randomUUID().toString(),
                type = RequestType.CRAWL_CHAPTERS,
                parentNovel = novel.url,
                novelUrl = chapter.novelUrl,
                url = request.url,
                priority = request.priority,
                name = "Chapter ${chapter.index} Download",
                dependsOn = request.id,
                completedAt = null,
                crawlerName = request.crawlerName
            )
        }
        requestDao.insertRequests(chapterRequests)

        return JobResult.Success
    }
}