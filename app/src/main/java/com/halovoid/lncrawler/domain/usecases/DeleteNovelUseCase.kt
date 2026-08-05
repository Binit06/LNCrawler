package com.halovoid.lncrawler.domain.usecases

import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.domain.models.Novel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * UseCase for deleting a novel from the local database.
 */
class DeleteNovelUseCase(private val repository: NovelRepository) {
    suspend operator fun invoke(novel: Novel) = withContext(Dispatchers.IO) {
        repository.deleteNovel(novel)
    }
}
