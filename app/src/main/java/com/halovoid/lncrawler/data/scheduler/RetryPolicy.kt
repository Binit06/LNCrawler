package com.halovoid.lncrawler.data.scheduler

import kotlin.math.pow

/**
 * Strategy for calculating delays between retries.
 */
interface RetryPolicy {
    /**
     * Calculates the delay in milliseconds before the next retry.
     * @param retryCount The number of times the job has already been tried.
     * @return Delay in milliseconds.
     */
    fun getNextDelay(retryCount: Int): Long
}

/**
 * Exponential backoff strategy.
 */
class ExponentialBackoffPolicy(
    private val initialDelay: Long = 1000,
    private val factor: Double = 2.0,
    private val maxDelay: Long = 60000
) : RetryPolicy {
    override fun getNextDelay(retryCount: Int): Long {
        return (initialDelay * factor.pow(retryCount.toDouble())).toLong().coerceAtMost(maxDelay)
    }
}
