package com.halovoid.lncrawler.ui.screens.novel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.ui.theme.*

fun LazyListScope.novelTableOfContents(
    novel: Novel,
    chapters: List<Chapter>,
    downloadingChapters: Pair<Set<Int>, List<ClosedRange<Int>>>,
    onFetchChapter: (Chapter) -> Unit,
    onDeleteChapter: (Chapter) -> Unit,
    onReplayChapter: (Chapter) -> Unit,
    onChapterClick: (Chapter) -> Unit
) {
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Text(
                text = "${chapters.size} Chapters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
        }
    }

    novel.volumes.forEach { volume ->
        item(key = "vol_${volume.id}") {
            Text(
                text = "Volume ${volume.volumeIndex}".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp, bottom = 8.dp)
            )
        }
        
        val volumeChapters = chapters.filter { it.volumeId == volume.id }.distinctBy { it.id }
        items(volumeChapters, key = { it.id }) { chapter ->
            val isDownloading = remember(downloadingChapters, chapter.id, chapter.index) {
                downloadingChapters.first.contains(chapter.id) || 
                downloadingChapters.second.any { it.contains(chapter.index) }
            }
            ChapterRow(
                chapter = chapter,
                onFetchChapter = { onFetchChapter(it) },
                onDeleteChapter = { onDeleteChapter(it) },
                onReplayChapter = { onReplayChapter(it) },
                onChapterClick = { onChapterClick(it) },
                isDownloading = isDownloading
            )
            HorizontalDivider(
                color = BorderColor.copy(alpha = 0.4f), 
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun ChapterRow(
    chapter: Chapter,
    onFetchChapter: (Chapter) -> Unit,
    onDeleteChapter: (Chapter) -> Unit,
    onReplayChapter: (Chapter) -> Unit,
    onChapterClick: (Chapter) -> Unit,
    isDownloading: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .clickable { onChapterClick(chapter) }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Chapter ${chapter.index}",
                color = PrimaryText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (chapter.title.isNotBlank()) {
                Text(
                    text = chapter.title,
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            if (chapter.fileLocation?.contains("content://") == true) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Options",
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = BrandAccent
                )
            } else {
                IconButton(
                    onClick = { onFetchChapter(chapter) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.DownloadForOffline,
                        contentDescription = "Download Chapter",
                        tint = SecondaryText.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (showMenu) {
            ChapterActionsBottomSheet(
                chapter = chapter,
                onDismiss = { showMenu = false },
                onDelete = { onDeleteChapter(chapter) },
                onReplay = { onReplayChapter(chapter) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterActionsBottomSheet(
    chapter: Chapter,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onReplay: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "Chapter ${chapter.index}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            if (chapter.title.isNotBlank()) {
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkBackground.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Replay Chapter", color = PrimaryText) },
                        leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null, tint = PrimaryText) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { 
                            onReplay()
                            onDismiss()
                        }
                    )
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.2f), thickness = 0.5.dp)
                    ListItem(
                        headlineContent = { Text("Delete Chapter", color = ErrorRed) },
                        leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { 
                            onDelete()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}
