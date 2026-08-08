package com.halovoid.lncrawler.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.halovoid.lncrawler.data.db.entities.ChapterEntity

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE novelUrl = :url")
    fun getChapterFromNovel(url: String): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE novelUrl = :url AND volumeId = :id")
    fun getChapterFromNovelAndVolume(url: String, id: Int): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE id = :id")
    fun getChapterById(id: Int): ChapterEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChapters(chapters: List<ChapterEntity>)

    @Update
    fun updateChapter(chapter: ChapterEntity)
}