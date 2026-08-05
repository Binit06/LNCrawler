package com.halovoid.lncrawler.ui.screens.request

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.halovoid.lncrawler.data.crawler.core.CrawlerFactory
import com.halovoid.lncrawler.data.export.ExportProgress
import com.halovoid.lncrawler.data.export.ExportProgressManager
import com.halovoid.lncrawler.data.export.ExportWorker
import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.domain.usecases.DeleteNovelUseCase
import com.halovoid.lncrawler.domain.usecases.GetNovelDetailsUseCase
import com.halovoid.lncrawler.domain.usecases.GetSavedNovelsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the [RequestScreen] in the UI layer.
 * Manages URL validation, saved novels list, and background export tasks.
 */
class RequestViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NovelRepository(application)
    private val getSavedNovelsUseCase = GetSavedNovelsUseCase(repository)
    private val getNovelDetailsUseCase = GetNovelDetailsUseCase(repository)
    private val deleteNovelUseCase = DeleteNovelUseCase(repository)
    private val workManager = WorkManager.getInstance(application)

    /** Tracks validation errors for the URL input field. */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** Map of ongoing export progress. */
    val exportProgressMap: StateFlow<Map<String, ExportProgress>> = ExportProgressManager.progressMap

    /** Set of URLs currently being fetched from the network (metadata/chapters). */
    private val _activeFetches = MutableStateFlow<Set<String>>(emptySet())
    val activeFetches: StateFlow<Set<String>> = _activeFetches

    /** Flow of novels saved in the local database. */
    val savedNovels: StateFlow<List<Novel>> = getSavedNovelsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Validates if the given URL can be handled by any registered crawler.
     */
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

    /**
     * Starts fetching a novel from the network and saves it to the DB.
     */
    fun fetchNovel(crawlerName: String, url: String, onSuccess: (String, String) -> Unit) {
        if (_activeFetches.value.contains(url)) return
        
        viewModelScope.launch {
            _activeFetches.update { it + url }
            try {
                val novel = getNovelDetailsUseCase(crawlerName, url)
                if (novel != null) {
                    onSuccess(crawlerName, url)
                } else {
                    _error.value = "Failed to fetch novel data"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
            } finally {
                _activeFetches.update { it - url }
            }
        }
    }

    /**
     * Starts a background worker to fetch content and generate EPUB.
     * @param novel The novel to export.
     * @param destinationUri The destination URI picked by the user.
     */
    fun startExport(novel: Novel, destinationUri: Uri) {
        val inputData = Data.Builder()
            .putString("novelUrl", novel.url)
            .putString("crawlerName", novel.crawlerName ?: "NovelBins")
            .putString("destinationUri", destinationUri.toString())
            .build()
            
        val request = OneTimeWorkRequestBuilder<ExportWorker>()
            .setInputData(inputData)
            .build()
            
        // Use unique work based on URL to avoid duplicate exports for the same novel
        workManager.enqueueUniqueWork(
            novel.url,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Cancels an ongoing export for the given novel.
     */
    fun cancelExport(novelUrl: String) {
        workManager.cancelUniqueWork(novelUrl)
        ExportProgressManager.updateProgress(novelUrl, null)
    }

    /**
     * Removes a novel from the local history.
     */
    fun removeNovel(novel: Novel) {
        viewModelScope.launch {
            deleteNovelUseCase(novel)
        }
    }
}
