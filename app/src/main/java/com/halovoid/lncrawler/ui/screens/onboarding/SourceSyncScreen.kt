package com.halovoid.lncrawler.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.halovoid.lncrawler.api.loader.SourceLoader
import com.halovoid.lncrawler.ui.theme.PrimaryAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SourceSyncScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sourceLoader = remember { SourceLoader(context) }
    
    var status by remember { mutableStateOf("Initializing sync...") }
    var isSyncing by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                status = "Connecting to source repository..."
                delay(500.milliseconds) // Small delay for UX
                sourceLoader.loadSources()
                status = "Successfully loaded crawlers!"
                isSyncing = false
                delay(800.milliseconds)
                onComplete()
            } catch (e: Exception) {
                error = e.message ?: "An unknown error occurred"
                isSyncing = false
            }
        }
    }

    OnboardingStep(
        title = "Syncing Crawlers",
        subtitle = "We're downloading the latest sources so you can start crawling novels immediately.",
        buttonText = if (error != null) "Retry" else "Continue",
        onNext = {
            if (error != null) {
                // Trigger retry logic or just move on
                onComplete() 
            } else {
                onComplete()
            }
        },
        isNextEnabled = !isSyncing
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    color = PrimaryAccent,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(
                text = error ?: status,
                style = MaterialTheme.typography.bodyLarge,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
