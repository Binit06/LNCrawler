package com.halovoid.lncrawler.ui.screens.request

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.data.crawler.core.CrawlerFactory
import com.halovoid.lncrawler.data.export.EpubExporter
import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.domain.usecases.GetSavedNovelsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the [RequestScreen] in the UI layer.
 * Handles validation of source URLs and provides a stream of recently saved novels.
 */
class RequestViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NovelRepository(application)
    private val getSavedNovelsUseCase = GetSavedNovelsUseCase(repository)
    private val epubExporter = EpubExporter()

    /** Tracks validation errors for the URL input field. */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** The URL of the novel currently being exported, if any. */
    private val _isExporting = MutableStateFlow<String?>(null) 
    val isExporting: StateFlow<String?> = _isExporting

    /** Tracks progress description during the export process. */
    private val _exportProgress = MutableStateFlow<String?>(null)
    val exportProgress: StateFlow<String?> = _exportProgress

    /** Flow of novels saved in the local database. */
    val savedNovels: StateFlow<List<Novel>> = getSavedNovelsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Validates if the given URL can be handled by any registered crawler.
     * 
     * @param url The URL to validate.
     * @return The name of the matching crawler, or null if invalid.
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
     * Initiates an export of a [Novel] to a specified URI using SAF.
     * 
     * @param novel The novel data to export.
     * @param uri The destination file URI.
     * @param onComplete Success callback.
     */
    fun exportNovelToUri(novel: Novel, uri: Uri, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isExporting.value = novel.url
            val crawler = CrawlerFactory.getCrawlerByUrl(novel.url) ?: return@launch
            
            val chaptersToExport = mutableListOf<Pair<String, String>>()
            novel.chapters.forEachIndexed { index, chapter ->
                _exportProgress.value = "Fetching ${index + 1}/${novel.chapters.size}"
                var content = crawler.getChapterContent(chapter.url)
                if (content.isEmpty()) {
                    content = "<p><i>[Error: Failed to fetch chapter content]</i></p>"
                }
                chaptersToExport.add(chapter.title to content)
            }
            
            _exportProgress.value = "Finalizing..."
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { outputStream ->
                epubExporter.export(novel, chaptersToExport, outputStream)
            }
            
            _isExporting.value = null
            _exportProgress.value = null
            onComplete()
        }
    }
}
