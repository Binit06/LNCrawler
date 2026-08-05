package com.halovoid.lncrawler.domain.usecases

import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.domain.models.Novel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use case to fetch detailed information about a novel, including its chapter list.
 * This use case coordinates with the repository to decide whether to fetch from network or cache.
 */
class GetNovelDetailsUseCase(private val repository: NovelRepository) {
    /**
     * Executes the use case to retrieve novel details.
     *
     * @param crawlerName The name of the crawler to use for fetching details.
     * @param novelUrl The source URL of the novel.
     * @return The [Novel] details or null if the operation fails.
     */
    suspend operator fun invoke(crawlerName: String, novelUrl: String): Novel? = withContext(Dispatchers.IO) {
        repository.getNovelDetails(crawlerName, novelUrl)
    }
}
