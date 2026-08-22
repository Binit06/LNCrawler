package com.halovoid.lncrawler.data.scheduler.jobs

import com.halovoid.lncrawler.data.config.SchedulerConfig
import com.halovoid.lncrawler.data.db.dao.RequestDao
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
    private val workerPool = WorkerPool(config.maxConcurrentJobs)
    private val leaseMonitor = LeaseMonitor(config.abandonedTimeoutMs)
    private var onEmptyListener: (() -> Unit)? = null

    fun setOnEmptyListener(listener: () -> Unit) {
        this.onEmptyListener = listener
    }

    fun start() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (isActive) {
                try {
                    schedule()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(config.pollingIntervalMs.milliseconds)
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun cancelActiveJob(requestId: String) {
        activeJobs[requestId]?.cancel()
        scope.launch {
            val graph = JobGraph.build(requestDao.getAllRequests().first())
            graph.subtreeOf(requestId).forEach { descendant ->
                if (descendant.id != requestId) activeJobs[descendant.id]?.cancel()
            }
        }
    }

    private suspend fun schedule() {
        val graph = JobGraph.build(requestDao.getAllRequests().first())
        recoverAbandoned(graph)
        cancelChildrenOfCancelledParents(graph)

        val readyQueue = ReadyQueue()
        readyQueue.pushAll(graph.runnableJobs())
        if (activeJobs.isEmpty() && readyQueue.isEmpty()) {
            onEmptyListener?.invoke()
            return
        }
        launchReadyJobs(readyQueue)
    }

    private fun launchReadyJobs(readyQueue: ReadyQueue) {
        while (true) {
            val request = readyQueue.pop() ?: break
            if (activeJobs.containsKey(request.id)) continue

            if (!workerPool.tryAcquire()) break

            val job = scope.launch {
                try {
                    val runner = JobRunner(requestDao, handlerRegistry, retryPolicy, config)
                    runner.run(request) {
                        activeJobs.remove(request.id)
                    }
                } catch (e: Exception) {
                    activeJobs.remove(request.id)
                } finally {
                    workerPool.release()
                }
            }
            activeJobs[request.id] = job
        }
    }

    private suspend fun recoverAbandoned(graph: JobGraph) {
        val now = System.currentTimeMillis()
        val abandoned = graph.runningJobs().filter {
            leaseMonitor.isExpired(it, now) && !activeJobs.containsKey(it.id)
        }

        for (request in abandoned) {
            val parentCancelled = graph.parentOf(request)?.status == RequestStatus.CANCELLED
            if (parentCancelled) {
                requestDao.cancelRequest(request.id)
            } else {
                requestDao.resetStatus(request.id, RequestStatus.PENDING, RequestStatus.RUNNING)
            }
        }
    }

    private fun cancelChildrenOfCancelledParents(graph: JobGraph) {
        graph.allNodes()
            .filter { it.status == RequestStatus.PENDING && it.dependsOn != null }
            .forEach { req ->
                val parentCancelled = graph.parentOf(req)?.status == RequestStatus.CANCELLED
                if (parentCancelled) {
                    scope.launch { requestDao.cancelRequest(req.id) }
                }
            }
    }


}

internal class WorkerPool(maxConcurrent: Int) {
    private val semaphore = kotlinx.coroutines.sync.Semaphore(maxConcurrent)
    fun tryAcquire(): Boolean = semaphore.tryAcquire()
    fun release() = semaphore.release()
}

internal class LeaseMonitor(private val leaseDurationMs: Long) {
    fun isExpired(request: com.halovoid.lncrawler.data.db.entities.RequestEntity, now: Long = System.currentTimeMillis()): Boolean =
        request.status == com.halovoid.lncrawler.data.db.entities.RequestStatus.RUNNING &&
                (now - request.updatedAt) > leaseDurationMs
}

internal class ReadyQueue(
    comparator: Comparator<com.halovoid.lncrawler.data.db.entities.RequestEntity> =
        compareByDescending<com.halovoid.lncrawler.data.db.entities.RequestEntity> { it.priority }
            .thenBy { it.createdAt }
) {
    private val heap = java.util.PriorityQueue(comparator)
    fun pushAll(jobs: Collection<com.halovoid.lncrawler.data.db.entities.RequestEntity>) { heap.addAll(jobs) }
    fun pop(): com.halovoid.lncrawler.data.db.entities.RequestEntity? = heap.poll()
    fun isEmpty() = heap.isEmpty()
}

internal class JobGraph private constructor(
    private val nodes: Map<String, com.halovoid.lncrawler.data.db.entities.RequestEntity>,
    private val childrenOf: Map<String, List<String>>
) {
    fun get(id: String): com.halovoid.lncrawler.data.db.entities.RequestEntity? = nodes[id]
    fun allNodes(): Collection<com.halovoid.lncrawler.data.db.entities.RequestEntity> = nodes.values
    fun childrenOf(id: String): List<com.halovoid.lncrawler.data.db.entities.RequestEntity> =
        childrenOf[id]?.mapNotNull { nodes[it] } ?: emptyList()

    fun parentOf(entity: com.halovoid.lncrawler.data.db.entities.RequestEntity): com.halovoid.lncrawler.data.db.entities.RequestEntity? =
        entity.dependsOn?.let { nodes[it] }

    fun subtreeOf(rootId: String): List<com.halovoid.lncrawler.data.db.entities.RequestEntity> {
        val visited = LinkedHashSet<String>()
        val stack = ArrayDeque<String>()
        stack.addLast(rootId)
        while (stack.isNotEmpty()) {
            val currId = stack.removeLast()
            if (!visited.add(currId)) continue
            childrenOf[currId]?.forEach { stack.addLast(it) }
        }
        return visited.mapNotNull { nodes[it] }
    }

    fun runnableJobs(): List<com.halovoid.lncrawler.data.db.entities.RequestEntity> =
        nodes.values.filter { it.isRunnable() }

    fun runningJobs(): List<com.halovoid.lncrawler.data.db.entities.RequestEntity> =
        nodes.values.filter { it.status == com.halovoid.lncrawler.data.db.entities.RequestStatus.RUNNING }

    private fun com.halovoid.lncrawler.data.db.entities.RequestEntity.isRunnable(): Boolean {
        if (status != com.halovoid.lncrawler.data.db.entities.RequestStatus.PENDING) return false
        if (dependsOn != null) {
            val dependency = nodes[dependsOn] ?: return false
            if (dependency.status == com.halovoid.lncrawler.data.db.entities.RequestStatus.CANCELLED) return false
            if (dependency.status != com.halovoid.lncrawler.data.db.entities.RequestStatus.SUCCESS) return false
        }
        return true
    }

    companion object {
        fun build(allRequests: List<com.halovoid.lncrawler.data.db.entities.RequestEntity>): JobGraph {
            val byId = allRequests.associateBy { it.id }
            val children = allRequests
                .filter { it.dependsOn != null }
                .groupBy { it.dependsOn!! }
                .mapValues { (_, kids) -> kids.map { it.id } }
            return JobGraph(byId, children)
        }
    }
}
