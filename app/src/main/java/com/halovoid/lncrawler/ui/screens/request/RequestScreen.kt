package com.halovoid.lncrawler.ui.screens.request

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.lncrawler.domain.models.SearchItem
import com.halovoid.lncrawler.ui.components.RequestActionHandler
import com.halovoid.lncrawler.ui.components.ScreenHeader
import com.halovoid.lncrawler.ui.screens.search.SearchState
import com.halovoid.lncrawler.ui.screens.search.SearchViewModel
import com.halovoid.lncrawler.ui.theme.*
import java.util.Locale

enum class RequestTab {
    SEARCH, REQUEST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestScreen(
    onNavigateToPreview: () -> Unit,
    onCrawlerClick: () -> Unit,
    viewModel: RequestViewModel,
    searchViewModel: SearchViewModel,
    searchUrl: String? = null
) {
    var selectedTab by remember { mutableStateOf(RequestTab.SEARCH) }
    
    val novelPreview by viewModel.novelPreview.collectAsStateWithLifecycle()
    var isFetchingPreview by remember { mutableStateOf(false) }

    LaunchedEffect(novelPreview) {
        if (novelPreview != null && isFetchingPreview) {
            isFetchingPreview = false
            onNavigateToPreview()
        }
    }

    RequestActionHandler(
        onResolveCloudflare = { id, url -> viewModel.resolveCloudflare(id, url) }
    ) {
        Scaffold(
            containerColor = DarkBackground
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                ScreenHeader(
                    title = "Browse",
                    actions = {
                        IconButton(onClick = onCrawlerClick) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Crawlers",
                                tint = PrimaryAccent
                            )
                        }
                    }
                )

                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = PrimaryAccent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = PrimaryAccent,
                            height = 2.dp
                        )
                    },
                    divider = {},
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .height(48.dp)
                ) {
                    Tab(
                        selected = selectedTab == RequestTab.SEARCH,
                        onClick = { selectedTab = RequestTab.SEARCH },
                        text = { Text("Search", style = MaterialTheme.typography.titleSmall, fontWeight = if (selectedTab == RequestTab.SEARCH) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == RequestTab.REQUEST,
                        onClick = { selectedTab = RequestTab.REQUEST },
                        text = { Text("Request", style = MaterialTheme.typography.titleSmall, fontWeight = if (selectedTab == RequestTab.REQUEST) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        RequestTab.SEARCH -> {
                            SearchTabContent(
                                viewModel = searchViewModel,
                                requestViewModel = viewModel,
                                onStartFetching = { isFetchingPreview = true }
                            )
                        }
                        RequestTab.REQUEST -> {
                            ManualRequestContent(
                                viewModel = viewModel,
                                searchUrl = searchUrl,
                                onStartFetching = { isFetchingPreview = true }
                            )
                        }
                    }
// ...

                    if (isFetchingPreview) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable(enabled = false) {},
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = PrimaryAccent)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Fetching details...", color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CenteredInfo(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Info) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = SecondaryText.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun SearchTabContent(
    viewModel: SearchViewModel,
    requestViewModel: RequestViewModel,
    onStartFetching: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        val isIdle = searchState is SearchState.Idle
        
        if (isIdle) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Search the index",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Find novels across supported sources. Results are served from our index, not fetched directly from the source websites.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(24.dp))
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Hero Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(DarkSurface)
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search for novels...", color = SecondaryText, fontSize = 16.sp) },
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
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge
            )

            FilledIconButton(
                onClick = { if (searchQuery.isNotBlank()) viewModel.search(searchQuery) },
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

        if (isIdle) {
            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = SecondaryText.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "The index is still growing. Some novels may not be available yet. If you do not find your favourite novel create a request for it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText.copy(alpha = 0.6f),
                    lineHeight = 18.sp
                )
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (val state = searchState) {
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
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    }
                                    items(items, key = { it.url }) { item ->
                                        SearchResultItem(
                                            item = item,
                                            onClick = { 
                                                onStartFetching()
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
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun ManualRequestContent(
    viewModel: RequestViewModel,
    searchUrl: String?,
    onStartFetching: () -> Unit
) {
    var urlInput by remember { mutableStateOf(searchUrl ?: "") }
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp)
    ) {
        item {
            Text(
                text = "Request a Novel",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Can't find what you're looking for? Submit the novel's URL and we'll add it to the indexing queue.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter novel page URL", color = SecondaryText, fontSize = 14.sp) },
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

                IconButton(
                    onClick = {
                        if (!isLoading) {
                            val crawlerName = viewModel.validateUrl(urlInput)
                            if (crawlerName != null) {
                                onStartFetching()
                                viewModel.fetchNovelPreview(urlInput)
                            }
                        }
                    },
                    enabled = !isLoading && urlInput.isNotBlank(),
                    modifier = Modifier.size(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PrimaryAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Submit",
                            tint = if (urlInput.isNotBlank()) PrimaryAccent else SecondaryText.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (error != null) {
                Text(
                    text = error ?: "",
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Text(
                text = "How requesting works",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            StepItem(
                number = "01",
                title = "Submit a novel URL",
                description = "Enter the novel's page URL with supported source. You can continue on with adding the novel to your library"
            )
            Spacer(modifier = Modifier.height(20.dp))
            StepItem(
                number = "02",
                title = "We process the request",
                description = "Your request is queued and processed by our server. We take a note if the url can be indexed and if it is possible we index it as soon as possible"
            )
            Spacer(modifier = Modifier.height(20.dp))
            StepItem(
                number = "03",
                title = "It gets indexed",
                description = "Once indexed, it becomes available through Search. Now someone who someday needs to look for the same novel will have easy access to the novel."
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SourceBadge(name: String) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.3f))
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = PrimaryText.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun StepItem(number: String, title: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = number,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = PrimaryAccent.copy(alpha = 0.2f),
            fontSize = 28.sp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText,
                lineHeight = 18.sp
            )
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
            .padding(vertical = 8.dp)
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

                IconButton(onClick = onBrowserClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open in Browser",
                        tint = SecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
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
