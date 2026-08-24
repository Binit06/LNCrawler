package com.halovoid.lncrawler.ui.screens.novel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onChapterClick: (Chapter) -> Unit,
    isDownloading: Boolean
) {
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
        
        if (chapter.fileLocation?.contains("content://") == true) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Downloaded",
                tint = SuccessGreen,
                modifier = Modifier.size(20.dp) // Smaller, more subtle indicator
            )
        } else if (isDownloading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = BrandAccent
            )
        } else {
            IconButton(
                onClick = { onFetchChapter(chapter) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DownloadForOffline,
                    contentDescription = "Download Chapter",
                    tint = SecondaryText.copy(alpha = 0.5f), // Very subtle until interaction
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
