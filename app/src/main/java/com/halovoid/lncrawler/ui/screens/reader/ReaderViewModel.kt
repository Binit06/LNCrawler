package com.halovoid.lncrawler.ui.screens.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.data.repository.ChapterRepository
import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.data.repository.ReaderRepository
import com.halovoid.lncrawler.domain.models.Chapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class LoadedChapter(
    val chapter: Chapter,
    val paragraph: List<String>
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val chapterRepository = ChapterRepository(application)
    private val novelRepository = NovelRepository(application)
    private val readerRepository = ReaderRepository(application)

    private var allChapters: List<Chapter> = emptyList()
    private var crawlerName: String = ""
    private var centerPos: Int = -1
    private val windowMutex = Mutex()

    private val contentCache = mutableMapOf<Int, List<String>>()

    private val _window = MutableStateFlow<List<LoadedChapter>>(emptyList())
    val window: StateFlow<List<LoadedChapter>> = _window.asStateFlow()
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun start(novelUrl: String, initialChapterId: Int) {
        if (allChapters.isNotEmpty()) return //reading has already started
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            crawlerName = novelRepository.getNovelDetails(novelUrl)?.crawlerName.orEmpty()
            allChapters = chapterRepository.getChaptersByNovelUrl(novelUrl).sortedBy { it.index }
            val startPos = allChapters.indexOfFirst { it.id == initialChapterId }
                .let { if (it == -1) 0 else it }
            centerPos = startPos
            shiftWindow(startPos)
            _isLoading.value = false
        }
    }

    fun onCenterChapterChanged(chapterId: Int) {
        val pos = allChapters.indexOfFirst { it.id == chapterId }
        if (pos == -1 || pos == centerPos) return
        centerPos = pos
        viewModelScope.launch(Dispatchers.IO) {
            shiftWindow(pos)
        }
    }

    private suspend fun shiftWindow(centerPosition: Int) {
        windowMutex.withLock {
            val position = (centerPosition -1..centerPosition + 1).filter { it in allChapters.indices }

            val loaded = position.map { pos ->
                val chapter = allChapters[pos]
                val paragraphs = contentCache.getOrPut(chapter.id) {
                    readerRepository.getChapterContent(chapter, crawlerName)
                }
                LoadedChapter(chapter, paragraphs)
            }

            _window.value = loaded

            val keep = position.mapNotNull { allChapters.getOrNull(it)?.id }.toSet()
            contentCache.keys.retainAll { it in keep }
        }
    }

}