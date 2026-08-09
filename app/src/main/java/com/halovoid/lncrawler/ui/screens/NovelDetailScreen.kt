package com.halovoid.lncrawler.ui.screens

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.halovoid.lncrawler.domain.models.Artifact
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.Volume
import com.halovoid.lncrawler.ui.ViewModelFactory
import com.halovoid.lncrawler.ui.components.artifact.ArtifactCard
import com.halovoid.lncrawler.ui.components.RequestCard
import com.halovoid.lncrawler.ui.components.artifact.ArtifactExportButton
import com.halovoid.lncrawler.ui.components.artifact.ExportFormat
import com.halovoid.lncrawler.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelDetailScreen(
    novelUrl: String,
    onRequestClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val factory = remember { ViewModelFactory(context.applicationContext as android.app.Application) }
    val viewModel: NovelDetailViewModel = viewModel(factory = factory)
    
    val novel by viewModel.novel.collectAsState()
    var descriptionExpanded by remember { mutableStateOf(false) }

    val requestHistory by viewModel.rootRequests.collectAsStateWithLifecycle()
    val artifacts by viewModel.artifacts.collectAsStateWithLifecycle()
    var selectedArtifact by remember { mutableStateOf<Artifact?>(null) }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Open external link */ }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open in browser", tint = PrimaryAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        if (novel == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryAccent)
            }
        } else {
            val currentNovel = novel!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = innerPadding.calculateTopPadding(), bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Hero Section
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = currentNovel.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .width(180.dp)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
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
                    MetadataSection(
                        mapOf(
                            "Author" to (currentNovel.author ?: "Unknown"),
                            "Volumes" to currentNovel.volumes.size.toString(),
                            "Chapters" to currentNovel.chapters.size.toString(),
                            "Source" to currentNovel.crawlerName
                        )
                    )
                }

                // Synopsis
                item {
                    Column(modifier = Modifier.animateContentSize()) {
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
                items(requestHistory) { request ->
                    RequestCard(
                        request = request,
                        onClick = { onRequestClick(request.id) },
                        onReplay = { },
                        onCancel = { }
                    )
                }

                // Artifacts Section
                if (artifacts.isNotEmpty()) {
                    item {
                        Text(
                            "Artifacts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(artifacts) { artifact ->
                        ArtifactCard (
                            artifact = artifact,
                            onDownload = {
                                selectedArtifact = it
                                exportLauncher.launch(it.artifactName)
                            }
                        )
                    }
                }

                // Actions Section (FETCH only)
                item {
                    ActionsSection(
                        onFetchMetadata = { viewModel.fetchNovelMetadata(currentNovel) },
                        onFetchFull = { viewModel.fetchFullNovel(currentNovel) },
                        onExport = { format ->
                            viewModel.startBackgroundExport(currentNovel, format)
                        }
                    )
                }

                // Table of Contents
                item {
                    Text(
                        "Table of Contents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(currentNovel.volumes) { volume ->
                    val volumeChapters = currentNovel.chapters.filter { it.volumeId == volume.id }
                    ExpandableVolume(
                        volume = volume,
                        chapters = volumeChapters,
                        onFetchVolume = { viewModel.fetchVolume(currentNovel, volume.volumeIndex) }
                    )
                }
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
    onFetchFull: () -> Unit,
    onExport: (ExportFormat) -> Unit
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
            icon = Icons.Default.DownloadForOffline,
            title = "Fetch Full Novel",
            subtext = "Download all volumes and chapters",
            onClick = onFetchFull
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
fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtext: String, onClick: () -> Unit) {
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

@Composable
fun ExpandableVolume(volume: Volume, chapters: List<Chapter>, onFetchVolume: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(bottom = if (expanded) 8.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Volume ${volume.volumeIndex}", color = PrimaryText, fontWeight = FontWeight.Bold)
                Text("${chapters.size} Chapters", color = SecondaryText, fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onFetchVolume) {
                    Icon(Icons.Default.Download, contentDescription = "Fetch Volume", tint = PrimaryAccent)
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = SecondaryText
                )
            }
        }
        
        AnimatedVisibility(visible = expanded) {
            Column {
                chapters.forEach { chapter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${chapter.index}",
                            color = PrimaryAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(32.dp)
                        )
                        Text(
                            text = chapter.title,
                            color = SecondaryText,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (chapter.fileLocation != null && chapter.fileLocation.contains("content://")) {
                            Button(
                                onClick = { /* Read chapter */ },
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Read", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
