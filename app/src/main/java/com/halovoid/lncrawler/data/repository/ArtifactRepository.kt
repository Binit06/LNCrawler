package com.halovoid.lncrawler.data.repository

import android.content.Context
import android.net.Uri
import com.halovoid.lncrawler.data.db.AppDatabase
import com.halovoid.lncrawler.domain.models.Artifact
import com.halovoid.lncrawler.domain.models.toDomain
import com.halovoid.lncrawler.domain.models.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

class ArtifactRepository(private val context: Context) {

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

    suspend fun artifactExists(artifact: Artifact): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Try to open the input stream to verify the file is actually there and readable
                context.contentResolver.openInputStream(artifact.artifactDestination.toUri())?.use {
                    true
                } ?: false
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun copyArtifactToUri(artifact: Artifact, destinationUri: Uri): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val sourceUri = artifact.artifactDestination.toUri()

                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                destinationUri
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun insertArtifacts(artifact: Artifact) {
        withContext(Dispatchers.IO) {
            artifactDao.insertArtifact(artifact.toEntity())
        }
    }

    suspend fun removeArtifact(artifact: Artifact) {
        withContext(Dispatchers.IO) {
            artifactDao.removeArtifact(artifact.toEntity())
        }
    }
}