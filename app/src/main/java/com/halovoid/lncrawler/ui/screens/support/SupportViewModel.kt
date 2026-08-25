package com.halovoid.lncrawler.ui.screens.support

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.BuildConfig
import com.halovoid.lncrawler.api.loader.AppUpdateManager
import com.halovoid.lncrawler.api.loader.UpdateDownloader
import com.halovoid.lncrawler.api.loader.UpdateInstaller
import com.halovoid.lncrawler.data.repository.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AppUpdateState {
    object Idle : AppUpdateState()
    object Loading : AppUpdateState()
    data class UpdateAvailable(
        val tagName: String,
        val releaseUrl: String,
        val apkDownloadUrl: String?,
        val releaseNotes: String? = null,
        val publishedAt: String? = null
    ) : AppUpdateState()
    object Downloading : AppUpdateState()
    data class ReadyToInstall(val uri: String) : AppUpdateState()
    object Installing : AppUpdateState()
    object UpToDate : AppUpdateState()
    data class Error(val message: String) : AppUpdateState()
}

class SupportViewModel(application: Application) : AndroidViewModel(application) {
    private val updateRepository = UpdateRepository.getInstance(application)
    private val updateDownloader = UpdateDownloader(application)

    private val _updateState = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val updateState: StateFlow<AppUpdateState> = _updateState.asStateFlow()

    init {
        // Observe repository state
        viewModelScope.launch {
            updateRepository.latestAppRelease.collect { latest ->
                if (latest != null) {
                    val currentVersion = BuildConfig.VERSION_NAME
                    if (AppUpdateManager.isUpdateAvailable(currentVersion, latest.tagName)) {
                        _updateState.value = AppUpdateState.UpdateAvailable(
                            latest.tagName,
                            latest.releaseUrl,
                            latest.apkDownloadUrl,
                            latest.body,
                            latest.publishedAt
                        )
                    } else {
                        _updateState.value = AppUpdateState.UpToDate
                    }
                }
            }
        }
        checkForUpdates()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            if (_updateState.value !is AppUpdateState.UpdateAvailable) {
                _updateState.value = AppUpdateState.Loading
            }
            updateRepository.checkForUpdates()
        }
    }

    fun startUpdateDownload(url: String) {
        viewModelScope.launch {
            _updateState.value = AppUpdateState.Downloading
            val downloadId = updateDownloader.downloadUpdate(url, "LNCrawler_update.apk")
            updateDownloader.getDownloadStatus(downloadId).collect { status ->
                when (status) {
                    is UpdateDownloader.DownloadStatus.Success -> {
                        _updateState.value = AppUpdateState.ReadyToInstall(status.uri)
                    }
                    is UpdateDownloader.DownloadStatus.Error -> {
                        _updateState.value = AppUpdateState.Error(status.message)
                    }
                }
            }
        }
    }

    fun installUpdate(context: Context, uri: String) {
        try {
            UpdateInstaller.installApk(context, uri)
            _updateState.value = AppUpdateState.Installing
        } catch (e: Exception) {
            _updateState.value = AppUpdateState.Error("Failed to start installation: ${e.message}")
        }
    }
}
