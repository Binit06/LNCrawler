package com.halovoid.lncrawler.ui.screens.crawler

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.api.core.crawler.Crawler
import com.halovoid.lncrawler.api.core.crawler.CrawlerFactory
import com.halovoid.lncrawler.api.loader.SourceLoader
import com.halovoid.lncrawler.api.loader.VersionUtils
import com.halovoid.lncrawler.data.repository.PreferenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    data class Success(val message: String) : SyncState()
    data class Incompatible(val minVersion: String) : SyncState()
    data class Error(val error: String) : SyncState()
}

class CrawlerViewModel(
    application: Application,
    private val preferenceRepository: PreferenceRepository
) : AndroidViewModel(application) {
    private val sourceLoader = SourceLoader(application)
    
    val crawlers: StateFlow<List<Crawler>> = CrawlerFactory.crawlersFlow

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _isUpdateAvailable = MutableStateFlow(false)
    val isUpdateAvailable: StateFlow<Boolean> = _isUpdateAvailable.asStateFlow()

    val showSyncOption: StateFlow<Boolean> = combine(
        preferenceRepository.currentDexTag,
        _isUpdateAvailable,
        crawlers
    ) { currentTag, updateAvailable, crawlerList ->
        currentTag == null || updateAvailable || crawlerList.isEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private var latestReleaseInfo: SourceLoader.ReleaseInfo? = null

    init {
        viewModelScope.launch {
            sourceLoader.loadLocalSources()
        }
        checkForUpdates()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            try {
                val currentTag = preferenceRepository.currentDexTag.first() ?: return@launch

                val info = sourceLoader.fetchLatestReleaseInfo()
                latestReleaseInfo = info
                
                _isUpdateAvailable.value = VersionUtils.isUpdateAvailable(currentTag, info.tagName)
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
                _syncState.value = SyncState.Success("Crawlers updated successfully")
                _isUpdateAvailable.value = false
            } catch (e: SourceLoader.IncompatibleAppException) {
                _syncState.value = SyncState.Incompatible(e.minVersion)
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Failed to sync crawlers")
            }
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }
}
