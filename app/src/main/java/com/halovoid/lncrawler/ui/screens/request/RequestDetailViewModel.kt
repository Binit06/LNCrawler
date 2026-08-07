package com.halovoid.lncrawler.ui.screens.request

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.data.db.dao.RequestDao
import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.domain.models.toDomain
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Request Detail screen.
 */
class RequestDetailViewModel(
    application: Application,
    private val requestDao: RequestDao
) : AndroidViewModel(application) {
    private val repository = NovelRepository(application)

    private val _novel = MutableStateFlow<Novel?>(null)
    val novel: StateFlow<Novel?> = _novel.asStateFlow()

    /** Observes the specific record from the database as a hot flow. */
    fun getRequest(requestId: Int): Flow<Request?> {
        TODO()
    }

    fun deleteHistoryRecord(id: Int, novelUrl: String) {
        TODO()
    }
    
    fun replayRequest(destinationUri: android.net.Uri, request: Request) {
        // TODO: Implement Replay Request Feature
    }
}
