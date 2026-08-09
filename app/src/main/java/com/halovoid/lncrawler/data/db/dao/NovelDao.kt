package com.halovoid.lncrawler.data.db.dao

import androidx.room.*
import com.halovoid.lncrawler.data.db.entities.ChapterEntity
import com.halovoid.lncrawler.data.db.entities.NovelEntity
import com.halovoid.lncrawler.domain.models.Novel
import kotlinx.coroutines.flow.Flow

@Dao
interface NovelDao {
    @Query("SELECT * FROM novels")
    fun getAllNovels(): Flow<List<NovelEntity>>

    @Query("SELECT * FROM novels WHERE url = :url")
    fun getNovelByUrl(url: String): NovelEntity?

    @Query("SELECT * FROM novels WHERE url = :url")
    fun getNovelByUrlFlow(url: String): Flow<NovelEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNovel(novel: NovelEntity)

    @Upsert
    fun upsertNovel(novel: NovelEntity)

    @Delete
    fun deleteNovel(novel: NovelEntity)
}