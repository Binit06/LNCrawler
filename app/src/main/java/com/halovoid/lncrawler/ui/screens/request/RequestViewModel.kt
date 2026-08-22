package com.halovoid.lncrawler.ui.screens.request

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.api.core.crawler.CrawlerFactory
import com.halovoid.lncrawler.api.core.scrapper.Scrapper
import com.halovoid.lncrawler.data.db.dao.RequestDao
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.data.redis.RedisManager
import com.halovoid.lncrawler.data.scheduler.services.SchedulerService
import com.halovoid.lncrawler.data.repository.RequestRepository
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.domain.models.toDomain
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

class RequestViewModel(
    application: Application,
    private val requestRepository: RequestRepository
) : AndroidViewModel(application) {

    /** Tracks validation errors for the URL input field. */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val cancellingRequestIds: StateFlow<Set<String>> = requestRepository.cancellingRequestIds
    val activeActionIds: StateFlow<Set<String>> = requestRepository.activeActionIds

    fun resolveCloudflare(requestId: String, url: String) {
        viewModelScope.launch {
            android.util.Log.i("RequestViewModel", "Starting Cloudflare resolution for $requestId at $url")
            val success = Scrapper.globalResolver?.resolve(url) ?: false
            android.util.Log.i("RequestViewModel", "Resolution result: $success")
            if (success) {
                // Reset status so scheduler can try again
                android.util.Log.i("RequestViewModel", "Replaying request $requestId")
                requestRepository.replayRequest(requestId)
            }
        }
    }

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
                type = RequestType.NOVEL_METADATA,
                novelUrl = url,
                name = "Metadata: $url",
                metadata = metadata,
                status = RequestStatus.PENDING,
                dependsOn = null,
                url = url,
                priority = 0,
                completedAt = null,
                parentNovel = null
            )

            requestRepository.insertRequests(listOf(request))

            RedisManager.pushUrl(url)

            SchedulerService.startService(getApplication())

            _isLoading.value = false
        }
    }

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            requestRepository.cancelRequest(requestId)
        }
    }

    fun replayRequest(requestId: String) {
        viewModelScope.launch {
            requestRepository.replayRequest(requestId)
        }
    }

    fun resumeRequest(requestId: String) {
        viewModelScope.launch {
            requestRepository.resumeRequest(requestId)
        }
    }

    // Since use of FLOW Progress Data for all request is emitted as the records are updated
    val requestHistory: StateFlow<List<Request>> = requestRepository.getRootRequests()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteRequestRecord(id: String, requestId: Int) {
        viewModelScope.launch {
            requestRepository.requestDao.deleteById(id)
        }
    }
}
