package com.halovoid.lncrawler.data.repository

import android.content.Context
import com.halovoid.lncrawler.data.db.AppDatabase
import com.halovoid.lncrawler.domain.models.Artifact
import com.halovoid.lncrawler.domain.models.toDomain
import com.halovoid.lncrawler.domain.models.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ArtifactRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)

    private val artifactDao = db.artifactDao()

    fun getArtifactById(id: Int): Artifact {
        return artifactDao.getArtifactById(id).toDomain()
    }

    fun getArtifactForRequest(id: String) : List<Artifact> {
        return artifactDao.getArtifactForRequest(id).map { it.toDomain() }
    }

    fun getArtifactsByNovelFlow(url: String): Flow<List<Artifact>> {
        return artifactDao.getArtifactsByNovelFlow(url).map { it.map { entity -> entity.toDomain() } }
    }

    fun insertArtifacts(artifact: Artifact) {
        artifactDao.insertArtifact(artifact.toEntity())
    }

    fun removeArtifact(artifact: Artifact) {
        artifactDao.removeArtifact(artifact.toEntity())
    }
}