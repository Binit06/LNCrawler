package com.halovoid.lncrawler

import android.app.Application
import com.halovoid.lncrawler.crash.CrashActivity
import com.halovoid.lncrawler.crash.GlobalExceptionHandler

class LNCrawlerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalExceptionHandler.initialize(this, CrashActivity::class.java)
    }
}
