package com.halovoid.lncrawler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.halovoid.lncrawler.ui.screens.MainScreen
import com.halovoid.lncrawler.ui.theme.LNCrawlerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LNCrawlerTheme {
                MainScreen()
            }
        }
    }
}
