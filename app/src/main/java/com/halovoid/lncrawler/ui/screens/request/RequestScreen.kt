package com.halovoid.lncrawler.ui.screens.request

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.lncrawler.domain.models.ExportRecord
import com.halovoid.lncrawler.domain.models.ExportStatus
import com.halovoid.lncrawler.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Hub screen for creating novel requests and viewing export history.
 * Follows a developer-tool aesthetic with high information density.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestScreen(
    onNovelClick: (String, String) -> Unit,
    onHistoryClick: (Int) -> Unit,
    onLibraryClick: () -> Unit,
    viewModel: RequestViewModel
) {
    var urlInput by remember { mutableStateOf("") }
    val error by viewModel.error.collectAsState()
    val activeFetches by viewModel.activeFetches.collectAsState()
    val history by viewModel.exportHistory.collectAsStateWithLifecycle()
    val progressMap by viewModel.exportProgressMap.collectAsState()
    val context = LocalContext.current
    val isFetching = !activeFetches.isEmpty()
    var pendingDelete by remember { mutableStateOf<ExportRecord?>(null) }

    Scaffold(
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "LNCrawler", 
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    ) 
                },
                actions = {
                    IconButton(onClick = onLibraryClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.LibraryBooks, 
                            contentDescription = "Library", 
                            tint = PrimaryAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = PrimaryText,
                    actionIconContentColor = PrimaryAccent
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Request Creation Section
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(DarkSurface)
                            .padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Novel URL", color = SecondaryText) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = PrimaryAccent,
                                focusedTextColor = PrimaryText,
                                unfocusedTextColor = PrimaryText
                            ),
                            singleLine = true
                        )
                        
                        FilledIconButton(
                            onClick = {
                                val crawlerName = viewModel.validateUrl(urlInput)
                                if (crawlerName != null) {
                                    viewModel.fetchNovel(crawlerName, urlInput) { name, url ->
                                        urlInput = ""
                                        onNovelClick(name, url)
                                    }
                                }
                            },
                            enabled = !isFetching,
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = PrimaryAccent,
                                contentColor = Color.Black,
                                disabledContainerColor = PrimaryAccent.copy(alpha = 0.7f),
                                disabledContentColor = Color.Black
                            )
                        ) {
                            if (isFetching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.Black
                                )
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Fetch",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    if (error != null) {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Enter a direct URL from a supported source (e.g. NovelBin) to fetch chapters and prepare for export.",
                        modifier = Modifier.padding(horizontal = 10.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                        color = SecondaryText
                    )
                }
            }

            item { HorizontalDivider(color = BorderColor) }

            // Request History Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.History,
                            contentDescription = null,
                            tint = SecondaryText,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = "Request History",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = PrimaryText
                        )
                    }

                    OutlinedIconButton(
                        onClick = {  },
                        border = BorderStroke(1.dp, BorderColor),
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            containerColor = DarkSurface,
                            contentColor = PrimaryText
                        )
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            items(history, key = { it.id }) { record ->
                val progress = progressMap[record.id.toLong()]
                val isFinished = record.status == ExportStatus.SUCCESS
                val displayProgress = if (progress != null) {
                    progress.progress
                } else if (isFinished) {
                    1f
                } else {
                    0f
                }

                RequestHistoryCard(
                    title = record.novelTitle,
                    status = record.status,
                    timestamp = record.timestamp,
                    progress = displayProgress,
                    errorLog = record.errorLog,
                    onClick = { onHistoryClick(record.id) },
                    onReplay = { 
                        viewModel.fetchNovel(record.crawlerName, record.novelUrl) { _, _ ->
                            Toast.makeText(context, "Refresh started", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRemove = { pendingDelete = record }
                )
            }
        }

        pendingDelete?.let { record ->
            val displayName = record.novelTitle.ifBlank { record.novelUrl }
            ConfirmDeleteDialog(
                title = "Delete request?",
                message = "This will permanently remove \"$displayName\" from your request history. This action cannot be undone.",
                onConfirm = {
                    viewModel.deleteHistoryRecord(record.id, record.novelUrl)
                    pendingDelete = null
                },
                onDismiss = { pendingDelete = null }
            )
        }
    }
}

@Composable
fun RequestHistoryCard(
    title: String,
    status: ExportStatus,
    timestamp: Long,
    progress: Float = 0f,
    errorLog: String? = null,
    onClick: () -> Unit = {},
    onReplay: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                ),
                color = PrimaryText,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(status)
                MetadataChip("EPUB")
                if (errorLog != null) {
                    MetadataChip("Error Info", Color.Red.copy(alpha = 0.2f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = SecondaryText
                )

                Spacer(Modifier.width(4.dp))

                Text(
                    text = dateFormatter.format(Date(timestamp)),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = SecondaryText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = PrimaryAccent,
                    trackColor = BorderColor
                )
                
                Spacer(modifier = Modifier.width(10.dp))

                Text (
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText
                )

                if (status == ExportStatus.SUCCESS) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                }
            }

            if (onReplay != null || onRemove != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onRemove != null) {
                        TextButton(onClick = onRemove) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Red.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove", color = Color.Red.copy(alpha = 0.7f), fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    if (onReplay != null) {
                        OutlinedButton(
                            onClick = onReplay,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, BorderColor),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryText)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Replay", color = PrimaryText, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: ExportStatus) {
    val backgroundColor = when (status) {
        ExportStatus.SUCCESS -> Color(0xFF4CAF50).copy(alpha = 0.2f)
        ExportStatus.FAILED -> Color(0xFFF44336).copy(alpha = 0.2f)
        ExportStatus.PENDING -> Color(0xFF2196F3).copy(alpha = 0.2f)
        ExportStatus.CANCELLED -> Color(0xFF9E9E9E).copy(alpha = 0.2f)
    }
    val textColor = when (status) {
        ExportStatus.SUCCESS -> Color(0xFF81C784)
        ExportStatus.FAILED -> Color(0xFFE57373)
        ExportStatus.PENDING -> Color(0xFF64B5F6)
        ExportStatus.CANCELLED -> Color(0xFFBDBDBD)
    }

    Surface(
        color = backgroundColor,
        shape = CircleShape,
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}

@Composable
fun MetadataChip(text: String, color: Color = DarkBackground) {
    Surface(
        color = color,
        shape = CircleShape,
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = SecondaryText
        )
    }
}
