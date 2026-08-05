package com.halovoid.lncrawler.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

/**
 * Detailed view of a novel, displaying synopsis, chapter list, and background export status.
 */
@Composable
fun NovelDetailScreen(
    crawlerName: String,
    novelUrl: String,
    viewModel: NovelDetailViewModel = viewModel()
) {
    val novel by viewModel.novel.collectAsState()
    val progressMap by viewModel.exportProgressMap.collectAsState()
    val progress = progressMap[novelUrl]
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/epub+zip")
    ) { uri ->
        uri?.let {
            novel?.let { n ->
                viewModel.startBackgroundExport(n, it)
                Toast.makeText(context, "Export started in background", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(novelUrl) {
        viewModel.loadNovel(crawlerName, novelUrl)
    }

    //TODO: When progress.progress reaches 100% show a toast showing EPUB has been exported

    if (novel == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                item {
                    Row {
                        AsyncImage(
                            model = novel?.coverUrl,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp, 180.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = novel?.title ?: "", style = MaterialTheme.typography.headlineSmall)
                            Text(text = novel?.author ?: "Unknown", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (progress != null) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        LinearProgressIndicator(
                                            progress = { progress.progress },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(12.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                        IconButton(onClick = { viewModel.cancelExport(novelUrl) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    Text(
                                        text = "${progress.status} (${progress.currentChapter}/${progress.totalChapters})",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { 
                                        val fileName = "${novel?.title?.filter { it.isLetterOrDigit() } ?: "novel"}.epub"
                                        exportLauncher.launch(fileName)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Export to EPUB")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "Synopsis", style = MaterialTheme.typography.titleLarge)
                    Text(text = novel?.description ?: "", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "Chapters (${novel?.chapters?.size})", style = MaterialTheme.typography.titleLarge)
                }
                items(novel?.chapters ?: emptyList()) { chapter ->
                    ListItem(
                        headlineContent = { Text(chapter.title) },
                        supportingContent = { Text("Chapter ${chapter.index + 1}") }
                    )
                }
            }
        }
    }
}
