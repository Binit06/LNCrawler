package com.halovoid.lncrawler.ui.screens.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.BuildConfig
import com.halovoid.lncrawler.api.loader.AppUpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AppUpdateState {
    object Idle : AppUpdateState()
    object Loading : AppUpdateState()
    data class UpdateAvailable(val tagName: String, val releaseUrl: String) : AppUpdateState()
    object UpToDate : AppUpdateState()
    data class Error(val message: String) : AppUpdateState()
}

class SupportViewModel : ViewModel() {
    private val appUpdateManager = AppUpdateManager()

    private val _updateState = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val updateState: StateFlow<AppUpdateState> = _updateState.asStateFlow()

    init {
        checkForUpdates()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateState.value = AppUpdateState.Loading
            try {
                val latest = appUpdateManager.fetchLatestAppRelease()
                val currentVersion = BuildConfig.VERSION_NAME
                
                if (AppUpdateManager.isUpdateAvailable(currentVersion, latest.tagName)) {
                    _updateState.value = AppUpdateState.UpdateAvailable(latest.tagName, latest.releaseUrl)
                } else {
                    _updateState.value = AppUpdateState.UpToDate
                }
            } catch (e: Exception) {
                _updateState.value = AppUpdateState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
