package com.halovoid.lncrawler.ui.screens.novel

import android.app.Application
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.domain.models.Artifact
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.domain.models.Volume
import com.halovoid.lncrawler.data.handlers.utility.parsedMetadata
import com.halovoid.lncrawler.ui.ViewModelFactory
import com.halovoid.lncrawler.ui.components.ConfirmDeleteDialog
import com.halovoid.lncrawler.ui.components.DownloadRangeDialog
import com.halovoid.lncrawler.ui.components.ExportWarningDialog
import com.halovoid.lncrawler.ui.components.artifact.ArtifactCard
import com.halovoid.lncrawler.ui.components.artifact.ArtifactExportButton
import com.halovoid.lncrawler.ui.components.artifact.ExportFormat
import com.halovoid.lncrawler.ui.components.requestHistorySection
import com.halovoid.lncrawler.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelDetailScreen(
    novelUrl: String,
    onRequestClick: (String) -> Unit,
    onGroupClick: (RequestType) -> Unit,
    onChapterClick: (String, Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val factory = remember { ViewModelFactory(context.applicationContext as Application) }
    val viewModel: NovelDetailViewModel = viewModel(factory = factory)
    
    val novel by viewModel.novel.collectAsState()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val chapterRange by viewModel.chapterRange.collectAsState()
    var descriptionExpanded by remember { mutableStateOf(false) }

    val requestHistory by viewModel.rootRequests.collectAsStateWithLifecycle()
    
    val downloadingChapters = remember(requestHistory) {
        val ids = mutableSetOf<Int>()
        val ranges = mutableListOf<ClosedRange<Int>>()
        
        requestHistory.filter { it.status == RequestStatus.RUNNING || it.status == RequestStatus.PENDING }.forEach { req ->
            val meta = req.parsedMetadata
            if (req.type == RequestType.CHAPTER) {
                meta.chapterId?.let { ids.add(it) }
            } else if (req.type == RequestType.RANGE_DOWNLOAD) {
                val start = meta.startIndex
                val end = meta.endIndex
                if (start != null && end != null) {
                    ranges.add(start..end)
                }
            }
        }
        Pair(ids, ranges)
    }

    val activeActionIds by viewModel.activeActionIds.collectAsStateWithLifecycle()
    val cancellingRequestIds by viewModel.cancellingRequestIds.collectAsStateWithLifecycle()

    val artifacts by viewModel.artifacts.collectAsStateWithLifecycle()
    var selectedArtifact by remember { mutableStateOf<Artifact?>(null) }

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showExportWarning by remember { mutableStateOf(false) }
    var pendingExportFormat by remember { mutableStateOf<ExportFormat?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/epub+zip")
    ) { uri ->
        uri?.let { destUri ->
            selectedArtifact?.let { artifact ->
                viewModel.copyArtifactToUri(
                    artifact = artifact,
                    destinationUri = destUri,
                    onComplete = { resultUri ->
                        if (resultUri != null) {
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Exported: ${artifact.artifactName}",
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
    }

    LaunchedEffect(novelUrl) {
        viewModel.loadNovel(novelUrl)
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (novel == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryAccent)
            }
        } else {
            val currentNovel = novel!!
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Hero Section
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(top = 72.dp, bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = currentNovel.coverUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .width(160.dp)
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = currentNovel.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = currentNovel.crawlerName,
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryAccent,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Metadata Table
                    item {
                        Box(modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp)) {
                            MetadataSection(
                                mapOf(
                                    "Author" to (currentNovel.author ?: "Unknown"),
                                    "Volumes" to currentNovel.volumes.size.toString(),
                                    "Chapters" to currentNovel.chapters.size.toString(),
                                    "Source" to currentNovel.crawlerName
                                )
                            )
                        }
                    }

                    // Synopsis
                    item {
                        Column(modifier = Modifier
                            .animateContentSize()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp)) {
                            Text(
                                "Synopsis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = currentNovel.description ?: "No description available.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondaryText,
                                maxLines = if (descriptionExpanded) Int.MAX_VALUE else 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (descriptionExpanded) "Read Less" else "Read More",
                                color = PrimaryAccent,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .clickable { descriptionExpanded = !descriptionExpanded }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }

                    // Request History Section
                    requestHistorySection(
                        requestHistory = requestHistory,
                        onRequestClick = onRequestClick,
                        onGroupClick = onGroupClick,
                        onReplay = { viewModel.replayRequest(it) },
                        onContinue = { viewModel.resumeRequest(it) },
                        cancellingRequestIds = cancellingRequestIds,
                        activeActionIds = activeActionIds,
                        horizontalPadding = 24.dp,
                        allowAction = true
                    )

                    item { Spacer(modifier = Modifier.height(12.dp)) }

                    // Artifacts Section
                    if (artifacts.isNotEmpty()) {
                        item {
                            Text(
                                "Artifacts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText,
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .padding(bottom = 8.dp)
                            )
                        }
                        items(artifacts) { artifact ->
                            Box(modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 8.dp)) {
                                ArtifactCard (
                                    artifact = artifact,
                                    onDownload = {
                                        selectedArtifact = it
                                        exportLauncher.launch(it.artifactName)
                                    }
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    // Actions Section (FETCH only)
                    item {
                        Box(modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp)) {
                            ActionsSection(
                                onFetchMetadata = { viewModel.fetchNovelMetadata(currentNovel) },
                                onShowDownloadRange = { showDownloadDialog = true },
                                onExport = { format ->
                                    val start = chapterRange.start.toInt()
                                    val end = chapterRange.endInclusive.toInt()
                                    val rangeChapters = chapters.filter { it.index in start..end }
                                    val downloaded = rangeChapters.count { it.fileLocation?.startsWith("content://") == true }

                                    if (downloaded < rangeChapters.size) {
                                        pendingExportFormat = format
                                        showExportWarning = true
                                    } else {
                                        viewModel.startBackgroundExport(currentNovel, format)
                                    }
                                },
                                onDelete = {
                                    showDeleteConfirmation = true
                                }
                            )
                        }
                    }

                    // Table of Contents
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(top = 12.dp, bottom = 8.dp)
                        ) {
                            Text(
                                "Table of Contents",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "${chapters.size} Chapters",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
                            )
                        }
                    }

                    currentNovel.volumes.forEach { volume ->
                        item(key = "vol_${volume.id}") {
                            Text(
                                text = "Volume ${volume.volumeIndex}",
                                style = MaterialTheme.typography.labelMedium,
                                color = PrimaryAccent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkBackground)
                                    .padding(horizontal = 24.dp)
                                    .padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        
                        val volumeChapters = chapters.filter { it.volumeId == volume.id }
                        items(volumeChapters, key = { it.id }) { chapter ->
                            val isDownloading = remember(downloadingChapters, chapter.id, chapter.index) {
                                downloadingChapters.first.contains(chapter.id) || 
                                downloadingChapters.second.any { it.contains(chapter.index) }
                            }
                            ChapterRow(
                                chapter = chapter,
                                onFetchChapter = { viewModel.fetchChapter(currentNovel, it) },
                                onChapterClick = { onChapterClick(currentNovel.url, it.id) },
                                isDownloading = isDownloading
                            )
                            HorizontalDivider(
                                color = BorderColor.copy(alpha = 0.2f), 
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }

                // Integrated Floating Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryText
                        )
                    }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, novelUrl.toUri())
                        context.startActivity(intent)
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in browser",
                            tint = PrimaryAccent
                        )
                    }
                }
            }

            if (showDeleteConfirmation) {
                val displayName = currentNovel.title
                ConfirmDeleteDialog (
                    title = "Delete request?",
                    message = "This will permanently remove \"$displayName\" from your request history. This action cannot be undone.",
                    onConfirm = {
                        showDeleteConfirmation = false
                        viewModel.deleteNovelPermanently(currentNovel)
                        onBack()
                    },
                    onDismiss = { showDeleteConfirmation = false }
                )
            }

            if (showDownloadDialog) {
                DownloadRangeDialog(
                    initialRange = chapterRange,
                    totalChapters = currentNovel.chapters.size,
                    onConfirm = { range ->
                        viewModel.updateChapterRange(range)
                        viewModel.fetchRange(currentNovel)
                        showDownloadDialog = false
                    },
                    onDismiss = { showDownloadDialog = false }
                )
            }

            if (showExportWarning && pendingExportFormat != null) {
                val start = chapterRange.start.toInt()
                val end = chapterRange.endInclusive.toInt()
                val rangeChapters = chapters.filter { it.index in start..end }
                val downloadedCount = rangeChapters.count { it.fileLocation?.startsWith("content://") == true }

                ExportWarningDialog(
                    totalSelected = rangeChapters.size,
                    downloadedCount = downloadedCount,
                    onDownloadFirst = {
                        showExportWarning = false
                        viewModel.fetchRange(currentNovel)
                    },
                    onExportAnyway = {
                        showExportWarning = false
                        viewModel.startBackgroundExport(currentNovel, pendingExportFormat!!)
                    },
                    onDismiss = {
                        showExportWarning = false
                    }
                )
            }
        }
    }
}

@Composable
fun ChapterRow(
    chapter: Chapter,
    onFetchChapter: (Chapter) -> Unit,
    onChapterClick: (Chapter) -> Unit,
    isDownloading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .clickable { onChapterClick(chapter) }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Chapter ${chapter.index}",
                color = PrimaryText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = chapter.title.ifBlank { "Unknown" },
                color = SecondaryText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        if (chapter.fileLocation?.contains("content://") == true) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Downloaded",
                tint = SuccessGreen,
                modifier = Modifier.size(28.dp)
            )
        } else if (isDownloading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = PrimaryAccent
            )
        } else {
            IconButton(
                onClick = { onFetchChapter(chapter) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DownloadForOffline,
                    contentDescription = "Download Chapter",
                    tint = PrimaryAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun MetadataSection(data: Map<String, String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(16.dp)
    ) {
        data.forEach { (key, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = key, color = SecondaryText, fontSize = 14.sp)
                Text(text = value, color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            if (key != data.keys.last()) {
                HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
fun ActionsSection(
    onFetchMetadata: () -> Unit,
    onShowDownloadRange: () -> Unit,
    onExport: (ExportFormat) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(8.dp)
    ) {
        Text(
            "FETCH",
            style = MaterialTheme.typography.labelSmall,
            color = PrimaryAccent,
            modifier = Modifier.padding(
                start = 8.dp,
                top = 8.dp,
                bottom = 4.dp
            )
        )

        ActionRow(
            icon = Icons.Default.Refresh,
            title = "Refresh Metadata",
            subtext = "Update novel info and chapter list",
            onClick = onFetchMetadata
        )

        HorizontalDivider(color = BorderColor)

        ActionRow(
            icon = Icons.Default.Download,
            title = "Download Range",
            subtext = "Select chapter range to download",
            onClick = onShowDownloadRange
        )

        HorizontalDivider(color = BorderColor)

        ActionRow(
            icon = Icons.Default.DeleteForever,
            title = "Delete Novel",
            subtext = "Permanently remove this novel and all assosiated data",
            onClick = onDelete
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "EXPORT",
            style = MaterialTheme.typography.labelSmall,
            color = PrimaryAccent,
            modifier = Modifier.padding(
                start = 8.dp,
                top = 8.dp,
                bottom = 4.dp
            )
        )

        ArtifactExportButton(
            onExport = onExport
        )
    }
}

@Composable
fun ActionRow(icon: ImageVector, title: String, subtext: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = PrimaryText, fontWeight = FontWeight.Medium)
            Text(subtext, color = SecondaryText, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SecondaryText)
    }
}
