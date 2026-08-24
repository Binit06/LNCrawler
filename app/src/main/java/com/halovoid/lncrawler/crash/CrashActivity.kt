package com.halovoid.lncrawler.crash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.halovoid.lncrawler.MainActivity
import com.halovoid.lncrawler.data.repository.PreferenceRepository
import com.halovoid.lncrawler.data.repository.StorageRepositoryImpl
import com.halovoid.lncrawler.ui.screens.crash.CrashScreen
import com.halovoid.lncrawler.ui.theme.LNCrawlerTheme

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val exception = GlobalExceptionHandler.getThrowableFromIntent(intent)
        val storageRepository = StorageRepositoryImpl.getInstance(applicationContext)

        setContent {
            LNCrawlerTheme {
                CrashScreen(
                    exception = exception,
                    storageRepository = storageRepository,
                    onRestartClick = {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}
