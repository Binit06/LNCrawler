package com.halovoid.lncrawler.data.scheduler

import com.halovoid.lncrawler.data.db.entities.RequestType
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry to map job types to their corresponding [JobHandler].
 */
class JobHandlerRegistry {
    private val handlers = ConcurrentHashMap<RequestType, JobHandler>()

    /**
     * Registers a handler for a specific job type.
     * @param type The type identifier (e.g., crawler name or request type).
     * @param handler The handler implementation.
     */
    fun register(type: RequestType, handler: JobHandler) {
        handlers[type] = handler
    }

    /**
     * Retrieves the handler for a specific job type.
     * @param type The type identifier.
     * @return The handler, or null if not found.
     */
    fun getHandler(type: RequestType): JobHandler? = handlers[type]
}
