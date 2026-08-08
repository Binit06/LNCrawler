package com.halovoid.lncrawler.ui.screens.request

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.data.crawler.core.crawler.CrawlerFactory
import com.halovoid.lncrawler.data.db.dao.RequestDao
import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.domain.models.toDomain
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the [RequestScreen] in the UI layer.
 * Manages URL validation, saved novels list, and background export tasks.
 */
class RequestViewModel(
    application: Application,
    private val requestDao: RequestDao
) : AndroidViewModel(application) {
    private val repository = NovelRepository(application)

    /** Tracks validation errors for the URL input field. */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** Set of URLs currently being fetched from the network (metadata/chapters). */
    private val _activeFetches = MutableStateFlow<Set<String>>(emptySet())
    val activeFetches: StateFlow<Set<String>> = _activeFetches

    /**
     * Validates if the given URL can be handled by any registered crawler.
     */
    fun validateUrl(url: String): String? {
        val crawler = CrawlerFactory.getCrawlerByUrl(url)
        return if (crawler != null) {
            _error.value = null
            crawler.name
        } else {
            _error.value = "URL not supported or invalid"
            null
        }
    }

    /**
     * Starts fetching a novel from the network and saves it to the DB.
     */
    fun fetchMetadata(crawlerName: String, url: String) {
        // TODO: Implement Crawler fetching the novel
    }

    val requestHistory: StateFlow<List<Request>> = requestDao.getRootRequests()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteRequestRecord(id: String, requestId: Int) {
        viewModelScope.launch {
            requestDao.deleteById(id)
        }
    }
}