package com.halovoid.lncrawler.data.db.dao

import androidx.room.*
import com.halovoid.lncrawler.data.db.entities.ChapterEntity
import com.halovoid.lncrawler.data.db.entities.NovelEntity
import com.halovoid.lncrawler.domain.models.Novel
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for [NovelEntity] and [ChapterEntity] in the Data layer.
 * Defines the database operations for saving and retrieving novel data.
 */
@Dao
interface NovelDao {
    /** Returns a [Flow] of all saved novels. */
    @Query("SELECT * FROM novels")
    fun getAllNovels(): Flow<List<NovelEntity>>

    @Query("SELECT * FROM novels WHERE url = :url")
    fun getNovelByUrl(url: String): NovelEntity?

    /** Inserts or replaces a novel in the database. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNovel(novel: NovelEntity)

    /** Deletes a novel from the database. */
    @Delete
    fun deleteNovel(novel: NovelEntity)
}