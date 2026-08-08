package com.halovoid.lncrawler.ui.screens.request

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.ui.components.RequestCard
import com.halovoid.lncrawler.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestScreen(
    onRequestClick: (String) -> Unit,
    viewModel: RequestViewModel,
) {
    var urlInput by remember { mutableStateOf("") }
    val error by viewModel.error.collectAsState()
    val requestHistory by viewModel.requestHistory.collectAsStateWithLifecycle()

    var statusFilter by remember { mutableStateOf<RequestStatus?>(null) }
    var typeFilter by remember { mutableStateOf<RequestType?>(null) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { 
                    Text("Requests", fontWeight = FontWeight.Bold, color = PrimaryText) 
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Request Novel Section
            item {
                Column {
                    Text(
                        "Request Novel",
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
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
                            placeholder = { Text("Enter novel page URL", color = SecondaryText) },
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
                                    viewModel.startNovelCrawl(crawlerName, urlInput)
                                }
                                // TODO: Redirect to Request Detail Screen after sending the request
                            },
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = SuccessGreen,
                                contentColor = Color.White
                            )
                        ) {
                            // TODO: Implement a small animation when the user clicks that happens till the user is not redirected
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Submit",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (error != null) {
                        Text(
                            text = error ?: "",
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Supported sources: NovelBin, etc. Format: https://novelbins.com/b/novel-title",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Filters Section
            // TODO: Show the filters only if there are some requests available
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterDropdown("Status", RequestStatus.values().map { it.name }, { statusFilter = RequestStatus.valueOf(it) })
                    FilterDropdown("Type", RequestType.values().map { it.name }, { typeFilter = RequestType.valueOf(it) })
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    IconButton(onClick = { /* Refresh logic */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PrimaryAccent)
                    }
                }
            }

            // Request List
            val filteredHistory = requestHistory.filter { 
                (statusFilter == null || it.status == statusFilter) &&
                (typeFilter == null || it.type == typeFilter)
            }

            items(filteredHistory) { request ->
                RequestCard(
                    request = request,
                    onClick = { onRequestClick(request.id) },
                    onReplay = { viewModel.replayRequest(request.id) },
                    onCancel = { viewModel.cancelRequest(request.id) }
                )
            }
        }
    }
}

@Composable
fun FilterDropdown(label: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("Any") }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryText)
        ) {
            Text("$label: $selectedOption", fontSize = 12.sp)
            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkSurface)
        ) {
            DropdownMenuItem(
                text = { Text("Any", color = PrimaryText) },
                onClick = {
                    selectedOption = "Any"
                    expanded = false
                    // Handle "Any" logic
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = PrimaryText) },
                    onClick = {
                        selectedOption = option
                        expanded = false
                        onSelected(option)
                    }
                )
            }
        }
    }
}
