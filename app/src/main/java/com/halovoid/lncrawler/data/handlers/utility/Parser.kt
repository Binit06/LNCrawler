package com.halovoid.lncrawler.data.handlers.utility

import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.scheduler.RequestMetadata
import org.json.JSONObject

val RequestEntity.parsedMetadata: RequestMetadata
    get() {
        if (this.metadata.isNullOrBlank()) return RequestMetadata()
        return try {
            val json = JSONObject(this.metadata)
            RequestMetadata(
                crawlerName = json.optString("crawlerName", null),
                artifactFormat = json.optString("artifactFormat", null)
            )
        } catch (e: Exception) {
            RequestMetadata()
        }
    }