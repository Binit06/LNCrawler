package com.halovoid.lncrawler.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

/**
 * UI Screen that displays the full details of a novel, including its synopsis and chapter list.
 * Provides the interface for initiating the SAF-based EPUB export process.
 *
 * @param crawlerName The name of the crawler that handles this novel's source.
 * @param novelUrl The landing page URL of the novel.
 * @param viewModel The ViewModel providing state and business logic for this screen.
 */
@Composable
fun NovelDetailScreen(
    crawlerName: String,
    novelUrl: String,
    viewModel: NovelDetailViewModel = viewModel()
) {
    val novel by viewModel.novel.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/epub+zip")
    ) { uri ->
        uri?.let {
            viewModel.exportToUri(crawlerName, it) {
                Toast.makeText(context, "Export complete!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(novelUrl) {
        viewModel.loadNovel(crawlerName, novelUrl)
    }

    if (novel == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                        Button(
                            onClick = { 
                                val fileName = "${novel?.title?.filter { it.isLetterOrDigit() } ?: "novel"}.epub"
                                exportLauncher.launch(fileName)
                            },
                            enabled = !isExporting
                        ) {
                            if (isExporting) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(exportProgress ?: "Exporting...")
                                }
                            } else {
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
