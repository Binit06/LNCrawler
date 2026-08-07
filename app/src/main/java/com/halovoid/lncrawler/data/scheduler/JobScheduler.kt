package com.halovoid.lncrawler.data.scheduler

import com.halovoid.lncrawler.data.config.SchedulerConfig
import com.halovoid.lncrawler.data.db.dao.RequestDao
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

/**
 * Main scheduler that manages the lifecycle of jobs.
 * Polls the database for runnable requests and manages concurrent execution.
 */
class JobScheduler(
    private val requestDao: RequestDao,
    private val handlerRegistry: JobHandlerRegistry,
    private val config: SchedulerConfig = SchedulerConfig(),
    private val retryPolicy: RetryPolicy = ExponentialBackoffPolicy(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private var pollingJob: Job? = null
    private val activeJobs = ConcurrentHashMap<String, Job>()

    /**
     * Starts the scheduler's polling loop.
     */
    fun start() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (isActive) {
                try {
                    schedule()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // In a real app, use a logger here
                    e.printStackTrace()
                }
                delay(config.pollingIntervalMs.milliseconds)
            }
        }
    }

    /**
     * Stops the scheduler. In-flight jobs will continue until finished or the scope is cancelled.
     */
    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * Main scheduling cycle: recovers abandoned jobs and launches new ones.
     */
    private suspend fun schedule() {
        // 1. Recover jobs that were marked as RUNNING but are no longer active
        recoverAbandoned()

        // 2. Fetch all requests to evaluate dependencies
        // DAO returns Flow, we take the current snapshot
        val allRequests = requestDao.getAllRequests().first()

        // 3. Filter and prioritize runnable requests
        val runnableRequests = allRequests.filter { it.isRunnable(allRequests) }
            .sortedWith(compareByDescending<RequestEntity> { it.priority }.thenBy { it.createdAt })

        // 4. Launch new runners for eligible requests
        for (request in runnableRequests) {
            if (activeJobs.size >= config.maxConcurrentJobs) break
            if (activeJobs.containsKey(request.id)) continue

            val job = scope.launch {
                try {
                    val runner = JobRunner(requestDao, handlerRegistry, retryPolicy, config)
                    runner.run(request) {
                        activeJobs.remove(request.id)
                    }
                } catch (e: Exception) {
                    activeJobs.remove(request.id)
                }
            }
            activeJobs[request.id] = job
        }
    }

    /**
     * Identifies jobs stuck in RUNNING state (e.g., due to process crash) and resets them to PENDING.
     */
    private suspend fun recoverAbandoned() {
        val allRequests = requestDao.getAllRequests().first()
        val now = System.currentTimeMillis()
        
        val abandoned = allRequests.filter { 
            it.status == RequestStatus.RUNNING && 
            (now - it.updatedAt) > config.abandonedTimeoutMs &&
            !activeJobs.containsKey(it.id)
        }

        for (request in abandoned) {
            requestDao.updateRequest(request.copy(
                status = RequestStatus.PENDING,
                updatedAt = now,
                error = "Recovered from abandoned state (stale for > ${config.abandonedTimeoutMs}ms)"
            ))
        }
    }

    /**
     * Checks if a request is eligible for execution.
     */
    private fun RequestEntity.isRunnable(allRequests: List<RequestEntity>): Boolean {
        // Only PENDING jobs can be started
        if (status != RequestStatus.PENDING) return false
        
        // Check if dependency is satisfied
        if (dependsOn != null) {
            val dependency = allRequests.find { it.id == dependsOn }
            // If dependency is not found or not successful, this job is not runnable
            if (dependency == null || dependency.status != RequestStatus.SUCCESS) {
                return false
            }
        }
        
        // Parent-child: Currently, we don't block root jobs based on children, 
        // but we could add logic here if children must finish first.
        
        return true
    }
}
