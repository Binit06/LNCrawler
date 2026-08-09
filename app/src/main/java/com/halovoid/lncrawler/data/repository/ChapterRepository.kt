package com.halovoid.lncrawler.data.repository

import android.content.Context
import com.halovoid.lncrawler.data.db.AppDatabase
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.toDomain
import com.halovoid.lncrawler.domain.models.toEntity
import kotlinx.coroutines.flow.Flow

class ChapterRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val chapterDao = db.chapterDao()

    fun getChaptersByNovelUrl(url: String): List<Chapter> {
        return chapterDao.getChapterFromNovel(url).map { it -> it.toDomain() }
    }

    suspend fun insertChapters(chapters : List<Chapter>) {
        chapterDao.insertChapters(chapters.map { it -> it.toEntity() })
    }

    fun getChapterById(id: Int) : Chapter {
        return chapterDao.getChapterById(id).toDomain()
    }

    suspend fun updateChapter(chapter: Chapter) {
        chapterDao.updateChapter(chapter = chapter.toEntity())
    }

    fun getChapterCount(novelUrl: String): Flow<Int> =
        chapterDao.getChapterCountFlow(novelUrl)
}