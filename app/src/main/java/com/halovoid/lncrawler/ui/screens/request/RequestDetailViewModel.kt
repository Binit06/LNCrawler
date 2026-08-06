package com.halovoid.lncrawler.ui.screens.request

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.halovoid.lncrawler.data.db.dao.ExportRecordDao
import com.halovoid.lncrawler.data.export.ExportProgress
import com.halovoid.lncrawler.data.export.ExportProgressManager
import com.halovoid.lncrawler.data.export.ExportWorker
import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.domain.models.ExportRecord
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.domain.models.toDomain
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Request Detail screen.
 * Fetches specific export record details and associated novel metadata.
 */
class RequestDetailViewModel(
    application: Application,
    private val exportRecordDao: ExportRecordDao
) : AndroidViewModel(application) {
    private val repository = NovelRepository(application)
    private val workManager = WorkManager.getInstance(application)

    private val _record = MutableStateFlow<ExportRecord?>(null)
    val record: StateFlow<ExportRecord?> = _record.asStateFlow()

    private val _novel = MutableStateFlow<Novel?>(null)
    val novel: StateFlow<Novel?> = _novel.asStateFlow()

    /** Observes the global export progress map keyed by Record ID. */
    val exportProgressMap: StateFlow<Map<Long, ExportProgress>> = ExportProgressManager.recordProgressMap

    fun loadRecord(recordId: Int) {
        viewModelScope.launch {
            val recordEntity = exportRecordDao.getRecordById(recordId.toLong())
            val domainRecord = recordEntity?.toDomain()
            _record.value = domainRecord
            
            domainRecord?.let {
                // LOAD FROM CACHE ONLY during initial screen transition to avoid UI lag
                _novel.value = repository.getNovelDetails(it.crawlerName, it.novelUrl, refresh = false)
            }
        }
    }

    fun deleteHistoryRecord(id: Int, novelUrl: String) {
        viewModelScope.launch {
            exportRecordDao.deleteById(id)
            ExportProgressManager.updateProgress(id.toLong(), novelUrl, null)
        }
    }
    
    fun rerequestExport(destinationUri: android.net.Uri) {
        val currentRecord = _record.value ?: return
        val inputData = Data.Builder()
            .putString("novelUrl", currentRecord.novelUrl)
            .putString("crawlerName", currentRecord.crawlerName)
            .putString("destinationUri", destinationUri.toString())
            .build()
            
        val request = OneTimeWorkRequestBuilder<ExportWorker>()
            .setInputData(inputData)
            .addTag(currentRecord.novelUrl)
            .build()
            
        workManager.enqueueUniqueWork(
            currentRecord.novelUrl,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelExport() {
        _record.value?.let {
            workManager.cancelUniqueWork(it.novelUrl)
            ExportProgressManager.updateProgress(it.id.toLong(), it.novelUrl, null)
        }
    }
}
