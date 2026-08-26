package com.halovoid.lncrawler.ui.screens.novel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.data.repository.RequestRepository
import com.halovoid.lncrawler.domain.models.Request
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GroupedRequestsViewModel(
    application: Application,
    private val requestRepository: RequestRepository
) : AndroidViewModel(application) {

    private val _context = MutableStateFlow<Pair<String, String>?>(null)
    
    private val _statusFilter = MutableStateFlow<RequestStatus?>(null)
    val statusFilter: StateFlow<RequestStatus?> = _statusFilter.asStateFlow()

    fun setStatusFilter(status: RequestStatus?) {
        _statusFilter.value = status
    }

    val cancellingRequestIds: StateFlow<Set<String>> = requestRepository.cancellingRequestIds
    val activeActionIds: StateFlow<Set<String>> = requestRepository.activeActionIds

    @OptIn(ExperimentalCoroutinesApi::class)
    val requests: StateFlow<List<Request>> = combine(_context.filterNotNull(), _statusFilter) { context, status ->
        context to status
    }
        .flatMapLatest { (context, status) ->
            val (type, value) = context
            val baseFlow = when (type) {
                "ALL" -> requestRepository.getRootRequests()
                "NOVEL" -> requestRepository.getRootRequestByNovelFlow(value)
                "DEPENDENCY" -> requestRepository.getRequestsByDependenceFlow(value)
                else -> flowOf(emptyList())
            }
            
            baseFlow.map { list ->
                if (status == null) list else list.filter { it.status == status }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun loadRequests(contextType: String, contextValue: String) {
        _context.value = contextType to contextValue
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

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            requestRepository.cancelRequest(requestId)
        }
    }

    fun resolveCloudflare(requestId: String, url: String) {
        viewModelScope.launch {
            com.halovoid.lncrawler.api.core.scrapper.Scrapper.globalResolver?.resolve(url)
            requestRepository.replayRequest(requestId)
        }
    }
}
