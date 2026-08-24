package com.halovoid.lncrawler.ui.screens.novel.components

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.ui.theme.*

@Composable
fun NovelTopBar(
    novel: Novel,
    isOpaque: Boolean,
    showTitle: Boolean,
    onBack: () -> Unit,
    onDownloadClick: () -> Unit,
    onRefreshMetadata: () -> Unit,
    onDeleteNovel: () -> Unit
) {
    val context = LocalContext.current

    // Smoothly animate the background color
    val backgroundColor by animateColorAsState(
        targetValue = if (isOpaque) DarkBackground else Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label = "TopBarBackgroundAnimation"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor,
        tonalElevation = 0.dp // Remove heavy elevation, use background color only
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryText
                )
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showTitle,
                    enter = fadeIn(animationSpec = tween(durationMillis = 250)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 200))
                ) {
                    Text(
                        text = novel.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            IconButton(onClick = onDownloadClick) {
                Icon(Icons.Default.Download, contentDescription = "Download", tint = PrimaryText)
            }

            IconButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, novel.url.toUri())
                context.startActivity(intent)
            }) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = "Open in browser",
                    tint = SecondaryText // Muted icon for secondary browser action
                )
            }

            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = PrimaryText)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Refresh Metadata", color = PrimaryText) },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = SecondaryText) },
                        onClick = {
                            showMenu = false
                            onRefreshMetadata()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Novel", color = ErrorRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                        onClick = {
                            showMenu = false
                            onDeleteNovel()
                        }
                    )
                }
            }
        }
    }
}
