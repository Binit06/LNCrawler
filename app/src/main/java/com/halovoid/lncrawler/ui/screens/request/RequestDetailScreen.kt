package com.halovoid.lncrawler.ui.screens.request

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.halovoid.lncrawler.ui.ViewModelFactory

/**
 * Detailed view for a specific export request record.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RequestDetailScreen(
    requestId: Int?,
    onBackClick: () -> Unit
) {
    // Move Back a screen in case requestId comes to be null
    if (requestId == null) {
        onBackClick()
        return
    }

    val context = LocalContext.current
    val factory =
        remember { ViewModelFactory(context.applicationContext as android.app.Application) }
    val viewModel: RequestDetailViewModel = viewModel(factory = factory)

    // Using collectAsState directly from the Flow to ensure we get DB updates.
    val record by viewModel.getRequest(requestId).collectAsState(initial = null)
    val novel by viewModel.novel.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/epub+zip")
    ) { uri ->
        uri?.let {
            record?.let { r ->
                viewModel.replayRequest(it, r)
                Toast.makeText(context, "Re-request started", Toast.LENGTH_SHORT).show()
            }
        }
    }
}