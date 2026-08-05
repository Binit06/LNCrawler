package com.halovoid.lncrawler.domain.usecases

import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.domain.models.Novel
import kotlinx.coroutines.flow.Flow

/**
 * Use case to retrieve the list of all novels saved in the local database.
 * This is typically used to populate the "Recently Requested" or "Library" sections.
 */
class GetSavedNovelsUseCase(private val repository: NovelRepository) {
    /**
     * Returns a [Flow] of the list of saved [Novel]s.
     */
    operator fun invoke(): Flow<List<Novel>> = repository.getSavedNovels()
}
