package com.halovoid.lncrawler.data.repository

import android.content.Context
import com.halovoid.lncrawler.BuildConfig
import com.halovoid.lncrawler.api.loader.AppUpdateManager
import com.halovoid.lncrawler.api.loader.SourceLoader
import com.halovoid.lncrawler.api.loader.VersionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class UpdateRepository private constructor(context: Context) {
    private val appUpdateManager = AppUpdateManager()
    private val sourceLoader = SourceLoader(context)
    private val preferenceRepository = PreferenceRepository.getInstance(context)

    private val _isAppUpdateAvailable = MutableStateFlow(false)
    val isAppUpdateAvailable: StateFlow<Boolean> = _isAppUpdateAvailable.asStateFlow()

    private val _latestAppRelease = MutableStateFlow<AppUpdateManager.AppReleaseInfo?>(null)
    val latestAppRelease: StateFlow<AppUpdateManager.AppReleaseInfo?> = _latestAppRelease.asStateFlow()

    private val _isCrawlerUpdateAvailable = MutableStateFlow(false)
    val isCrawlerUpdateAvailable: StateFlow<Boolean> = _isCrawlerUpdateAvailable.asStateFlow()

    suspend fun checkForUpdates() {
        checkAppUpdate()
        checkCrawlerUpdate()
    }

    private suspend fun checkAppUpdate() {
        val info = appUpdateManager.fetchLatestAppRelease()
        _latestAppRelease.value = info
        _isAppUpdateAvailable.value = VersionUtils.isUpdateAvailable(BuildConfig.VERSION_NAME, info.tagName)
    }

    private suspend fun checkCrawlerUpdate() {
        val currentTag = preferenceRepository.currentDexTag.first()
        val info = sourceLoader.fetchLatestReleaseInfo()
        _isCrawlerUpdateAvailable.value = VersionUtils.isUpdateAvailable(currentTag, info.tagName)
    }

    fun setCrawlerUpdateAvailable(available: Boolean) {
        _isCrawlerUpdateAvailable.value = available
    }

    companion object {
        @Volatile
        private var INSTANCE: UpdateRepository? = null

        fun getInstance(context: Context): UpdateRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UpdateRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
