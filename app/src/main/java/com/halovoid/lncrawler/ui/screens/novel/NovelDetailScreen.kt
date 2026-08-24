package com.halovoid.lncrawler.ui.screens.novel

import android.app.Application
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.domain.models.Artifact
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.data.handlers.utility.parsedMetadata
import com.halovoid.lncrawler.ui.ViewModelFactory
import com.halovoid.lncrawler.ui.components.ConfirmDeleteDialog
import com.halovoid.lncrawler.ui.components.DownloadRangeDialog
import com.halovoid.lncrawler.ui.components.ExportWarningDialog
import com.halovoid.lncrawler.ui.components.artifact.ExportFormat
import com.halovoid.lncrawler.ui.components.requestHistorySection
import com.halovoid.lncrawler.ui.screens.novel.components.*
import com.halovoid.lncrawler.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalResources

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
    val listState = rememberLazyListState()
    val density = LocalResources.current.displayMetrics.density
    val heroHeightPx = remember { (340 * density).toInt() }

    val isTopBarOpaque by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    val showTitleInTopBar by remember {
        derivedStateOf {
            val firstItemIndex = listState.firstVisibleItemIndex
            val firstItemOffset = listState.firstVisibleItemScrollOffset
            firstItemIndex > 0 || (firstItemIndex == 0 && firstItemOffset > heroHeightPx)
        }
    }

    var descriptionExpanded by remember { mutableStateOf(value = false) }

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
            Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // 1. Hero Info (Title, Author, Source, Small Cover)
                    item {
                        NovelHeroSection(novel = currentNovel)
                    }

                    // 2. Metadata Grid (Author, Volumes, etc)
                    item {
                        NovelMetadataTable(novel = currentNovel)
                    }

                    // 3. Synopsis
                    item {
                        NovelSynopsisSection(
                            novel = currentNovel,
                            isExpanded = descriptionExpanded,
                            onExpandClick = { descriptionExpanded = !descriptionExpanded }
                        )
                    }

                    // 4. Request History (Active crawls)
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

                    // 5. Artifacts (Exports)
                    novelArtifactsSection(
                        artifacts = artifacts,
                        onDownload = {
                            selectedArtifact = it
                            exportLauncher.launch(it.artifactName)
                        }
                    )

                    // 6. Actions (Manual controls)
                    item {
                        NovelActionsSection(
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
                            }
                        )
                    }

                    // 7. Table of Contents
                    novelTableOfContents(
                        novel = currentNovel,
                        chapters = chapters,
                        downloadingChapters = downloadingChapters,
                        onFetchChapter = { viewModel.fetchChapter(currentNovel, it) },
                        onChapterClick = { onChapterClick(currentNovel.url, it.id) }
                    )
                }

                // Dynamic Top Bar
                NovelTopBar(
                    novel = currentNovel,
                    isOpaque = isTopBarOpaque,
                    showTitle = showTitleInTopBar,
                    onBack = onBack,
                    onDownloadClick = { showDownloadDialog = true },
                    onRefreshMetadata = { viewModel.fetchNovelMetadata(currentNovel) },
                    onDeleteNovel = { showDeleteConfirmation = true }
                )
            }

            // Dialogs
            if (showDeleteConfirmation) {
                ConfirmDeleteDialog (
                    title = "Delete request?",
                    message = "This will permanently remove \"${currentNovel.title}\" from your request history. This action cannot be undone.",
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
