package com.halovoid.lncrawler.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.data.crawler.core.CrawlerFactory
import com.halovoid.lncrawler.data.export.EpubExporter
import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.domain.usecases.GetNovelDetailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the [NovelDetailScreen] in the UI layer.
 * Manages fetching novel details and coordinates the background export process.
 */
class NovelDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NovelRepository(application)
    private val getNovelDetailsUseCase = GetNovelDetailsUseCase(repository)
    private val epubExporter = EpubExporter()

    /** The current novel being displayed. */
    private val _novel = MutableStateFlow<Novel?>(null)
    val novel: StateFlow<Novel?> = _novel

    /** Tracks whether an export operation is currently in progress. */
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting

    /** Current progress message for the export process (e.g., "Fetching chapter 1/10"). */
    private val _exportProgress = MutableStateFlow<String?>(null)
    val exportProgress: StateFlow<String?> = _exportProgress

    /**
     * Loads the novel details from the repository.
     * 
     * @param crawlerName The crawler to use.
     * @param novelUrl The novel's URL.
     */
    fun loadNovel(crawlerName: String, novelUrl: String) {
        viewModelScope.launch {
            _novel.value = getNovelDetailsUseCase(crawlerName, novelUrl)
        }
    }

    /**
     * Executes the novel export to the specified URI using Android's Storage Access Framework (SAF).
     * Fetches all chapter contents sequentially before generating the EPUB.
     *
     * @param crawlerName The crawler name to use for fetching chapter content.
     * @param uri The destination URI provided by SAF.
     * @param onComplete Callback invoked when export finishes successfully.
     */
    fun exportToUri(crawlerName: String, uri: Uri, onComplete: () -> Unit) {
        val currentNovel = _novel.value ?: return
        viewModelScope.launch {
            _isExporting.value = true
            val crawler = CrawlerFactory.getCrawler(crawlerName) ?: return@launch
            
            val chaptersToExport = mutableListOf<Pair<String, String>>()
            currentNovel.chapters.forEachIndexed { index, chapter ->
                _exportProgress.value = "Fetching chapter ${index + 1}/${currentNovel.chapters.size}"
                var content = crawler.getChapterContent(chapter.url)
                if (content.isEmpty()) {
                    content = "<p><i>[Error: Failed to fetch chapter content]</i></p>"
                }
                chaptersToExport.add(chapter.title to content)
            }
            
            _exportProgress.value = "Generating EPUB..."
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { outputStream ->
                epubExporter.export(currentNovel, chaptersToExport, outputStream)
            }
            
            _isExporting.value = false
            _exportProgress.value = null
            onComplete()
        }
    }
}
