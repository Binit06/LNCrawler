package com.halovoid.lncrawler.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halovoid.lncrawler.api.loader.SourceLoader
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.text.font.FontWeight
import com.halovoid.lncrawler.ui.theme.BrandAccent
import com.halovoid.lncrawler.ui.theme.DarkSurface
import com.halovoid.lncrawler.ui.theme.PrimaryText
import com.halovoid.lncrawler.ui.theme.SecondaryText
import com.halovoid.lncrawler.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@Composable
fun SourceSyncScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sourceLoader = remember { SourceLoader(context) }
    
    val logs = remember { mutableStateListOf<LogEntry>() }
    var isSyncing by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val listState = rememberLazyListState()

    fun addLog(message: String) {
        logs.add(LogEntry(message = message))
    }

    fun performSync() {
        isSyncing = true
        error = null
        scope.launch {
            try {
                sourceLoader.loadSources(onProgress = { log ->
                    addLog(log)
                })
                isSyncing = false
            } catch (e: SourceLoader.IncompatibleAppException) {
                error = "App Update Required: ${e.message}"
                addLog("Error: $error")
                isSyncing = false
            } catch (e: Exception) {
                error = e.message ?: "An unknown error occurred"
                addLog("Error: $error")
                isSyncing = false
            }
        }
    }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        performSync()
    }

    OnboardingStep(
        title = "Syncing Crawlers",
        subtitle = "We're downloading the latest sources so you can start crawling novels immediately.",
        buttonText = if (error != null) "Retry Sync" else "Continue",
        onNext = {
            if (error != null) {
                performSync()
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .padding(12.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(logs, key = { it.id }) { logEntry ->
                        Text(
                            text = if (logEntry.message.startsWith("Error")) "✖ ${logEntry.message}" else "✔ ${logEntry.message}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            ),
                            color = when {
                                logEntry.message.startsWith("Error") -> MaterialTheme.colorScheme.error
                                logEntry.message.contains("Complete") -> SuccessGreen
                                else -> PrimaryText.copy(alpha = 0.8f)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isSyncing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = BrandAccent,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Synchronizing...",
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandAccent
                    )
                }
            } else if (error != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Sync Failed",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onComplete) {
                        Text(
                            text = "Proceed Anyway",
                            color = SecondaryText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sources up to date",
                        style = MaterialTheme.typography.titleSmall,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Immutable
data class LogEntry(
    val id: Long = System.nanoTime(),
    val message: String
)
