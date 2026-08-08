package com.halovoid.lncrawler.data.repository

import android.content.Context
import com.halovoid.lncrawler.data.db.AppDatabase
import com.halovoid.lncrawler.domain.models.Artifact
import com.halovoid.lncrawler.domain.models.toDomain
import com.halovoid.lncrawler.domain.models.toEntity

class ArtifactRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)

    private val artifactDao = db.artifactDao()

    fun getArtifactForRequest(id: String) : Artifact {
        return artifactDao.getArtifactForRequest(id).toDomain()
    }

    fun insertArtifacts(artifact: Artifact) {
        artifactDao.insertArtifact(artifact.toEntity())
    }

    fun removeArtifact(artifact: Artifact) {
        artifactDao.removeArtifact(artifact.toEntity())
    }
}