package com.halovoid.lncrawler.ui.cloudflare

import android.content.Context
import android.content.Intent
import android.util.Log
import com.halovoid.lncrawler.api.core.scrapper.CloudflareResolver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.edit

/**
 * Implementation of CloudflareResolver that launches a WebView activity
 * to solve challenges.
 */
class CloudflareResolverImpl(private val context: Context) : CloudflareResolver {

    private val sharedPrefs = context.getSharedPreferences("cloudflare_prefs", Context.MODE_PRIVATE)
    
    // We use a singleton-like pattern to communicate between the Resolver and the Activity
    companion object {
        private var currentDeferred: CompletableDeferred<Boolean>? = null

        /**
         * Called by the CloudflareActivity when the challenge is solved or failed.
         */
        fun onResolutionResult(success: Boolean) {
            currentDeferred?.complete(success)
            currentDeferred = null
        }
    }

    override suspend fun resolve(url: String): Boolean = withContext(Dispatchers.Main) {
        Log.i("CloudflareResolver", "Launching resolution for $url")
        
        // If a resolution is already in progress, wait for it
        if (currentDeferred != null) {
            return@withContext currentDeferred?.await() ?: false
        }

        val deferred = CompletableDeferred<Boolean>()
        currentDeferred = deferred

        try {
            val intent = Intent(context, CloudflareActivity::class.java).apply {
                putExtra("url", url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            
            // Wait for the activity to call onResolutionResult
            deferred.await()
        } catch (e: Exception) {
            Log.e("CloudflareResolver", "Failed to launch CloudflareActivity", e)
            currentDeferred = null
            false
        }
    }

    override fun getUserAgent(): String {
        return sharedPrefs.getString("user_agent", "") ?: ""
    }

    fun saveUserAgent(userAgent: String) {
        sharedPrefs.edit { putString("user_agent", userAgent) }
    }
}
