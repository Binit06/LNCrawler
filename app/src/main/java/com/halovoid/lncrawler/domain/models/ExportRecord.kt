package com.halovoid.lncrawler.domain.models

/**
 * Represents the current status of an export request.
 */
enum class ExportStatus {
    PENDING, SUCCESS, FAILED, CANCELLED
}

/**
 * Domain model for an export history record.
 */
data class ExportRecord(
    val id: Int,
    val novelUrl: String,
    val novelTitle: String,
    val status: ExportStatus,
    val destinationUri: String?,
    val errorLog: String?,
    val crawlerName: String,
    val timestamp: Long
)

/**
 * Extension to convert database entity to domain model.
 */
fun com.halovoid.lncrawler.data.db.entities.ExportRecordEntity.toDomain() = ExportRecord(
    id = id,
    novelUrl = novelUrl,
    novelTitle = novelTitle,
    status = try { ExportStatus.valueOf(status) } catch (e: Exception) { ExportStatus.FAILED },
    destinationUri = destinationUri,
    errorLog = errorLog,
    crawlerName = crawlerName,
    timestamp = timestamp
)
