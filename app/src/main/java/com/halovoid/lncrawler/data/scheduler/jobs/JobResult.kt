package com.halovoid.lncrawler.data.scheduler.jobs

/**
 * Represents the result of a job execution.
 */
sealed class JobResult {
    /**
     * Job completed successfully.
     */
    object Success : JobResult()

    /**
     * Job failed.
     * @param error The exception that caused the failure.
     * @param isRecoverable Whether the job can be retried.
     */
    data class Failure(
        val error: Throwable,
        val isRecoverable: Boolean = true
    ) : JobResult()

    /**
     * Job was cancelled.
     */
    object Cancelled : JobResult()

    /**
     * Job is blocked by external protection (e.g. Cloudflare)
     */
    object Blocked : JobResult()
}
