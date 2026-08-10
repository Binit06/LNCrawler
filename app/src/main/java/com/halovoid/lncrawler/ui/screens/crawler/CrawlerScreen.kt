package com.halovoid.lncrawler.ui.screens.crawler

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.lncrawler.api.core.crawler.Crawler
import com.halovoid.lncrawler.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrawlerScreen(
    viewModel: CrawlerViewModel
) {
    val crawlers by viewModel.crawlers.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val isUpdateAvailable by viewModel.isUpdateAvailable.collectAsStateWithLifecycle()
    val showSyncOption by viewModel.showSyncOption.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncState) {
        when (syncState) {
            is SyncState.Success -> {
                snackbarHostState.showSnackbar((syncState as SyncState.Success).message)
                viewModel.resetSyncState()
            }
            is SyncState.Error -> {
                snackbarHostState.showSnackbar((syncState as SyncState.Error).error)
                viewModel.resetSyncState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Crawlers", fontWeight = FontWeight.Bold, color = PrimaryText) },
                actions = {
                    if (syncState is SyncState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PrimaryAccent,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    } else if (showSyncOption) {
                        IconButton(onClick = { viewModel.syncCrawlers() }) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync Crawlers", tint = PrimaryAccent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isUpdateAvailable) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryAccent.copy(alpha = 0.1f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Update Available",
                                style = MaterialTheme.typography.labelLarge,
                                color = PrimaryAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "New crawler versions are ready to download.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
                            )
                        }
                        Button(
                            onClick = { viewModel.syncCrawlers() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Update Now", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (crawlers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No crawlers available. Please sync.", color = SecondaryText)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(crawlers, key = { it.baseUrl }) { crawler ->
                        CrawlerItem(crawler)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrawlerItem(crawler: Crawler) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = crawler.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
                
                Badge(
                    containerColor = PrimaryAccent.copy(alpha = 0.1f),
                    contentColor = PrimaryAccent
                ) {
                    Text(
                        text = "Active",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    tint = SecondaryText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = crawler.baseUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = null,
                    tint = SecondaryText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Language: ${crawler.language.uppercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
        }
    }
}
