package com.halovoid.lncrawler.ui.screens.request

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
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
import com.halovoid.lncrawler.domain.models.ExportStatus
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.ui.ViewModelFactory
import com.halovoid.lncrawler.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri

/**
 * Detailed view for a specific export request record.
 * Displays request status and novel metadata in an inspection-style layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    recordId: Int,
    onBackClick: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val factory = remember { ViewModelFactory(context.applicationContext as android.app.Application) }
    val viewModel: RequestDetailViewModel = viewModel(factory = factory)

    val record by viewModel.record.collectAsState()
    val novel by viewModel.novel.collectAsState()
    val progressMap by viewModel.exportProgressMap.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/epub+zip")
    ) { uri ->
        uri?.let {
            viewModel.rerequestExport(it)
            Toast.makeText(context, "Re-request started", Toast.LENGTH_SHORT).show()
        }
    }

    fun remove() {
        if (record?.id === null || novel?.url === null) {
            Toast.makeText(context, "Failed to Remove Request", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.deleteHistoryRecord(record!!.id, novel!!.url)
        Toast.makeText(context, "Removed request", Toast.LENGTH_SHORT).show()
        onBackClick()
    }

    LaunchedEffect(recordId) {
        viewModel.loadRecord(recordId)
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PrimaryAccent
                    )
                }
                TextButton(onClick = onBackClick) {
                    Text("All Requests", color = PrimaryAccent, fontSize = 16.sp)
                }
            }
        }
    ) { innerPadding ->
        if (record == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryAccent)
            }
        } else {
            val currentRecord = record!!
            val progress = progressMap[currentRecord.id.toLong()]
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Request Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = currentRecord.novelUrl,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 26.sp
                            ),
                            color = PrimaryText,
                            maxLines = 3,
                            overflow = TextOverflow.Clip
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusChipDetail(currentRecord.status)
                            MetadataChipDetail("Novel")
                            MetadataChipDetail("Normal")
                            MetadataChipDetail("EPUB")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = SecondaryText)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Requested: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(currentRecord.timestamp))}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = SecondaryText
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val displayProgress = progress?.progress ?: if (currentRecord.status == ExportStatus.SUCCESS) 1f else 0f
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { displayProgress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = PrimaryAccent,
                                trackColor = BorderColor
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            if (progress?.progress !== null) {
                                Text (
                                    text = "${(progress.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecondaryText
                                )
                            }
                            if (currentRecord.status == ExportStatus.SUCCESS) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Timeline / Events (Simplified as Chips)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EventChip(Icons.Default.PlayArrow, "Started", currentRecord.timestamp)
                            if (currentRecord.status == ExportStatus.SUCCESS) {
                                EventChip(Icons.Default.Check, "Completed", currentRecord.timestamp + 5000)
                            } else if (currentRecord.status == ExportStatus.FAILED) {
                                EventChip(Icons.Default.Error, "Failed", currentRecord.timestamp + 2000)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            if (progress != null) {
                                TextButton(onClick = { viewModel.cancelExport() }) {
                                    Text("Cancel", color = Color.Red)
                                }
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedIconButton(
                                        onClick = {
                                            remove()
                                        },
                                        border = BorderStroke(1.dp, BorderColor),
                                        colors = IconButtonDefaults.outlinedIconButtonColors(
                                            containerColor = DarkSurface,
                                            contentColor = Color.Red.copy(alpha = 0.8f)
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete"
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val fileName =
                                                "${currentRecord.novelTitle.filter { it.isLetterOrDigit() }}.epub"
                                            exportLauncher.launch(fileName)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, BorderColor),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = DarkSurface
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = PrimaryText
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Replay", color = PrimaryText)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Novel Information Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Source Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = SecondaryText, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentRecord.novelUrl.toUri().host ?: "Unknown Source",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                                color = SecondaryText
                            )
                        }
                        
                        TextButton(
                            onClick = { onOpenUrl(currentRecord.novelUrl) }
                        ) {
                            Text(
                                text = novel?.title ?: currentRecord.novelTitle,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                ),
                                color = PrimaryAccent,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 12.dp))

                        // Cover Image
                        AsyncImage(
                            model = novel?.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .aspectRatio(2f / 3f)
                                .align(Alignment.CenterHorizontally)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Metadata Table
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            MetadataRow("Authors", novel?.author ?: "Unknown")
                            MetadataRow("Genres", "Unknown") // We could add genres to the model
                            MetadataRow("Chapters", novel?.chapters?.size?.toString() ?: "Unknown")
                            MetadataRow("Status", "Unknown")
                            MetadataRow("Created", SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(currentRecord.timestamp)))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Synopsis
                        Text(
                            text = "Synopsis",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            ),
                            color = PrimaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = novel?.description ?: "No synopsis available.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            ),
                            color = PrimaryText
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(0.4f),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
            color = SecondaryText
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            color = PrimaryText
        )
    }
    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
}

@Composable
fun EventChip(icon: androidx.compose.ui.graphics.vector.ImageVector, name: String, timestamp: Long) {
    Surface(
        color = BorderColor.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = SecondaryText)
            Spacer(modifier = Modifier.width(4.dp))
            Text(name, fontSize = 10.sp, color = SecondaryText, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp)),
                fontSize = 10.sp,
                color = SecondaryText
            )
        }
    }
}

@Composable
fun StatusChipDetail(status: ExportStatus) {
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
fun MetadataChipDetail(text: String) {
    Surface(
        color = BorderColor.copy(alpha = 0.2f),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}
