package com.halovoid.lncrawler.domain.usecases

import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.domain.models.Novel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use case to search for novels across supported sources.
 * Currently serves as a placeholder for future search implementations.
 */
class SearchNovelsUseCase(private val repository: NovelRepository) {
    /**
     * Searches for novels matching the query using the specified crawler.
     *
     * @param crawlerName The name of the crawler to perform the search.
     * @param query The search term.
     * @return A list of [Novel] results (currently empty).
     */
    suspend operator fun invoke(crawlerName: String, query: String): List<Novel> = withContext(Dispatchers.IO) {
        // Search is not yet implemented in NovelRepository with DB, just a wrapper
        // Since the user wants to focus on direct URL entry and saved novels:
        emptyList() 
    }
}
