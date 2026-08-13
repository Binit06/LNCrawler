package com.halovoid.lncrawler.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RequestCard(
    request: Request,
    onClick: () -> Unit,
    onReplay: () -> Unit,
    onCancel: () -> Unit,
    onSecurityClick: (() -> Unit)? = null,
    allowReplay : Boolean? = true,
    allowCancel : Boolean? = true,
    showProgress: Boolean? = true,
    isCancelling: Boolean = false,
) {
    val isWorkFinished = (request.progressSuccess + request.progressFailed + request.progressCancelled) == request.progressTotal

    val isProcessing = request.status == RequestStatus.RUNNING ||
            request.status == RequestStatus.PENDING

    val displayStatus = if (isProcessing || (request.status == RequestStatus.SUCCESS && !isWorkFinished)) {
        RequestStatus.RUNNING
    } else {
        request.status
    }
    val locale = LocalConfiguration.current.locales[0]

    val formattedDate = remember(request.createdAt, locale) {
        SimpleDateFormat("MMM dd, HH:mm", locale).format(Date(request.createdAt))
    }

    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog) {
        ConfirmCancelDialog(
            title = "Cancel Request?",
            message = "Are you sure you want to stop \"${request.name}\"? Any progress made will be preserved, but the current task will stop.",
            onConfirm = {
                showCancelDialog = false
                onCancel()
            },
            onDismiss = { showCancelDialog = false }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCancelling) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text(request.type.name, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 10.sp)
                        }
                        if (request.priority > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FlashOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = SuccessGreen
                                )
                                Text(
                                    "High Priority",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SuccessGreen,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (request.status == RequestStatus.BLOCKED && onSecurityClick != null) {
                        IconButton(
                            onClick = onSecurityClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = "Security Check Needed",
                                tint = PrimaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    StatusIcon(displayStatus)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Section
            val progress = if (request.progressTotal > 0) {
                (request.progressSuccess).toFloat() / request.progressTotal
            } else 0f

            Column {
                ProgressIndicator(
                    success = request.progressSuccess,
                    failed = request.progressFailed,
                    cancelled = request.progressCancelled,
                    total = request.progressTotal,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}% completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCancelling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = ErrorRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Cancelling...",
                        style = MaterialTheme.typography.labelSmall,
                        color = ErrorRed
                    )
                } else if (isProcessing) {
                    TextButton(
                        onClick = { showCancelDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel")
                    }
                } else {
                    TextButton(
                        onClick = onReplay,
                        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryAccent)
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Replay")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIcon(status: RequestStatus) {
    when (status) {
        RequestStatus.SUCCESS -> Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = SuccessGreen)
        RequestStatus.FAILED -> Icon(Icons.Default.Error, contentDescription = "Failed", tint = ErrorRed)
        RequestStatus.CANCELLED -> Icon(Icons.Default.Cancel, contentDescription = "Cancelled", tint = SecondaryText)
        RequestStatus.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = PrimaryAccent)
        RequestStatus.BLOCKED -> Icon(Icons.Default.Security, contentDescription = "Blocked", tint = PrimaryAccent)
        else -> Icon(Icons.Default.Schedule, contentDescription = "Pending", tint = SecondaryText)
    }
}
