package com.halovoid.lncrawler.domain.models

import com.halovoid.lncrawler.data.db.entities.ArtifactRequestEntity
import com.halovoid.lncrawler.data.db.entities.ArtifactStatus


/**
 * Domain model for an export history record.
 * Supports parent-child linking to track related requests.
 */
data class ArtifactRequest(
    val id: Int = 0,
    val requestId: Int? = null,
    val status: ArtifactStatus,
    val destinationUri: String?,
    val timestamp: Long
)

/**
 * Extension to convert database entity to domain model.
 */
fun ArtifactRequestEntity.toDomain() : ArtifactRequest = ArtifactRequest(
    id = id,
    requestId = requestId,
    status = status,
    destinationUri = destinationUri,
    timestamp = timestamp
)
fun ArtifactRequest.toEntity(): ArtifactRequestEntity = ArtifactRequestEntity(
    id = id,
    requestId = requestId,
    status = status,
    destinationUri = destinationUri,
    timestamp = timestamp
)