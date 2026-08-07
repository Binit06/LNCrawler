package com.halovoid.lncrawler.data.scheduler

import com.halovoid.lncrawler.data.db.entities.RequestEntity

/**
 * Interface for executing a specific type of request.
 */
interface JobHandler {
    /**
     * Executes the given request.
     * @param request The request entity to process.
     * @return The result of the execution.
     */
    suspend fun handle(request: RequestEntity): JobResult
}
