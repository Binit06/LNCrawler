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
 */
class RequestDetailViewModel(
    application: Application,
    private val exportRecordDao: ExportRecordDao
) : AndroidViewModel(application) {
    private val repository = NovelRepository(application)
    private val workManager = WorkManager.getInstance(application)

    private val _novel = MutableStateFlow<Novel?>(null)
    val novel: StateFlow<Novel?> = _novel.asStateFlow()

    /** Observes the specific record from the database as a hot flow. */
    fun getRecord(recordId: Int): Flow<ExportRecord?> {
        return exportRecordDao.getRecordById(recordId.toLong())
            .map { it?.toDomain() }
            .onEach { domainRecord ->
                if (domainRecord != null && _novel.value == null) {
                    _novel.value = repository.getNovelDetails(
                        domainRecord.crawlerName, 
                        domainRecord.novelUrl, 
                        refresh = false
                    )
                }
            }
    }

    /** Observes the global export progress map keyed by Record ID. */
    val exportProgressMap: StateFlow<Map<Long, ExportProgress>> = ExportProgressManager.recordProgressMap

    fun deleteHistoryRecord(id: Int, novelUrl: String) {
        viewModelScope.launch {
            exportRecordDao.deleteById(id)
            ExportProgressManager.updateProgress(id.toLong(), novelUrl, null)
        }
    }
    
    fun rerequestExport(destinationUri: android.net.Uri, record: ExportRecord) {
        val inputData = Data.Builder()
            .putString("novelUrl", record.novelUrl)
            .putString("crawlerName", record.crawlerName)
            .putString("destinationUri", destinationUri.toString())
            .build()
            
        val request = OneTimeWorkRequestBuilder<ExportWorker>()
            .setInputData(inputData)
            .addTag(record.novelUrl)
            .build()
            
        workManager.enqueueUniqueWork(
            record.novelUrl,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelExport(record: ExportRecord) {
        workManager.cancelUniqueWork(record.novelUrl)
        ExportProgressManager.updateProgress(record.id.toLong(), record.novelUrl, null)
    }
}
