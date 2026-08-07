package com.halovoid.lncrawler.ui.screens.request

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
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
import com.halovoid.lncrawler.ui.theme.*

/**
 * Hub screen for creating novel requests and viewing export history.
 * Follows a developer-tool aesthetic with high information density.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestScreen(
    onHistoryClick: (Int) -> Unit,
    onLibraryClick: () -> Unit,
    viewModel: RequestViewModel,
    onFetchComplete: (requestId: Int) -> Unit
) {
    var urlInput by remember { mutableStateOf("") }
    val error by viewModel.error.collectAsState()
    val activeFetches by viewModel.activeFetches.collectAsState()
    val requestHistory by viewModel.requestHistory.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isFetching = !activeFetches.isEmpty()

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
            // Request Creation Section
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
                                    viewModel.fetchMetadata(crawlerName, urlInput)
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
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        color = SecondaryText
                    )
                }
            }
        }
    }
}