package com.halovoid.lncrawler.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.data.db.AppDatabase
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.data.repository.ArtifactRepository
import com.halovoid.lncrawler.data.repository.ChapterRepository
import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.data.repository.VolumeRepository
import com.halovoid.lncrawler.data.scheduler.services.SchedulerService
import com.halovoid.lncrawler.domain.models.Artifact
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.domain.models.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

/**
 * ViewModel for the [NovelDetailScreen].
 */
class NovelDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val novelRepository = NovelRepository(application)
    private val volumeRepository = VolumeRepository(application)

    private val artifactRepository = ArtifactRepository(application)
    private val chapterRepository = ChapterRepository(application)
    private val database = AppDatabase.getDatabase(application)

    private val requestDao = database.requestDao()

    private val _novel = MutableStateFlow<Novel?>(null)
    val novel: StateFlow<Novel?> = _novel.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val rootRequests: StateFlow<List<Request>> = novel
        .filterNotNull()
        .flatMapLatest { nov ->
            requestDao.getRootRequestByNovelFlow(nov.url)
                .map { entities -> entities.map { it.toDomain() } }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val artifacts: StateFlow<List<Artifact>> = novel
        .filterNotNull()
        .flatMapLatest { nov ->
            artifactRepository.getArtifactsByNovelFlow(nov.url)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

    fun startFileExport(artifactId: Int, destinationUri: String) {
        val currentNovel = _novel.value ?: return
        viewModelScope.launch {
            val metadata = JSONObject().apply {
                put("artifactId", artifactId)
                put("destinationUri", destinationUri)
            }.toString()

            val request = RequestEntity(
                // Use a unique ID to allow multiple exports if needed
                id = "${currentNovel.url}_file_save_${System.currentTimeMillis()}",
                type = RequestType.EXPORT,
                novelUrl = currentNovel.url,
                name = "Saving File: ${currentNovel.title}",
                metadata = metadata,
                parentNovel = currentNovel.url,
                url = null,
                completedAt = null,
                status = RequestStatus.PENDING,
                priority = 10 // Higher priority for user-initiated exports
            )

            requestDao.insertRequests(listOf(request))

            // Trigger the scheduler to process the new Export task
            SchedulerService.startService(getApplication())
        }
    }

    fun copyArtifactToUri(artifact: Artifact, destinationUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sourceFile = File(artifact.artifactDestination)

                if (!sourceFile.exists()) {
                    return@launch
                }

                getApplication<Application>().contentResolver
                    .openOutputStream(destinationUri)
                    ?.use { outputStream ->
                        sourceFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getArtifactIdForFormat(format: String): Int? {
        val novelUrl = _novel.value?.url ?: return null
        return rootRequests.value
            .filter { it.type == RequestType.ARTIFACT && it.status == RequestStatus.SUCCESS }
            .find { it.name.contains(format, ignoreCase = true) }
            ?.let { it.id.hashCode() }
    }
}
