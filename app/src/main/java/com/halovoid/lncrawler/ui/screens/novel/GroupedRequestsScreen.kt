package com.halovoid.lncrawler.ui.screens.novel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.ui.components.*
import com.halovoid.lncrawler.ui.theme.BrandAccent
import com.halovoid.lncrawler.ui.theme.DarkBackground
import com.halovoid.lncrawler.ui.theme.PrimaryAccent
import com.halovoid.lncrawler.ui.theme.PrimaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupedRequestsScreen(
    type: RequestType,
    requests: List<Request>,
    statusFilter: RequestStatus?,
    onFilterChange: (RequestStatus?) -> Unit,
    onBack: () -> Unit,
    onRequestClick: (String) -> Unit,
    onReplay: (String) -> Unit = {},
    onCancel: (String) -> Unit = {},
    onContinue: (String) -> Unit = {},
    onResolveCloudflare: (String, String) -> Unit = { _, _ -> },
    cancellingRequestIds: Set<String> = emptySet(),
    activeActionIds: Set<String> = emptySet(),
    allowAction: Boolean = false
) {
    val filteredRequests = requests.filter { it.type == type }
    var showFilterMenu by remember { mutableStateOf(false) }

    RequestActionHandler(
        onResolveCloudflare = onResolveCloudflare
    ) { onSecurityClick ->
        val title = when (type) {
            RequestType.NOVEL_METADATA -> "Metadata"
            RequestType.CHAPTER -> "Chapter Downloads"
            RequestType.ARTIFACT -> "Exports"
            RequestType.RANGE_DOWNLOAD -> "Downloads"
        }

        Scaffold(
            containerColor = DarkBackground,
            topBar = {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold, color = PrimaryText) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = if (statusFilter != null) BrandAccent else PrimaryAccent
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                requestHistorySection(
                    requestHistory = filteredRequests,
                    onRequestClick = onRequestClick,
                    onGroupClick = {},
                    onReplay = onReplay,
                    onCancel = onCancel,
                    onContinue = onContinue,
                    onSecurityClick = onSecurityClick,
                    cancellingRequestIds = cancellingRequestIds,
                    activeActionIds = activeActionIds,
                    allowAction = allowAction,
                    forceUngrouped = true
                )
            }
        }
    }

    if (showFilterMenu) {
        val filterOptions = listOf(
            null to "All Status",
            RequestStatus.SUCCESS to "Success",
            RequestStatus.FAILED to "Failed",
            RequestStatus.CANCELLED to "Cancelled"
        )

        AppBottomSheet(
            onDismiss = { showFilterMenu = false },
            title = "Filter Status"
        ) {
            AppBottomSheetGroup {
                filterOptions.forEachIndexed { index, (status, label) ->
                    ListItem(
                        headlineContent = {
                            Text(
                                label,
                                color = PrimaryText,
                                fontWeight = FontWeight.Normal
                            )
                        },
                        trailingContent = {
                            if (statusFilter == status) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = BrandAccent)
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            onFilterChange(status)
                            showFilterMenu = false
                        }
                    )
                    if (index < filterOptions.lastIndex) {
                        AppBottomSheetDivider()
                    }
                }
            }
        }
    }
}
