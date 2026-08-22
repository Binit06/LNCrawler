package com.halovoid.lncrawler.data.redis

import android.util.Log
import com.halovoid.lncrawler.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.net.URI
import kotlin.time.Duration.Companion.milliseconds

/**
 * Redis is used to push the URL's user enter for indexing it later on
 * The index is used for enabling fast searches for novels
 * This is planned as an optional feature that can be disabled
 */
object RedisManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var jedisPool: JedisPool? = null

    fun initialize() {
        scope.launch {
            try {
                val url = BuildConfig.REDIS_URL
                if (url.isBlank()) {
                    Log.i("RedisManager", "Redis URL is empty. Redis integration disabled.")
                    return@launch
                }

                Log.i("RedisManager", "Initializing Redis Pool...")
                val poolConfig = JedisPoolConfig().apply {
                    maxTotal = 3 // Reduced for mobile
                    maxIdle = 2
                    minIdle = 1
                    // Disable aggressive testing to prevent lagginess
                    testOnBorrow = false
                    testWhileIdle = false
                    blockWhenExhausted = true
                    maxWaitMillis = 2000
                    jmxEnabled = false
                }

                // Explicitly use the URI constructor which handles rediss:// for TLS
                val uri = URI(url)
                
                // Use a 5-second connection timeout to avoid hanging the background thread
                jedisPool = JedisPool(poolConfig, uri, 5000)

                // Silent ping in background
                launch {
                    try {
                        withTimeout(3000.milliseconds) {
                            jedisPool?.resource?.use { it.ping() }
                        }
                        Log.i("RedisManager", "Redis connection established successfully.")
                    } catch (e: Exception) {
                        Log.w("RedisManager", "Redis ping failed: ${e.message}. Will retry on next use.")
                    }
                }
            } catch (e: Exception) {
                Log.e("RedisManager", "Failed to initialize Redis Pool: ${e.message}")
                jedisPool = null
            }
        }
    }

    fun pushUrl(url: String) {
        if (BuildConfig.REDIS_URL.isBlank()) return

        scope.launch {
            try {
                jedisPool?.resource?.use { jedis ->
                    jedis.rpush("index_requests", url)
                    Log.i("RedisManager", "URL pushed to Redis: $url")
                } ?: Log.w("RedisManager", "Cannot push URL: Redis Pool not initialized")
            } catch (e: Exception) {
                Log.e("RedisManager", "Error pushing URL to Redis: ${e.message}")
            }
        }
    }
}
