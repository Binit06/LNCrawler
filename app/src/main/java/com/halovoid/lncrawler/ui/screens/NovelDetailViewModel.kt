package com.halovoid.lncrawler.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.data.db.AppDatabase
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.data.repository.ChapterRepository
import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.data.repository.VolumeRepository
import com.halovoid.lncrawler.data.scheduler.services.SchedulerService
import com.halovoid.lncrawler.domain.models.Novel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ViewModel for the [NovelDetailScreen].
 */
class NovelDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val novelRepository = NovelRepository(application)
    private val volumeRepository = VolumeRepository(application)
    private val chapterRepository = ChapterRepository(application)
    private val database = AppDatabase.getDatabase(application)

    private val requestDao = database.requestDao()

    private val _novel = MutableStateFlow<Novel?>(null)
    val novel: StateFlow<Novel?> = _novel.asStateFlow()

    fun loadNovel(novelUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val details = novelRepository.getNovelDetails(novelUrl)
            val volumeDetails = volumeRepository.getVolumeByNovelUrl(novelUrl)
            val chapterDetails = chapterRepository.getChaptersByNovelUrl(novelUrl)
            _novel.value = details?.copy(
                volumes = volumeDetails,
                chapters = chapterDetails
            )
        }
    }

    fun startBackgroundExport(novel: Novel, format: String) {
        viewModelScope.launch {
            val metadata = JSONObject().apply {
                put("format", format)
                put("crawlerName", novel.crawlerName)
            }.toString()

            val request = RequestEntity(
                id = "${novel.url}_export_$format",
                type = RequestType.ARTIFACT,
                novelUrl = novel.url,
                name = "Export: ${novel.title} ($format)",
                metadata = metadata,
                parentNovel = novel.url,
                url = null,
                completedAt = null
            )

            requestDao.insertRequests(listOf(request))

            SchedulerService.startService(getApplication())
        }
    }

    fun fetchNovelMetadata(novel: Novel) {
        viewModelScope.launch {
            val metadata = JSONObject().apply {
                put("crawlerName", novel.crawlerName)
            }.toString()

            val request = RequestEntity(
                id = "${novel.url}_metadata",
                type = RequestType.NOVEL_METADATA,
                novelUrl = novel.url,
                name = "Metadata: ${novel.url}",
                metadata = metadata,
                status = RequestStatus.PENDING,
                dependsOn = null,
                url = novel.url,
                priority = 0,
                completedAt = null,
                parentNovel = novel.url
            )

            requestDao.insertRequests(listOf(request))

            SchedulerService.startService(getApplication())
        }
    }

    // Replay full Novel Request from start to end
    fun fetchFullNovel(novel: Novel) {
        viewModelScope.launch {
            val requestId = "${novel.url}_crawl"

            requestDao.replayRequest(requestId)

            SchedulerService.startService(getApplication())
        }
    }

    // Replay Volume Request from Start to End
    fun fetchVolume(novel: Novel, volumeIndex: Int) {
        viewModelScope.launch {
            val requestId = "${novel.url}_vol_$volumeIndex"

            requestDao.replayRequest(requestId)

            SchedulerService.startService(getApplication())
        }
    }
}
