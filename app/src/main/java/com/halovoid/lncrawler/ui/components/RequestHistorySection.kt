package com.halovoid.lncrawler.ui.components

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.ui.theme.DarkSurface
import com.halovoid.lncrawler.ui.theme.PrimaryAccent
import com.halovoid.lncrawler.ui.theme.PrimaryText
import com.halovoid.lncrawler.ui.theme.SecondaryText

/**
 * Extension for LazyListScope to provide a grouped request history section.
 */
fun LazyListScope.requestHistorySection(
    requestHistory: List<Request>,
    onRequestClick: (String) -> Unit,
    onGroupClick: (RequestType) -> Unit,
    onReplay: (String) -> Unit = {},
    onCancel: (String) -> Unit = {},
    onContinue: (String) -> Unit = {},
    onSecurityClick: (Request) -> Unit = {},
    cancellingRequestIds: Set<String> = emptySet(),
    activeActionIds: Set<String> = emptySet(),
    horizontalPadding: androidx.compose.ui.unit.Dp = 0.dp,
    allowAction: Boolean = false
) {
    if (requestHistory.isEmpty()) return

    val groupedRequests = requestHistory.groupBy { it.type }

    groupedRequests.forEach { (type, requests) ->
        if (requests.size > 1) {
            item(key = "group_$type") {
                Box(modifier = Modifier.padding(horizontal = horizontalPadding).padding(bottom = 12.dp)) {
                    RequestGroupCard(
                        type = type,
                        requests = requests,
                        onClick = { onGroupClick(type) }
                    )
                }
            }
        } else {
            items(requests, key = { it.id }) { request ->
                Box(modifier = Modifier.padding(horizontal = horizontalPadding).padding(bottom = 12.dp)) {
                    RequestCard(
                        request = request,
                        onClick = { onRequestClick(request.id) },
                        onReplay = { onReplay(request.id) },
                        onCancel = { onCancel(request.id) },
                        onContinue = { onContinue(request.id) },
                        onSecurityClick = { onSecurityClick(request) },
                        isCancelling = cancellingRequestIds.contains(request.id),
                        isActionPending = activeActionIds.contains(request.id),
                        allowAction = allowAction
                    )
                }
            }
        }
    }
}

@Composable
fun RequestGroupCard(
    type: RequestType,
    requests: List<Request>,
    onClick: () -> Unit
) {
    val totalSuccess = requests.sumOf { it.progressSuccess }
    val totalFailed = requests.sumOf { it.progressFailed }
    val totalCancelled = requests.sumOf { it.progressCancelled }
    val totalProgress = requests.sumOf { it.progressTotal }

    val typeName = when (type) {
        RequestType.NOVEL_METADATA -> "Metadata Refreshes"
        RequestType.CHAPTER -> "Single Chapters"
        RequestType.ARTIFACT -> "Exports"
        RequestType.RANGE_DOWNLOAD -> "Range Downloads"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                        text = typeName,
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
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(type.name, fontSize = 10.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${requests.size} requests",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = SecondaryText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Section
            val progress = if (totalProgress > 0) {
                totalSuccess.toFloat() / totalProgress
            } else 0f

            Column {
                ProgressIndicator(
                    success = totalSuccess,
                    failed = totalFailed,
                    cancelled = totalCancelled,
                    total = totalProgress,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}% average completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText
                    )
                }
            }
        }
    }
}
