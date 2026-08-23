package com.halovoid.lncrawler.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.lncrawler.ui.screens.request.RequestViewModel
import com.halovoid.lncrawler.domain.models.SearchItem
import com.halovoid.lncrawler.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    requestViewModel: RequestViewModel,
    onBack: () -> Unit,
    onNavigateToPreview: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val novelPreview by requestViewModel.novelPreview.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var isFetchingPreview by remember { mutableStateOf(false) }

    LaunchedEffect(novelPreview) {
        if (novelPreview != null && isFetchingPreview) {
            isFetchingPreview = false
            onNavigateToPreview()
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Global Search", fontWeight = FontWeight.Bold, color = PrimaryText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
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
            // Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(DarkSurface)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search for novels...", color = SecondaryText) },
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
                    onClick = { viewModel.search(searchQuery) },
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = PrimaryAccent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }

            // Results
            Box(modifier = Modifier.weight(1f)) {
                when (val state = searchState) {
                    is SearchState.Idle -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Start searching for light novels", color = SecondaryText)
                        }
                    }
                    is SearchState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryAccent)
                        }
                    }
                    is SearchState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error: ${state.message}", color = ErrorRed, modifier = Modifier.padding(16.dp))
                        }
                    }
                    is SearchState.Success -> {
                        if (state.response.results.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No results found", color = SecondaryText)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                state.response.results.forEach { (source, items) ->
                                    item {
                                        Text(
                                            text = source,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryAccent,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                    items(items) { item ->
                                        SearchResultItem(
                                            item = item,
                                            onClick = { 
                                                isFetchingPreview = true
                                                requestViewModel.fetchNovelPreview(item.url)
                                            },
                                            onBrowserClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, item.url.toUri())
                                                context.startActivity(intent)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isFetchingPreview) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable(enabled = false) {},
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryAccent)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Fetching novel details...", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    item: SearchItem,
    onClick: () -> Unit,
    onBrowserClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row {
                    IconButton(onClick = onBrowserClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in Browser",
                            tint = SecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryAccent
                )
                Text(
                    text = "Score: ${String.format(Locale.US, "%.2f", item.score)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText
                )
            }
        }
    }
}
