package com.halovoid.lncrawler.data.export

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton manager to track ongoing export progress across the app.
 * Provides a way for background workers to report progress and UI components to observe it.
 */
object ExportProgressManager {
    private val _progressMap = MutableStateFlow<Map<String, ExportProgress>>(emptyMap())
    /** A map of novel URLs to their current [ExportProgress]. */
    val progressMap: StateFlow<Map<String, ExportProgress>> = _progressMap.asStateFlow()

    /**
     * Updates the progress for a specific novel.
     * @param novelUrl The unique identifier (URL) of the novel.
     * @param progress The new progress state.
     */
    fun updateProgress(novelUrl: String, progress: ExportProgress?) {
        _progressMap.update { current ->
            if (progress == null) {
                current - novelUrl
            } else {
                current + (novelUrl to progress)
            }
        }
    }
}

/**
 * Data class representing the state of an ongoing export operation.
 */
data class ExportProgress(
    val currentChapter: Int,
    val totalChapters: Int,
    val status: String
) {
    /** The normalized progress value between 0.0 and 1.0. */
    val progress: Float = if (totalChapters > 0) currentChapter.toFloat() / totalChapters else 0f
}
