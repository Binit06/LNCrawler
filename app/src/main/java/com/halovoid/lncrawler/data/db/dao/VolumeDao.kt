package com.halovoid.lncrawler.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.halovoid.lncrawler.data.db.entities.VolumeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VolumeDao {
    @Query("SELECT * FROM volumes WHERE novelUrl = :url")
    fun getVolumesForNovel(url: String) : List<VolumeEntity>

    @Query("SELECT COUNT(*) FROM volumes WHERE novelUrl = :novelUrl")
    fun getVolumeCountFlow(novelUrl: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVolumes(volumes: List<VolumeEntity>)
}