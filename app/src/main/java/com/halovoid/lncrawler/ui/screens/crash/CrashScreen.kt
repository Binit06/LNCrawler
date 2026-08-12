package com.halovoid.lncrawler.ui.screens.crash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halovoid.lncrawler.crash.utils.CrashLogUtil
import com.halovoid.lncrawler.data.repository.StorageRepository
import com.halovoid.lncrawler.ui.screens.intermediates.InfoScreen
import kotlinx.coroutines.launch

@Composable
fun CrashScreen(
    exception: Throwable?,
    storageRepository: StorageRepository,
    onRestartClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    InfoScreen(
        icon = Icons.Outlined.BugReport,
        headingText = "Crash Occurred",
        subtitleText = "The app has crashed",
        acceptText = "Dump Crash Log",
        onAcceptClick = {
            scope.launch {
                CrashLogUtil(context, storageRepository).dumpLogs(exception)
            }
        },
        rejectText = "Close App",
        onRejectClick = onRestartClick,
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(
                text = exception.toString(),
                modifier = Modifier
                    .padding(all = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}