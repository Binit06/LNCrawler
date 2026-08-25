package com.halovoid.lncrawler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.halovoid.lncrawler.data.repository.UpdateRepository
import com.halovoid.lncrawler.data.scheduler.services.SchedulerService
import com.halovoid.lncrawler.ui.screens.MainScreen
import com.halovoid.lncrawler.ui.theme.LNCrawlerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Resume background tasks
        // Source loading is now handled at Application level
        lifecycleScope.launch {
            SchedulerService.startService(this@MainActivity)
            UpdateRepository.getInstance(this@MainActivity).checkForUpdates()
        }

        enableEdgeToEdge()
        setContent {
            LNCrawlerTheme {
                MainScreen()
            }
        }
    }
}
