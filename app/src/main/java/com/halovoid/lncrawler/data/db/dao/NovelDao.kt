package com.halovoid.lncrawler.data.db.dao

import androidx.room.*
import com.halovoid.lncrawler.data.db.entities.ChapterEntity
import com.halovoid.lncrawler.data.db.entities.NovelEntity
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

    /** Inserts or replaces a novel in the database. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNovel(novel: NovelEntity)

    /** Inserts or replaces a list of chapters. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    /** Retrieves a novel and all its associated chapters using a transaction. */
    @Transaction
    @Query("SELECT * FROM novels WHERE url = :url")
    suspend fun getNovelWithChapters(url: String): NovelWithChapters?

    /** Deletes a novel from the database. */
    @Delete
    suspend fun deleteNovel(novel: NovelEntity)
}

/**
 * Data class representing a [NovelEntity] along with its related [ChapterEntity]s.
 * Used for Room [Relation] queries.
 */
data class NovelWithChapters(
    /** The parent novel entity. */
    @Embedded val novel: NovelEntity,
    /** The list of related chapter entities. */
    @Relation(
        parentColumn = "url",
        entityColumn = "novelUrl"
    )
    val chapters: List<ChapterEntity>
)
