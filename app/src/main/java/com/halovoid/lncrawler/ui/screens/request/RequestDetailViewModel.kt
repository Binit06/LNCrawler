package com.halovoid.lncrawler.ui.screens.request

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.data.db.dao.RequestDao
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for the Request Detail screen.
 */
class RequestDetailViewModel(
    application: Application,
    private val requestDao: RequestDao
) : AndroidViewModel(application) {
    private val chapterRepository = ChapterRepository(application)
    private val artifactRepository = ArtifactRepository(application)

    private val _requestId = MutableStateFlow<String?>(null)
    fun setRequestId(id: String) {
        _requestId.value = id
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val linkedRequests: StateFlow<List<Request>> = _requestId
        .filterNotNull()
        .flatMapLatest { id ->
            requestDao.getRequestsByDependenceFlow(id)
                .map { entities -> entities.map { it.toDomain() } }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val chapterMetadata: StateFlow<Chapter?> = _requestId
        .filterNotNull()
        .flatMapLatest { id ->
            requestDao.getRequestByIdFlow(id)
        }
        .filterNotNull()
        .map { request ->
            // Move this to the IO Thread from the Main Thread
            val chapters = chapterRepository.getChaptersByNovelUrl(request.novelUrl)
            chapters.find { it.url == request.url }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val artifactMetadata: StateFlow<Artifact?> = _requestId
        .filterNotNull()
        .flatMapLatest { id ->
            requestDao.getRequestByIdFlow(id)
        }
        .filterNotNull()
        .map { request ->
            val artifacts = artifactRepository.getArtifactForRequest(request.id)
            artifacts.find { it.requestId == request.id }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    fun getRequest(requestId: String): Flow<Request?> {
        return requestDao.getRequestByIdFlow(requestId)
            .map { it?.toDomain() }
    }

    fun replayRequest(requestId: String) {
        viewModelScope.launch {
            requestDao.replayRequest(requestId)

            SchedulerService.startService(getApplication())
        }
    }

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            requestDao.cancelRequest(requestId)

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

                application.contentResolver
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
}
