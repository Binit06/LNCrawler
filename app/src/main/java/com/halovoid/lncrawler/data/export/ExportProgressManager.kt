package com.halovoid.lncrawler.data.export

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton manager to track ongoing export progress across the app.
 * Provides a way for background workers to report progress and UI components to observe it.
 * Progress is tracked by both Record ID and Novel URL to support independent history tracking
 * and easy access from detail screens.
 */
object ExportProgressManager {
    private val _recordProgressMap = MutableStateFlow<Map<Long, ExportProgress>>(emptyMap())
    private val _urlProgressMap = MutableStateFlow<Map<String, ExportProgress>>(emptyMap())

    /** A map of Record IDs to their current [ExportProgress]. */
    val recordProgressMap: StateFlow<Map<Long, ExportProgress>> = _recordProgressMap.asStateFlow()
    
    /** A map of Novel URLs to their current [ExportProgress] (always the latest active one). */
    val urlProgressMap: StateFlow<Map<String, ExportProgress>> = _urlProgressMap.asStateFlow()

    /**
     * Updates the progress for a specific record and its associated novel.
     * @param recordId The unique database ID of the export record.
     * @param novelUrl The URL of the novel.
     * @param progress The new progress state.
     */
    fun updateProgress(recordId: Long, novelUrl: String, progress: ExportProgress?) {
        _recordProgressMap.update { current ->
            if (progress == null) current - recordId else current + (recordId to progress)
        }
        _urlProgressMap.update { current ->
            if (progress == null) current - novelUrl else current + (novelUrl to progress)
        }
    }

    /**
     * Clears progress for a specific record and novel.
     */
    fun clearProgress(recordId: Long, novelUrl: String) {
        updateProgress(recordId, novelUrl, null)
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
