package com.halovoid.lncrawler.data.scheduler.jobs

import com.halovoid.lncrawler.data.config.SchedulerConfig
import com.halovoid.lncrawler.data.db.dao.RequestDao
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import kotlinx.coroutines.delay
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
                when (val result = handler.handle(currentRequest)) {
                    is JobResult.Success -> {
                        markSuccess(currentRequest)
                        success = true
                    }
                    is JobResult.Cancelled -> {
                        markCancelled(currentRequest)
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
        } catch (e: Exception) {
            fail(request, e.message ?: "Unexpected error during execution")
        } finally {
            isRunning.set(false)
            onComplete()
        }
    }

    private suspend fun markSuccess(request: RequestEntity) {
        requestDao.updateRequest(request.copy(
            status = RequestStatus.SUCCESS,
            completedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            error = null
        ))
    }

    private suspend fun fail(request: RequestEntity, errorMessage: String) {
        requestDao.updateRequest(request.copy(
            status = RequestStatus.FAILED,
            updatedAt = System.currentTimeMillis(),
            error = errorMessage
        ))
    }

    private suspend fun markCancelled(request: RequestEntity) {
        requestDao.updateRequest(request.copy(
            status = RequestStatus.CANCELLED,
            updatedAt = System.currentTimeMillis()
        ))
    }
}
