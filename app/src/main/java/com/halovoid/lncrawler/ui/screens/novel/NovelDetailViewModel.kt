package com.halovoid.lncrawler.ui.screens.novel

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
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.domain.models.toDomain
import com.halovoid.lncrawler.ui.components.artifact.ExportFormat
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

    private val _chapterRange = MutableStateFlow<ClosedFloatingPointRange<Float>>(1f..1f)
    val chapterRange: StateFlow<ClosedFloatingPointRange<Float>> = _chapterRange.asStateFlow()

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
            val updatedNovel = details?.copy(
                volumes = volumeDetails,
                chapters = chapterDetails
            )
            _novel.value = updatedNovel
            if (updatedNovel != null && updatedNovel.chapters.isNotEmpty()) {
                _chapterRange.value = 1f..updatedNovel.chapters.size.toFloat()
            }
        }
    }

    fun updateChapterRange(range: ClosedFloatingPointRange<Float>) {
        _chapterRange.value = range
    }

    fun startBackgroundExport(novel: Novel, format: ExportFormat) {
        viewModelScope.launch {
            val start = _chapterRange.value.start.toInt()
            val end = _chapterRange.value.endInclusive.toInt()

            val metadata = JSONObject().apply {
                put("format", format.toString())
                put("crawlerName", novel.crawlerName)
                put("startIndex", start)
                put("endIndex", end)
            }.toString()

            val request = RequestEntity(
                id = "${novel.url}_export_${format}_${start}_${end}_${System.currentTimeMillis()}",
                type = RequestType.ARTIFACT,
                novelUrl = novel.url,
                name = "Export: ${novel.title} ($format) [$start-$end]",
                metadata = metadata,
                parentNovel = novel.url,
                url = null,
                dependsOn = null,
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

    // Selective Novel Request for a range of chapters
    fun fetchRange(novel: Novel) {
        viewModelScope.launch {
            val start = _chapterRange.value.start.toInt()
            val end = _chapterRange.value.endInclusive.toInt()
            
            val rangeChapters = novel.chapters.filter { it.index in start..end }
            
            val requestId = "${novel.url}_range_${start}_${end}"
            val metadata = JSONObject().apply {
                put("crawlerName", novel.crawlerName)
                put("startIndex", start)
                put("endIndex", end)
            }.toString()

            val request = RequestEntity(
                id = requestId,
                type = RequestType.RANGE_DOWNLOAD,
                novelUrl = novel.url,
                name = "Download: ${novel.title} ($start-$end)",
                metadata = metadata,
                parentNovel = novel.url,
                url = novel.url,
                completedAt = null,
                progressTotal = rangeChapters.size
            )

            requestDao.insertRequests(listOf(request))

            SchedulerService.startService(getApplication())
        }
    }

    // Download all chapters in the novel
    fun downloadAllChapters(novel: Novel) {
        viewModelScope.launch {
            if (novel.chapters.isEmpty()) return@launch
            
            val start = 1
            val end = novel.chapters.size
            
            val requestId = "${novel.url}_full_download"
            val metadata = JSONObject().apply {
                put("crawlerName", novel.crawlerName)
                put("startIndex", start)
                put("endIndex", end)
            }.toString()

            val request = RequestEntity(
                id = requestId,
                type = RequestType.RANGE_DOWNLOAD,
                novelUrl = novel.url,
                name = "Download All: ${novel.title}",
                metadata = metadata,
                parentNovel = novel.url,
                url = novel.url,
                completedAt = null,
                progressTotal = novel.chapters.size
            )

            requestDao.insertRequests(listOf(request))
            SchedulerService.startService(getApplication())
        }
    }

    // Download all chapters in a specific volume
    fun downloadVolume(novel: Novel, volumeId: String, volumeIndex: Int) {
        viewModelScope.launch {
            val volumeChapters = novel.chapters.filter { it.volumeId == volumeId }
            if (volumeChapters.isEmpty()) return@launch
            
            val start = volumeChapters.minOf { it.index }
            val end = volumeChapters.maxOf { it.index }
            
            val requestId = "${novel.url}_vol_${volumeIndex}_download"
            val metadata = JSONObject().apply {
                put("crawlerName", novel.crawlerName)
                put("startIndex", start)
                put("endIndex", end)
            }.toString()

            val request = RequestEntity(
                id = requestId,
                type = RequestType.RANGE_DOWNLOAD,
                novelUrl = novel.url,
                name = "Download: ${novel.title} Vol $volumeIndex",
                metadata = metadata,
                parentNovel = novel.url,
                url = novel.url,
                completedAt = null,
                progressTotal = volumeChapters.size
            )

            requestDao.insertRequests(listOf(request))
            SchedulerService.startService(getApplication())
        }
    }

    fun fetchChapter(novel: Novel, chapter: Chapter) {
        viewModelScope.launch {
            val chapterMetadata = JSONObject().apply {
                put("chapterId", chapter.id)
                put("crawlerName", novel.crawlerName)
            }.toString()

            val request = RequestEntity(
                id = "${novel.url}_ch_${chapter.index}",
                type = RequestType.CHAPTER,
                parentNovel = novel.url,
                dependsOn = null,
                priority = 10, // Higher priority for manual single chapter fetch
                name = "Chapter ${chapter.title} Download",
                completedAt = null,
                metadata = chapterMetadata,
                url = chapter.url,
                novelUrl = novel.url,
                progressTotal = 1,
                progressSuccess = 0,
            )

            requestDao.insertRequests(listOf(request))
            SchedulerService.startService(getApplication())
        }
    }

    fun copyArtifactToUri(artifact: Artifact, destinationUri: Uri, onComplete: (Uri?) -> Unit, onFileMissing: () -> Unit) {
        viewModelScope.launch {
            if (!artifactRepository.artifactExists(artifact)) {
                artifactRepository.removeArtifact(artifact)
                onFileMissing()
                return@launch
            }
            val result = artifactRepository.copyArtifactToUri(artifact, destinationUri)
            onComplete(result)
        }
    }

    fun deleteNovelPermanently(novel: Novel) {
        viewModelScope.launch {
            novelRepository.deleteNovel(novel)
        }
    }
}
