package com.halovoid.lncrawler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.halovoid.lncrawler.api.loader.SourceLoader
import com.halovoid.lncrawler.ui.screens.MainScreen
import com.halovoid.lncrawler.ui.theme.LNCrawlerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Load local sources on startup
        lifecycleScope.launch {
            SourceLoader(this@MainActivity).loadLocalSources()
        }

        enableEdgeToEdge()
        setContent {
            LNCrawlerTheme {
                MainScreen()
            }
        }
    }
}
