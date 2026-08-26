package com.halovoid.lncrawler.data.repository

import android.content.Context
import com.halovoid.lncrawler.data.db.AppDatabase
import com.halovoid.lncrawler.data.db.mappers.toDomain
import com.halovoid.lncrawler.data.db.mappers.toEntity
import com.halovoid.lncrawler.domain.models.Volume
import com.halovoid.lncrawler.domain.models.toDomain
import com.halovoid.lncrawler.domain.models.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VolumeRepository private constructor(context: Context) {
    private val db = AppDatabase.getDatabase(context)

    private val volumeDao = db.volumeDao()

    companion object {
        @Volatile
        private var INSTANCE: VolumeRepository? = null

        fun getInstance(context: Context): VolumeRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VolumeRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun getVolumeByNovelUrl(url: String): List<Volume> {
        return volumeDao.getVolumesForNovel(url).map { it -> it.toDomain() }
    }

    fun getVolumeByNovelUrlFlow(url: String): Flow<List<Volume>> {
        return volumeDao.getVolumesForNovelFlow(url).map { list -> list.map { it.toDomain() } }
    }

    fun getVolumeCount(novelUrl: String): Flow<Int> =
        volumeDao.getVolumeCountFlow(novelUrl)

    fun insertVolumes(volumes: List<Volume>) {
        volumeDao.insertVolumes(volumes.map { it -> it.toEntity() })
    }
}