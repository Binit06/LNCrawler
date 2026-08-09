package com.halovoid.lncrawler.data.handlers

import com.halovoid.lncrawler.data.artifact.ArtifactGeneratorFactory
import com.halovoid.lncrawler.data.crawler.core.crawler.CrawlerFactory
import com.halovoid.lncrawler.data.db.dao.RequestDao
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.handlers.utility.parsedMetadata
import com.halovoid.lncrawler.data.repository.ArtifactRepository
import com.halovoid.lncrawler.data.repository.ChapterRepository
import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.data.repository.StorageRepository
import com.halovoid.lncrawler.data.repository.VolumeRepository
import com.halovoid.lncrawler.data.scheduler.jobs.JobHandler
import com.halovoid.lncrawler.data.scheduler.jobs.JobResult
import com.halovoid.lncrawler.domain.models.Artifact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ArtifactHandler(
    private val novelRepository: NovelRepository,
    private val chapterRepository: ChapterRepository,
    private val volumeRepository: VolumeRepository,
    private val crawlerFactory: CrawlerFactory,
    private val storageRepository: StorageRepository,
    private val generatorFactory: ArtifactGeneratorFactory,
    private val artifactRepository: ArtifactRepository,
    private val requestDao: RequestDao
) : JobHandler {
    override suspend fun handle(request: RequestEntity): JobResult = withContext(Dispatchers.IO) {
        val metadata = request.parsedMetadata
        val format = metadata.format ?: return@withContext JobResult.Failure(Exception("No Format Provided"))
        val crawlerName = metadata.crawlerName ?: return@withContext JobResult.Failure(Exception("No Crawler Name provided"))

        try {
            // 1. Fetch All Necessary data
            val novel = novelRepository.getNovelDetails(request.novelUrl)
                ?: return@withContext JobResult.Failure(Exception("Novel not found in database"))
            val chapters = chapterRepository.getChaptersByNovelUrl(request.novelUrl)
            val volumes = volumeRepository.getVolumeByNovelUrl(request.novelUrl)

            // 2. Select generator and create temp file
            val generator = generatorFactory.getGenerator(format)
            val tempFile = generator.generate(novel, volumes, chapters, metadata)
            val crawler = crawlerFactory.getCrawler(crawlerName)
                ?: return@withContext JobResult.Failure(Exception("Crawler '$crawlerName' not found"))


            // 3. Save Permanenetly to the user's selected storage
            val novelKey = crawler.getNovelKey(novel.title)
            val fileName = "${novelKey}_${System.currentTimeMillis()}.$format"
            val finalUri = storageRepository.saveFile(
                relativePath = "artifacts/$novelKey",
                fileName = fileName,
                mimeType = "application/epub+zip",
                data = tempFile.readBytes()
            )

            // 4. Insert New Artifact to Database
            val artifact = Artifact(
                id = 0,
                novelUrl = novel.url,
                requestId = request.id,
                artifactDestination = finalUri.toString(),
                artifactName = fileName
            )
            artifactRepository.insertArtifacts(artifact)

            // 5. Cleanup Temp File
            tempFile.delete()

            // 6. Notify
            requestDao.propagateProgress(request.id)
            JobResult.Success
        } catch (e: Exception) {
            JobResult.Failure(e)
        }

    }
}