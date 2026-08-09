package com.halovoid.lncrawler.data.repository

import android.content.Context
import com.halovoid.lncrawler.data.db.AppDatabase
import com.halovoid.lncrawler.domain.models.Volume
import com.halovoid.lncrawler.domain.models.toDomain
import com.halovoid.lncrawler.domain.models.toEntity
import kotlinx.coroutines.flow.Flow

class VolumeRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)

    private val volumeDao = db.volumeDao()

    fun getVolumeByNovelUrl(url: String): List<Volume> {
        return volumeDao.getVolumesForNovel(url).map { it -> it.toDomain() }
    }

    fun getVolumeCount(novelUrl: String): Flow<Int> =
        volumeDao.getVolumeCountFlow(novelUrl)

    fun insertVolumes(volumes: List<Volume>) {
        volumeDao.insertVolumes(volumes.map { it -> it.toEntity() })
    }
}