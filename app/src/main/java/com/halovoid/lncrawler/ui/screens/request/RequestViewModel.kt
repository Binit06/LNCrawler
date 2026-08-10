package com.halovoid.lncrawler.ui.screens.request

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.api.core.crawler.CrawlerFactory
import com.halovoid.lncrawler.data.db.dao.RequestDao
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.data.scheduler.services.SchedulerService
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.domain.models.toDomain
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

class RequestViewModel(
    application: Application,
    private val requestDao: RequestDao
) : AndroidViewModel(application) {

    /** Tracks validation errors for the URL input field. */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

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

    fun startNovelCrawl(crawlerName: String, url: String) {
        viewModelScope.launch {
            _isLoading.value = true

            val metadata = JSONObject().apply {
                put("crawlerName", crawlerName)
            }.toString()

            val request = RequestEntity(
                id = "${url}_crawl",
                type = RequestType.FULL_NOVEL,
                novelUrl = url,
                name = "Crawl: $url",
                metadata = metadata,
                status = RequestStatus.PENDING,
                dependsOn = null,
                url = url,
                priority = 0,
                completedAt = null,
                parentNovel = null
            )

            requestDao.insertRequests(listOf(request))

            SchedulerService.startService(getApplication())

            _isLoading.value = false
        }
    }

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            requestDao.cancelRequest(requestId)

            SchedulerService.cancelJob(getApplication(), requestId)
        }
    }

    fun replayRequest(requestId: String) {
        viewModelScope.launch {
            requestDao.replayRequest(requestId)

            SchedulerService.startService(getApplication())
        }
    }

    // Since use of FLOW Progress Data for all request is emitted as the records are updated
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