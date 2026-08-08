package com.halovoid.lncrawler.domain.models

import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.data.db.entities.RequestType

enum class RequestStatus {
    PENDING, SUCCESS, FAILED, CANCELLED
}

data class Request(
    val id: String,
    val name: String,
    val parentNovel: String?,
    val dependsOn: String? = null,
    val url: String?,
    val novelUrl: String,
    val priority: Int = 0,
    val type: RequestType,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long?,
    val progressTotal: Int,
    val progressSuccess: Int = 0,
    val progressFailed: Int = 0,
    val progressCancelled: Int = 0,
    val status: RequestStatus = RequestStatus.PENDING,
    val metadata: String? = null,
    val error: String? = null
)

fun RequestEntity.toDomain(): Request = Request(
    id = id,
    name = name,
    parentNovel = parentNovel,
    dependsOn = dependsOn,
    url = url,
    novelUrl = novelUrl,
    priority = priority,
    type = type,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
    progressTotal = progressTotal,
    progressSuccess = progressSuccess,
    progressFailed = progressFailed,
    progressCancelled = progressCancelled,
    status = status,
    metadata = metadata,
    error = error
)
fun Request.toEntity(): RequestEntity = RequestEntity(
    id = id,
    name = name,
    parentNovel = parentNovel,
    dependsOn = dependsOn,
    url = url,
    novelUrl = novelUrl,
    priority = priority,
    type = type,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
    progressTotal = progressTotal,
    progressSuccess = progressSuccess,
    progressFailed = progressFailed,
    progressCancelled = progressCancelled,
    status = status,
    metadata = metadata,
    error = error
)
