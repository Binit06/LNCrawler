package com.halovoid.lncrawler.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.api.core.crawler.CrawlerFactory
import com.halovoid.lncrawler.domain.models.SearchItem
import com.halovoid.lncrawler.domain.models.SearchResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GlobalSearchViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    fun search(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            try {
                val crawlers = CrawlerFactory.getCrawlers()
                val deferredResults = crawlers.map { crawler ->
                    async {
                        try {
                            val results = crawler.getSearchResults(query)
                            crawler.name to results.map { novel ->
                                SearchItem(
                                    title = novel.title,
                                    source = novel.crawlerName,
                                    url = novel.url,
                                    description = novel.description ?: "",
                                    score = 0.0,
                                    imageUrl = novel.coverHttpsUrl ?: novel.coverUrl
                                )
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("GlobalSearchViewModel", "Error searching ${crawler.name}: ${e.message}", e)
                            crawler.name to emptyList()
                        }
                    }
                }

                val allResults = deferredResults.awaitAll()
                    .filter { it.second.isNotEmpty() }
                    .toMap()

                _searchState.value = SearchState.Success(
                    SearchResponse(
                        query = query,
                        results = allResults
                    )
                )
            } catch (e: Exception) {
                _searchState.value = SearchState.Error(e.message ?: "An unexpected error occurred.")
            }
        }
    }

    fun resetState() {
        _searchState.value = SearchState.Idle
    }
}
