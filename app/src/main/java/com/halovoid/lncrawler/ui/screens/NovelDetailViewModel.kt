package com.halovoid.lncrawler.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.domain.models.Novel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the [NovelDetailScreen].
 * Manages novel data and triggers background export tasks via WorkManager.
 */
class NovelDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NovelRepository(application)
    private val workManager = WorkManager.getInstance(application)

    private val _novel = MutableStateFlow<Novel?>(null)
    val novel: StateFlow<Novel?> = _novel

    /** Observes the global export progress map keyed by Novel URL. */

    fun loadNovel(crawlerName: String, novelUrl: String) {
        TODO()
    }

    /**
     * Enqueues a WorkManager request to handle the export in the background.
     * @param novel The novel to export.
     * @param destinationUri The destination URI from SAF.
     */
    fun startBackgroundExport(novel: Novel, destinationUri: Uri) {
        TODO()
    }

    /**
     * Cancels an ongoing export for the given novel.
     */
    fun cancelExport(novelUrl: String) {
        workManager.cancelUniqueWork(novelUrl)
        // Note: urlProgressMap will be cleared by the worker's finally block
    }
}
