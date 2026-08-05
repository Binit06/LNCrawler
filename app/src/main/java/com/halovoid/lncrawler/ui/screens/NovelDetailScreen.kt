package com.halovoid.lncrawler.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.halovoid.lncrawler.ui.theme.*

/**
 * Detailed view of a novel, displaying synopsis, chapter list, and background export status.
 * Inspired by Tachiyomi/Mihon dark theme.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    var descriptionExpanded by remember { mutableStateOf(false) }

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

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { /* Back navigation would go here if we had it */ }) {
                        // Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryText)
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Section
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = currentNovel.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .width(150.dp)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(8.dp))
                                .shadow(4.dp),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = currentNovel.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            ),
                            color = PrimaryText,
                            textAlign = TextAlign.Center
                        )
                        
                        if (!currentNovel.alternativeNames.isNullOrEmpty()) {
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = SecondaryText
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentNovel.alternativeNames,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Primary Action Button
                item {
                    if (progress != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurface)
                                .padding(16.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { progress.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = PrimaryAccent,
                                trackColor = BorderColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${progress.status} (${progress.currentChapter}/${progress.totalChapters})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecondaryText
                                )
                                TextButton(onClick = { viewModel.cancelExport(novelUrl) }) {
                                    Text("Cancel", color = Color.Red)
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                val fileName = "${currentNovel.title.filter { it.isLetterOrDigit() }}.epub"
                                exportLauncher.launch(fileName)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                        ) {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export to EPUB", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Description Section
                item {
                    Column(modifier = Modifier.animateContentSize()) {
                        Text(
                            text = currentNovel.description ?: "No description available.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = SecondaryText,
                            maxLines = if (descriptionExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (descriptionExpanded) "[view less]" else "[view more]",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = SecondaryText,
                            modifier = Modifier
                                .clickable { descriptionExpanded = !descriptionExpanded }
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                // Chapter List
                items(currentNovel.chapters) { chapter ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ch.${chapter.index + 1}",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 17.sp
                                    ),
                                    color = PrimaryAccent
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                    color = SecondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
