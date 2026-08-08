package com.halovoid.lncrawler.data.repository

import android.content.Context
import com.halovoid.lncrawler.data.crawler.core.crawler.CrawlerFactory
import com.halovoid.lncrawler.data.db.AppDatabase
import com.halovoid.lncrawler.data.db.entities.ChapterEntity
import com.halovoid.lncrawler.data.db.entities.NovelEntity
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.domain.models.toDomain
import com.halovoid.lncrawler.domain.models.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Main repository for managing novel data in the Data layer.
 * Coordinates between the [com.halovoid.lncrawler.data.crawler.core.crawler.Crawler]s (Network)
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
    fun getAllNovels(): Flow<List<Novel>> {
        return novelDao.getAllNovels().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Retrieves full details for a novel.
     *
     * @param novelUrl The URL of the novel.
     * @return The populated [Novel] or null if not found.
     */
    suspend fun getNovelDetails(novelUrl: String): Novel? {
        return novelDao.getNovelByUrl(novelUrl)?.toDomain()
    }

    /**
     * Persists a novel and its chapters to the local Room database.
     * @param novel The novel to save.
     */
    fun saveNovelMetadata(novel: Novel) {
        novelDao.insertNovel(novel.toEntity())
    }

    /**
     * Deletes a novel and its chapters from the local database.
     * @param novel The novel to delete.
     */
    suspend fun deleteNovel(novel: Novel) {
        val entity = novel.toEntity()
        novelDao.deleteNovel(entity)
    }
}
