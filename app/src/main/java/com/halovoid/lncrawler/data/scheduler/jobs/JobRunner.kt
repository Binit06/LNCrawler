package com.halovoid.lncrawler.data.scheduler.jobs

import com.halovoid.lncrawler.data.config.SchedulerConfig
import com.halovoid.lncrawler.data.db.dao.RequestDao
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.data.db.entities.RequestType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Responsible for executing a single [RequestEntity].
 * Handles claiming, execution via [JobHandler], and retry logic.
 */
class JobRunner(
    private val requestDao: RequestDao,
    private val handlerRegistry: JobHandlerRegistry,
    private val retryPolicy: RetryPolicy,
    private val config: SchedulerConfig
) {
    private val isRunning = AtomicBoolean(false)

    /**
     * Runs the job.
     * @param request The request to execute.
     * @param onComplete Callback invoked when the runner is finished (success, failure, or cancellation).
     */
    suspend fun run(request: RequestEntity, onComplete: suspend () -> Unit) {
        if (!isRunning.compareAndSet(false, true)) return

        try {
            var currentRequest = request
            
            // 1. Claim the request
            // Note: In a multi-worker environment, this should ideally be an atomic DB operation.
            currentRequest = currentRequest.copy(
                status = RequestStatus.RUNNING,
                updatedAt = System.currentTimeMillis()
            )
            requestDao.updateRequest(currentRequest)

            // Using crawlerName as the handler key since RequestType is not a field in RequestEntity
            val handler = handlerRegistry.getHandler(currentRequest.type)
            if (handler == null) {
                fail(currentRequest, "No handler found for: ${currentRequest.type}")
                return
            }

            var retryCount = 0
            var success = false

            while (retryCount <= config.maxRetries && !success) {
                var result = handler.handle(currentRequest)
                val latestRequest = requestDao.getRequestById(currentRequest.id) ?: currentRequest
                when (result) {
                    is JobResult.Success -> {
                        markSuccess(latestRequest)
                        success = true
                    }
                    is JobResult.Cancelled -> {
                        markCancelled(latestRequest)
                        return
                    }
                    is JobResult.Blocked -> {
                        markBlocked(latestRequest)
                        return
                    }
                    is JobResult.Failure -> {
                        if (result.isRecoverable && retryCount < config.maxRetries) {
                            retryCount++
                            val delayMs = retryPolicy.getNextDelay(retryCount)
                            
                            // Update request with current error but keep RUNNING
                            currentRequest = currentRequest.copy(
                                error = "Retry $retryCount/${config.maxRetries}: ${result.error.message}",
                                updatedAt = System.currentTimeMillis()
                            )
                            requestDao.updateRequest(currentRequest)
                            
                            delay(delayMs)
                        } else {
                            fail(currentRequest, result.error.message ?: "Execution failed")
                            return
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            // If a request is canceled first update on the DB
            markCancelled(request)
            throw e
        } catch (e: Exception) {
            fail(request, e.message ?: "Unexpected error during execution")
        } finally {
            isRunning.set(false)
            onComplete()
        }
    }

    private suspend fun markSuccess(request: RequestEntity) {
        val hasChildren = requestDao.hasChildren(request.id)
        requestDao.updateRequest(request.copy(
            status = RequestStatus.SUCCESS,
            completedAt = System.currentTimeMillis(),
            progressSuccess = if (hasChildren) request.progressSuccess else request.progressTotal,
            updatedAt = System.currentTimeMillis(),
            error = null
        ))
        // Request Progress Update Propagation - SUCCESS
        requestDao.propagateProgress(request.id)
    }

    private suspend fun fail(request: RequestEntity, errorMessage: String) {
        val hasChildren = requestDao.hasChildren(request.id)
        requestDao.updateRequest(request.copy(
            status = RequestStatus.FAILED,
            updatedAt = System.currentTimeMillis(),
            progressFailed = if (hasChildren) request.progressFailed else request.progressTotal,
            error = errorMessage
        ))
        // Request Progress Update Propagation - FAILED
        requestDao.propagateProgress(request.id)
    }

    private suspend fun markCancelled(request: RequestEntity) {
        val hasChildren = requestDao.hasChildren(request.id)
        requestDao.updateRequest(request.copy(
            status = RequestStatus.CANCELLED,
            updatedAt = System.currentTimeMillis(),
            progressCancelled = if (hasChildren) request.progressCancelled else request.progressTotal,
        ))
        // Request Progress Update Propagation - CANCELLED
        requestDao.propagateProgress(request.id)
    }

    private suspend fun markBlocked(request: RequestEntity) {
        requestDao.updateRequest(request.copy(
            status = RequestStatus.BLOCKED,
            updatedAt = System.currentTimeMillis(),
            error = "Security check required (Cloudflare)"
        ))
        // We don't propagate progress yet as it's not a final state (it's a wait state)
    }
}
