package com.halovoid.lncrawler.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.lncrawler.ui.theme.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    novelUrl: String,
    initialChapterId: Int,
    onBack: () -> Unit,
    viewModel: ReaderViewModel
) {
    val window by viewModel.window.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    
    var isControlsVisible by remember { mutableStateOf(true) }
    var hasScrolledToInitial by remember { mutableStateOf(false) }

    LaunchedEffect(novelUrl, initialChapterId) {
        viewModel.start(novelUrl, initialChapterId)
    }

    // Initial scroll to the selected chapter
    LaunchedEffect(window) {
        if (window.isNotEmpty() && !hasScrolledToInitial) {
            val index = window.indexOfFirst { it.chapter.id == initialChapterId }
            if (index != -1) {
                listState.scrollToItem(index)
                hasScrolledToInitial = true
            }
        }
    }

    // Track visible items to update the center chapter
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .mapNotNull { visibleItems ->
                if (visibleItems.isEmpty()) return@mapNotNull null
                val viewportCenter = (listState.layoutInfo.viewportEndOffset + listState.layoutInfo.viewportStartOffset) / 2
                val centerItem = visibleItems.minByOrNull { 
                    val itemCenter = it.offset + it.size / 2
                    kotlin.math.abs(itemCenter - viewportCenter)
                }
                centerItem?.key as? Int
            }
            .distinctUntilChanged()
            .collect { chapterId ->
                viewModel.onCenterChapterChanged(chapterId)
            }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            androidx.compose.animation.AnimatedVisibility(
                visible = isControlsVisible,
                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = window.firstOrNull()?.chapter?.title ?: "Reader",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryText
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground.copy(alpha = 0.9f))
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (isControlsVisible) innerPadding.calculateTopPadding() else 0.dp)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    isControlsVisible = !isControlsVisible
                }
        ) {
            if (isLoading && window.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryAccent
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, 
                        end = 16.dp, 
                        top = 32.dp, 
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(window, key = { it.chapter.id }) { loadedChapter ->
                        ChapterContent(loadedChapter)
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterContent(loadedChapter: LoadedChapter) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Chapter Started Indicator
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            color = Color.Transparent
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HorizontalDivider(
                    modifier = Modifier.width(60.dp),
                    color = PrimaryAccent.copy(alpha = 0.3f),
                    thickness = 2.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${loadedChapter.chapter.title} started",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryAccent.copy(alpha = 0.7f),
                    letterSpacing = 2.sp
                )
            }
        }

        loadedChapter.paragraph.forEach { para ->
            Text(
                text = para,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 34.sp,
                    letterSpacing = 0.5.sp,
                    fontSize = 19.sp
                ),
                color = PrimaryText.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 28.dp)
            )
        }
        
        // Chapter Ended Indicator
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            color = Color.Transparent
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${loadedChapter.chapter.title} ended",
                    style = MaterialTheme.typography.labelMedium,
                    color = SecondaryText.copy(alpha = 0.5f),
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    modifier = Modifier.width(60.dp),
                    color = SecondaryText.copy(alpha = 0.2f),
                    thickness = 2.dp
                )
            }
        }
    }
}
