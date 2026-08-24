package com.halovoid.lncrawler.ui.screens.novel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.ui.theme.*

@Composable
fun NovelMetadataTable(novel: Novel) {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        MetadataSection(
            mapOf(
                "Author" to (novel.author ?: "Unknown"),
                "Volumes" to novel.volumes.size.toString(),
                "Chapters" to novel.chapters.size.toString(),
                "Source" to novel.crawlerName
            )
        )
    }
}

@Composable
fun MetadataSection(data: Map<String, String>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        data.forEach { (key, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = key, 
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value, 
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryText, 
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
