package com.halovoid.lncrawler

import android.app.Application
import com.halovoid.lncrawler.api.core.scrapper.Scrapper
import com.halovoid.lncrawler.crash.CrashActivity
import com.halovoid.lncrawler.crash.GlobalExceptionHandler
import com.halovoid.lncrawler.ui.cloudflare.CloudflareResolverImpl

class LNCrawlerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalExceptionHandler.initialize(this, CrashActivity::class.java)

        // Initialize Cloudflare Resolver
        Scrapper.globalResolver = CloudflareResolverImpl(this)
    }
}
