package com.halovoid.lncrawler.data.repository

import android.content.Context
import com.halovoid.lncrawler.data.crawler.core.CrawlerFactory
import com.halovoid.lncrawler.data.db.AppDatabase
import com.halovoid.lncrawler.data.db.entities.ChapterEntity
import com.halovoid.lncrawler.data.db.entities.NovelEntity
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.Novel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Main repository for managing novel data in the Data layer.
 * Coordinates between the [com.halovoid.lncrawler.data.crawler.core.Crawler]s (Network)
 * and [com.halovoid.lncrawler.data.db.AppDatabase] (Local Storage).
 */
class NovelRepository(context: Context) {
    /** Access to the Room database instance. */
    private val db = AppDatabase.getDatabase(context)
    /** Data Access Object for novel-related database operations. */
    private val novelDao = db.novelDao()

    /**
     * Retrieves all novels saved in the local database.
     * @return A [Flow] emitting the latest list of [Novel]s.
     */
    fun getSavedNovels(): Flow<List<Novel>> {
        return novelDao.getAllNovels().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Retrieves full details for a novel.
     * 
     * @param crawlerName Name of the crawler to use for network fetch.
     * @param novelUrl The URL of the novel.
     * @param refresh If true, always attempts to fetch the latest data from the network.
     *                If false, returns cached data if available.
     * @return The populated [Novel] or null if not found.
     */
    suspend fun getNovelDetails(crawlerName: String, novelUrl: String, refresh: Boolean = true): Novel? {
        if (!refresh) {
            val cached = novelDao.getNovelWithChapters(novelUrl)
            if (cached != null) {
                return cached.novel.toDomain(cached.chapters.map { it.toDomain() })
            }
        }

        // Fetch from network
        val crawler = CrawlerFactory.getCrawler(crawlerName)
        val novel = crawler?.getNovelDetails(novelUrl)
        
        if (novel != null) {
            saveNovel(novel, crawlerName)
            return novel
        }

        // Fallback to DB if network fails or crawler not found
        val cached = novelDao.getNovelWithChapters(novelUrl)
        return cached?.novel?.toDomain(cached.chapters.map { it.toDomain() })
    }

    /**
     * Persists a novel and its chapters to the local Room database.
     * @param novel The novel to save.
     * @param crawlerName The name of the crawler that provided the data.
     */
    private suspend fun saveNovel(novel: Novel, crawlerName: String) {
        novelDao.insertNovel(novel.toEntity(crawlerName))
        novelDao.insertChapters(novel.chapters.map { it.toEntity(novel.url) })
    }

    /**
     * Fetches the content of a specific chapter from the network.
     * @param crawlerName The crawler to use.
     * @param chapterUrl The URL of the chapter.
     * @return The HTML content of the chapter.
     */
    suspend fun getChapterContent(crawlerName: String, chapterUrl: String): String {
        val crawler = CrawlerFactory.getCrawler(crawlerName)
        return crawler?.getChapterContent(chapterUrl) ?: ""
    }

    /**
     * Deletes a novel and its chapters from the local database.
     * @param novel The novel to delete.
     */
    suspend fun deleteNovel(novel: Novel) {
        val entity = novel.toEntity(novel.crawlerName ?: "NovelBin")
        novelDao.deleteNovel(entity)
    }

    /** Mapping extension: Converts [NovelEntity] to [Novel] domain model. */
    private fun NovelEntity.toDomain(chapters: List<Chapter> = emptyList()) = Novel(
        url = url,
        title = title,
        author = author,
        coverUrl = coverUrl,
        description = description,
        chapters = chapters,
        crawlerName = crawlerName,
        alternativeNames = alternativeNames
    )

    /** Mapping extension: Converts [ChapterEntity] to [Chapter] domain model. */
    private fun ChapterEntity.toDomain() = Chapter(
        url = url,
        title = title,
        index = index
    )

    /** Mapping extension: Converts [Novel] domain model to [NovelEntity]. */
    private fun Novel.toEntity(crawlerName: String) = NovelEntity(
        url = url,
        title = title,
        author = author,
        coverUrl = coverUrl,
        description = description,
        crawlerName = crawlerName,
        alternativeNames = alternativeNames
    )

    /** Mapping extension: Converts [Chapter] domain model to [ChapterEntity]. */
    private fun Chapter.toEntity(novelUrl: String) = ChapterEntity(
        novelUrl = novelUrl,
        url = url,
        title = title,
        index = index
    )
}
