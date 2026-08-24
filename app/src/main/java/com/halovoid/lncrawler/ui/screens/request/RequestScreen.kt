package com.halovoid.lncrawler.ui.screens.request

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.halovoid.lncrawler.domain.models.SearchItem
import com.halovoid.lncrawler.ui.components.RequestActionHandler
import com.halovoid.lncrawler.ui.components.ScreenHeader
import com.halovoid.lncrawler.ui.screens.search.SearchState
import com.halovoid.lncrawler.ui.screens.search.SearchViewModel
import com.halovoid.lncrawler.domain.models.Novel
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
                                tint = PrimaryText // Neutral icon for secondary action
                            )
                        }
                    }
                )

                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = BrandAccent, // Vibrant BrandAccent for tabs
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = BrandAccent,
                            height = 3.dp // Slightly thicker indicator for higher contrast
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
                        text = { 
                            Text(
                                "Search", 
                                style = MaterialTheme.typography.titleSmall, 
                                fontWeight = if (selectedTab == RequestTab.SEARCH) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == RequestTab.SEARCH) PrimaryText else SecondaryText
                            ) 
                        }
                    )
                    Tab(
                        selected = selectedTab == RequestTab.REQUEST,
                        onClick = { selectedTab = RequestTab.REQUEST },
                        text = { 
                            Text(
                                "Request", 
                                style = MaterialTheme.typography.titleSmall, 
                                fontWeight = if (selectedTab == RequestTab.REQUEST) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == RequestTab.REQUEST) PrimaryText else SecondaryText
                            ) 
                        }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        RequestTab.SEARCH -> {
                            SearchTabContent(
                                viewModel = searchViewModel,
                                requestViewModel = viewModel,
                                onNavigateToPreview = onNavigateToPreview
                            )
                        }
                        RequestTab.REQUEST -> {
                            ManualRequestContent(
                                viewModel = viewModel,
                                searchUrl = searchUrl,
                                onNavigateToPreview = onNavigateToPreview
                            )
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
    onNavigateToPreview: () -> Unit
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
                    cursorColor = BrandAccent,
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
                    containerColor = BrandAccent,
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
                                contentPadding = PaddingValues(bottom = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                state.response.results.forEach { (source, items) ->
                                    item {
                                        Column {
                                            SourceHeader(source = source, count = items.size)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                contentPadding = PaddingValues(bottom = 8.dp)
                                            ) {
                                                items(items, key = { it.url }) { item ->
                                                    SearchResultCard(
                                                        item = item,
                                                        onClick = {
                                                            requestViewModel.setPreviewUrl(item.url)
                                                            requestViewModel.setPreviewNovel(
                                                                Novel(
                                                                    url = item.url,
                                                                    title = item.title,
                                                                    description = item.description,
                                                                    coverUrl = item.imageUrl,
                                                                    crawlerName = item.source
                                                                )
                                                            )
                                                            onNavigateToPreview()
                                                        }
                                                    )
                                                }
                                            }
                                        }
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
    onNavigateToPreview: () -> Unit
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
                                    viewModel.setPreviewUrl(urlInput)
                                    viewModel.setPreviewNovel(null) // We don't have metadata yet
                                    onNavigateToPreview()
                                }
                        }
                    },
                    enabled = !isLoading && urlInput.isNotBlank(),
                    modifier = Modifier.size(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = BrandAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Submit",
                            tint = if (urlInput.isNotBlank()) BrandAccent else SecondaryText.copy(alpha = 0.5f),
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
            color = BrandAccent.copy(alpha = 0.3f), // Using BrandAccent for step numbers
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
fun SourceHeader(source: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = source,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )
        Text(
            text = "$count results",
            style = MaterialTheme.typography.labelSmall,
            color = SecondaryText
        )
    }
}

@Composable
fun SearchResultCard(
    item: SearchItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(130.dp) // Smaller width for horizontal scroll scannability
            .clickable { onClick() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = null
                )
                
                if (item.imageUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = null,
                            tint = SecondaryText.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium, // Smaller font for smaller card
            fontWeight = FontWeight.Bold,
            color = PrimaryText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
