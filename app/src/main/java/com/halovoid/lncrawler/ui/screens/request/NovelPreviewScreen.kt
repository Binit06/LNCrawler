package com.halovoid.lncrawler.ui.screens.request

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.tooling.preview.Preview
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.ui.theme.*

@Composable
fun NovelPreviewScreen(
    viewModel: RequestViewModel,
    onBack: () -> Unit,
    onConfirm: (Novel) -> Unit,
    onCrawlManually: (String, String) -> Unit
) {
    val novel by viewModel.novelPreview.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val previewUrl by viewModel.previewUrl.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(previewUrl) {
        if (previewUrl != null && (novel == null || novel?.chapters?.isEmpty() == true)) {
            viewModel.fetchNovelPreview(previewUrl!!)
        }
    }

    NovelPreviewContent(
        novel = novel,
        isLoading = isLoading,
        error = error,
        onBack = onBack,
        onConfirm = onConfirm,
        onCrawlManually = {
            if (previewUrl != null) {
                onCrawlManually(novel?.crawlerName ?: "", previewUrl!!)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelPreviewContent(
    novel: Novel?,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onConfirm: (Novel) -> Unit,
    onCrawlManually: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                    }
                },
                actions = {
                    if (novel != null) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, novel.url.toUri())
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Language, contentDescription = "View Source", tint = PrimaryText)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading && novel == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    CircularProgressIndicator(color = BrandAccent)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Fetching novel details...", color = SecondaryText)
                    Spacer(modifier = Modifier.height(32.dp))
                    TextButton(onClick = onBack) {
                        Text("Cancel", color = SecondaryText)
                    }
                }
            } else if (novel != null) {
                var isSynopsisExpanded by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = BrandAccent,
                            trackColor = Color.Transparent
                        )
                    }

                    // Atmospheric Hero Section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Blurred/Atmospheric backdrop wash
                        AsyncImage(
                            model = novel.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .matchParentSize()
                                .alpha(0.12f),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            DarkBackground.copy(alpha = 0.7f),
                                            DarkBackground
                                        )
                                    )
                                )
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(top = 96.dp)
                                .padding(bottom = 32.dp)
                        ) {
                            // Centered Cover
                            AsyncImage(
                                model = novel.coverUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .width(170.dp)
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurface),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = novel.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = novel.author ?: "Unknown Author",
                                style = MaterialTheme.typography.bodyLarge,
                                color = SecondaryText,
                                textAlign = TextAlign.Center
                            )

                            if (novel.crawlerName.isNotBlank()) {
                                Surface(
                                    modifier = Modifier.padding(top = 12.dp),
                                    color = DarkSurfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = novel.crawlerName.uppercase(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SecondaryText,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Content Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 32.dp)
                    ) {
                        if (error != null) {
                             Surface(
                                 modifier = Modifier.padding(bottom = 24.dp),
                                 color = ErrorRed.copy(alpha = 0.1f),
                                 shape = RoundedCornerShape(8.dp),
                                 border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                             ) {
                                 Text(
                                     text = error,
                                     modifier = Modifier.padding(12.dp),
                                     color = ErrorRed,
                                     style = MaterialTheme.typography.bodySmall
                                 )
                             }
                        }

                        Text(
                            text = "Synopsis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = novel.description ?: "No description available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText,
                            lineHeight = 24.sp,
                            maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 5,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.animateContentSize()
                        )
                        
                        val description = novel.description
                        if (!description.isNullOrBlank() && description.length > 200) {
                            TextButton(
                                onClick = { isSynopsisExpanded = !isSynopsisExpanded },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = if (isSynopsisExpanded) "Show Less" else "Read More",
                                    color = BrandAccent,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        if (novel.chapters.isNotEmpty()) {
                            Button(
                                onClick = { onConfirm(novel) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandAccent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Add to Library",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            TextButton(
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Maybe Later",
                                    color = SecondaryText,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        } else if (!isLoading) {
                            Button(
                                onClick = onCrawlManually,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkSurface,
                                    contentColor = PrimaryText
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Crawl Manually",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            
                            TextButton(
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Maybe Later",
                                    color = SecondaryText,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            } else if (error != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text(error, color = ErrorRed, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) { Text("Go Back") }
                }
            } else {
                Text("No preview available", color = SecondaryText)
            }
        }

    }
}

@Preview(name = "Short Title", showBackground = true)
@Composable
private fun PreviewShortTitle() {
    LNCrawlerTheme {
        NovelPreviewContent(
            novel = Novel(
                url = "https://example.com",
                title = "Short Title",
                author = "Author Name",
                crawlerName = "Test Crawler",
                description = "This is a short synopsis."
            ),
            isLoading = false,
            error = null,
            onBack = {},
            onConfirm = { _ -> },
            onCrawlManually = {}
        )
    }
}

@Preview(name = "Long Title", showBackground = true)
@Composable
private fun PreviewLongTitle() {
    LNCrawlerTheme {
        NovelPreviewContent(
            novel = Novel(
                url = "https://example.com",
                title = "A Very Long Novel Title That Should Wrap to Multiple Lines To Test Vertical Layout Reflow Correctly",
                author = "Some Prolific Author",
                crawlerName = "Generic Source",
                description = "A long synopsis ".repeat(20)
            ),
            isLoading = false,
            error = null,
            onBack = {},
            onConfirm = { _ -> },
            onCrawlManually = {}
        )
    }
}

@Preview(name = "Missing Author and Badge", showBackground = true)
@Composable
private fun PreviewMissingMetadata() {
    LNCrawlerTheme {
        NovelPreviewContent(
            novel = Novel(
                url = "https://example.com",
                title = "Mysterious Novel",
                author = null,
                crawlerName = "",
                description = "No author, no badge."
            ),
            isLoading = false,
            error = null,
            onBack = {},
            onConfirm = { _ -> },
            onCrawlManually = {}
        )
    }
}
