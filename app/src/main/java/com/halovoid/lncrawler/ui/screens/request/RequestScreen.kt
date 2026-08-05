package com.halovoid.lncrawler.ui.screens.request

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.ui.components.NovelCard

/**
 * Main hub of the application. 
 * Features a top URL bar for new requests and a list of recently requested novels with background progress.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestScreen(
    onNovelClick: (String, String) -> Unit,
    viewModel: RequestViewModel = viewModel()
) {
    var urlInput by remember { mutableStateOf("") }
    val error by viewModel.error.collectAsState()
    val savedNovels by viewModel.savedNovels.collectAsState()
    val activeFetches by viewModel.activeFetches.collectAsState()
    val progressMap by viewModel.exportProgressMap.collectAsState()
    val context = LocalContext.current

    var novelToExport by remember { mutableStateOf<Novel?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/epub+zip")
    ) { uri ->
        uri?.let {
            novelToExport?.let { novel ->
                viewModel.startExport(novel, it)
                Toast.makeText(context, "Export started in background", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    TextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter Novel URL") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        singleLine = true
                    )
                    
                    IconButton(
                        onClick = {
                            val crawlerName = viewModel.validateUrl(urlInput)
                            if (crawlerName != null) {
                                viewModel.fetchNovel(crawlerName, urlInput) { name, url ->
                                    urlInput = ""
                                    onNovelClick(name, url)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward, 
                            contentDescription = "Fetch",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                AnimatedVisibility(visible = error != null) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        if (savedNovels.isEmpty() && activeFetches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No requests yet.\nEnter a URL above to start.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Ongoing Metadata Fetches
                items(activeFetches.toList()) { url ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Fetching metadata...", style = MaterialTheme.typography.labelMedium)
                                Text(url, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    }
                }

                // Saved Novels with background export progress
                items(savedNovels) { novel ->
                    val progress = progressMap[novel.url]
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        NovelCard(
                            novel = novel,
                            onClick = { onNovelClick(novel.crawlerName ?: "NovelBins", novel.url) },
                            onExportClick = {
                                novelToExport = novel
                                val fileName = "${novel.title.filter { it.isLetterOrDigit() }}.epub"
                                exportLauncher.launch(fileName)
                            }
                        )
                        
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { 
                                viewModel.fetchNovel(novel.crawlerName ?: "NovelBins", novel.url) { _, _ ->
                                    Toast.makeText(context, "Refetched successfully", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Rerequest", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { viewModel.removeNovel(novel) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(20.dp))
                            }
                        }

                        if (progress != null) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LinearProgressIndicator(
                                        progress = { progress.progress },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                    IconButton(
                                        onClick = { viewModel.cancelExport(novel.url) },
                                        modifier = Modifier.size(32.dp).padding(start = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Text(
                                    text = "${progress.status} (${progress.currentChapter}/${progress.totalChapters})",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
