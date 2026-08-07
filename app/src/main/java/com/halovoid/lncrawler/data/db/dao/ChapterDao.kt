package com.halovoid.lncrawler.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.halovoid.lncrawler.data.db.entities.ChapterEntity

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE novelUrl = :url")
    fun getChapterFromNovel(url: String): List<ChapterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChapters(chapters: List<ChapterEntity>)
}