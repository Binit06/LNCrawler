package com.halovoid.lncrawler.data.repository

import android.content.Context
import com.halovoid.lncrawler.data.db.AppDatabase
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.toDomain
import com.halovoid.lncrawler.domain.models.toEntity

class ChapterRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val chapterDao = db.chapterDao()

    fun getChaptersByNovelUrl(url: String): List<Chapter> {
        return chapterDao.getChapterFromNovel(url).map { it -> it.toDomain() }
    }

    fun insertChapters(chapters : List<Chapter>) {
        chapterDao.insertChapters(chapters.map { it -> it.toEntity() })
    }
}