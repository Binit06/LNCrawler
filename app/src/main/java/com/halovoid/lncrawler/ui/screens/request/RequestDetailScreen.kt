package com.halovoid.lncrawler.ui.screens.request

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.ui.ViewModelFactory
import com.halovoid.lncrawler.ui.components.artifact.ArtifactCard
import com.halovoid.lncrawler.ui.components.RequestCard
import com.halovoid.lncrawler.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    requestId: String?,
    onBackClick: () -> Unit,
    onRequestClick: (String) -> Unit
) {
    if (requestId == null) {
        onBackClick()
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val factory = remember { ViewModelFactory(context.applicationContext as android.app.Application) }
    val viewModel: RequestDetailViewModel = viewModel(factory = factory)

    val record by viewModel.getRequest(requestId).collectAsState(initial = null)
    val linkedRequests by viewModel.linkedRequests.collectAsStateWithLifecycle()
    val cancellingRequestIds by viewModel.cancellingRequestIds.collectAsStateWithLifecycle()
    val chapterMetadata by viewModel.chapterMetadata.collectAsState()
    val artifactMetadata by viewModel.artifactMetadata.collectAsState()

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/epub+zip"
        )
    ) { uri ->
        if (uri != null && artifactMetadata != null) {
            viewModel.copyArtifactToUri(
                artifact = artifactMetadata!!,
                destinationUri = uri,
                onComplete = { resultUri ->
                    if (resultUri != null) {
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Exported: ${artifactMetadata!!.artifactName}",
                                actionLabel = "OPEN",
                                duration = SnackbarDuration.Long
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(resultUri, "application/epub+zip")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Open with"))
                            }
                        }
                    }
                },
                onFileMissing = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Original file not found. It may have been removed or deleted.",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            )
        }
    }

    LaunchedEffect(requestId) {
        viewModel.setRequestId(requestId)
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Request Details", fontWeight = FontWeight.Bold, color = PrimaryText) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { innerPadding ->
        if (record == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryAccent)
            }
        } else {
            val currentRecord = record!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Main Request Card
                item {
                    RequestCard(
                        request = currentRecord,
                        onClick = { /* Already here */ },
                        onReplay = { viewModel.replayRequest(currentRecord.id) },
                        onCancel = { viewModel.cancelRequest(currentRecord.id) }
                    )
                }

                // Metadata Table (CHAPTER type only)
                if (currentRecord.type == RequestType.CHAPTER && chapterMetadata != null) {
                    item {
                        Column {
                            Text(
                                "Chapter Metadata",
                                style = MaterialTheme.typography.titleMedium,
                                color = PrimaryText,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            MetadataTable(
                                mapOf(
                                    "Title" to chapterMetadata!!.title,
                                    "Serial" to chapterMetadata!!.index.toString(),
                                    "URL" to chapterMetadata!!.url,
                                    "Novel URL" to chapterMetadata!!.novelUrl
                                )
                            )
                        }
                    }
                }

                // Download Columns - if artifact metadata is not null that means the file has been saved in the folder
                // The user shall be able to download the file from the saved places
                // By download it means to just copy and paste to his desired place
                if (currentRecord.type == RequestType.ARTIFACT && artifactMetadata != null) {
                    item {
                        ArtifactCard(
                            artifact = artifactMetadata!!,
                            onDownload = {
                                saveLauncher.launch(it.artifactName)
                            }
                        )
                    }
                }

                // Linked Requests Section
                if (linkedRequests.isNotEmpty()) {
                    item {
                        Text(
                            "Linked Requests",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(linkedRequests) { linked ->
                        RequestCard(
                            request = linked,
                            onClick = { onRequestClick(linked.id) },
                            onReplay = { viewModel.replayRequest(linked.id) },
                            onCancel = { viewModel.cancelRequest(linked.id) },
                            isCancelling = cancellingRequestIds.contains(linked.id)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataTable(data: Map<String, String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .padding(8.dp)
    ) {
        data.forEach { (key, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = key, color = SecondaryText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(text = value, color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(2f))
            }
            if (key != data.keys.last()) {
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            }
        }
    }
}
