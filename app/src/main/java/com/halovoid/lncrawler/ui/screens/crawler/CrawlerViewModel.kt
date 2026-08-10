package com.halovoid.lncrawler.ui.screens.crawler

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.api.core.crawler.Crawler
import com.halovoid.lncrawler.api.core.crawler.CrawlerFactory
import com.halovoid.lncrawler.api.loader.SourceLoader
import com.halovoid.lncrawler.data.repository.PreferenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val error: String) : SyncState()
}

class CrawlerViewModel(application: Application) : AndroidViewModel(application) {
    private val sourceLoader = SourceLoader(application)
    private val preferenceRepository = PreferenceRepository(application)
    
    private val _crawlers = MutableStateFlow(CrawlerFactory.getCrawlers())
    val crawlers: StateFlow<List<Crawler>> = _crawlers.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _isUpdateAvailable = MutableStateFlow(false)
    val isUpdateAvailable: StateFlow<Boolean> = _isUpdateAvailable.asStateFlow()

    private val _showSyncOption = MutableStateFlow(false)
    val showSyncOption: StateFlow<Boolean> = _showSyncOption.asStateFlow()

    private var latestReleaseInfo: SourceLoader.ReleaseInfo? = null

    init {
        checkForUpdates()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            try {
                val currentTag = preferenceRepository.currentDexTag.first()
                if (currentTag == null) {
                    _showSyncOption.value = true
                    return@launch
                }

                val info = sourceLoader.fetchLatestReleaseInfo()
                latestReleaseInfo = info
                
                if (info.tagName != currentTag) {
                    _isUpdateAvailable.value = true
                    _showSyncOption.value = true
                } else {
                    _isUpdateAvailable.value = false
                    _showSyncOption.value = false
                }
            } catch (e: Exception) {
                // Ignore update check errors silently or log them
            }
        }
    }

    fun syncCrawlers() {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            try {
                sourceLoader.loadSources(latestReleaseInfo)
                _crawlers.value = CrawlerFactory.getCrawlers()
                _syncState.value = SyncState.Success("Crawlers updated successfully")
                _isUpdateAvailable.value = false
                _showSyncOption.value = false
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Failed to sync crawlers")
            }
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }
}
