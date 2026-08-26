package com.halovoid.lncrawler.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import coil.compose.AsyncImage
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.ui.theme.*

@Composable
fun NovelCard(
    novel: Novel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        var coverModel by remember(novel.coverUrl, novel.coverHttpsUrl) {
            mutableStateOf(novel.coverUrl ?: novel.coverHttpsUrl)
        }
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = coverModel,
                contentDescription = novel.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = {
                    if (coverModel != novel.coverHttpsUrl) {
                        coverModel = novel.coverHttpsUrl
                    }
                }
            )

            // Gradient overlay for text readability at bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 300f
                        )
                    )
            )

            // Title and Metadata overlay (Bottom)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .padding(bottom = 4.dp) // Space for progress bar
            ) {
                Text(
                    text = novel.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                if (novel.chapters.isNotEmpty()) {
                    Text(
                        text = "${novel.chapters.size} Chapters",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText,
                        fontSize = 10.sp
                    )
                }
            }

            // Subtle Progress indicator (Bottom)
            if (novel.chapters.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { 0.35f }, // Placeholder: 35% read
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(2.dp),
                    color = BrandAccent,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}
