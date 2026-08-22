package com.halovoid.lncrawler.data.scheduler.jobs

import com.halovoid.lncrawler.data.config.SchedulerConfig
import com.halovoid.lncrawler.data.db.dao.RequestDao
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class JobRunner(
    private val requestDao: RequestDao,
    private val handlerRegistry: JobHandlerRegistry,
    private val retryPolicy: RetryPolicy,
    private val config: SchedulerConfig
) {
    suspend fun run(request: RequestEntity, onComplete: suspend () -> Unit) {
        var currRequest = request
        try {
            val preClaimStatus = requestDao.getRequestById(currRequest.id)?.status
            if (preClaimStatus == RequestStatus.CANCELLED) return

            currRequest = applyEvent(currRequest, JobEvent.CLAIMED)
            requestDao.updateRequest(currRequest)

            val handler = handlerRegistry.getHandler(currRequest.type)
            if (handler == null) {
                fail(currRequest, "No handler found for ${currRequest.type}")
                return
            }

            val result = handler.handle(currRequest)

            when (result) {
                is JobResult.Success -> {
                    markSuccess(currRequest)
                    return
                }
                is JobResult.Cancelled -> {
                    markCancelled(currRequest)
                    return
                }
                is JobResult.Blocked -> {
                    markBlocked(currRequest)
                    return
                }
                is JobResult.Failure -> {
                    fail(currRequest, result.error.message ?: "Execution Failed")
                    return
                }
            }
        } catch (e: Exception) {
            fail(request, e.message ?: "Unexpected error during execution")
        } finally {
            onComplete()
        }
    }

    private fun applyEvent(request: RequestEntity, event: JobEvent): RequestEntity =
        request.copy(
            status = JobStateMachine.transition(request.status, event),
            updatedAt = System.currentTimeMillis()
        )

    private suspend fun markSuccess(request: RequestEntity) {
        val hasChildren = requestDao.hasChildren(request.id)
        val updated = applyEvent(request, JobEvent.HANDLER_SUCCESS).copy(
            rstatus = if (!hasChildren) RequestStatus.SUCCESS else request.rstatus,
            completedAt = System.currentTimeMillis(),
            progressSuccess = if (hasChildren) request.progressSuccess else request.progressTotal,
            error = null
        )

        requestDao.updateRequest(updated)
        requestDao.propagateProgress(request.id)
    }

    private suspend fun fail(request: RequestEntity, errorMessage: String) {
        val hasChildren = requestDao.hasChildren(request.id)
        val updated = applyEvent(request, JobEvent.HANDLER_FAILURE_FINAL).copy(
            rstatus = if (!hasChildren) RequestStatus.FAILED else request.status,
            progressFailed = if (hasChildren) request.progressFailed else request.progressTotal,
            error = errorMessage
        )

        requestDao.updateRequest(updated)
        requestDao.propagateProgress(request.id)
    }

    private suspend fun markCancelled(request: RequestEntity) {
        val hasChildren = requestDao.hasChildren(request.id)
        val updated = applyEvent(request, JobEvent.CANCEL_REQUESTED).copy(
            rstatus = if (hasChildren) RequestStatus.CANCELLING else RequestStatus.CANCELLED,
            progressCancelled = if (hasChildren) request.progressCancelled else request.progressTotal
        )
        requestDao.updateRequest(updated)
        requestDao.propagateProgress(request.id)
    }

    private suspend fun markBlocked(request: RequestEntity) {
        val updated = applyEvent(request, JobEvent.BLOCKED_BY_PROTECTION)
            .copy(error = "Security check required")

        requestDao.updateRequest(updated)
    }
}