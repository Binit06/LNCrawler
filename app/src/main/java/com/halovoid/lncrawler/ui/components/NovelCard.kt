package com.halovoid.lncrawler.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.halovoid.lncrawler.domain.models.Novel

/**
 * A reusable UI component that displays a summary card for a novel.
 * Shows the cover image, title, and author, with optional export functionality.
 *
 * @param novel The [Novel] data to display.
 * @param onClick Callback triggered when the card is tapped.
 * @param onExportClick Optional callback for the export button. If null, the button is hidden.
 */
@Composable
fun NovelCard(
    novel: Novel,
    onClick: () -> Unit,
    onExportClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = novel.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp, 120.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = novel.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = novel.author ?: "Unknown Author",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (onExportClick != null) {
                IconButton(onClick = onExportClick) {
                    Icon(Icons.Default.Download, contentDescription = "Export")
                }
            }
        }
    }
}
