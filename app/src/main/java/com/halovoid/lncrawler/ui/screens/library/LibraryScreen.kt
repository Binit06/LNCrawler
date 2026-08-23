package com.halovoid.lncrawler.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.lncrawler.ui.components.NovelCard
import com.halovoid.lncrawler.ui.components.ScreenHeader
import com.halovoid.lncrawler.ui.screens.request.RequestViewModel
import com.halovoid.lncrawler.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNovelClick: (String, String) -> Unit,
    onBackClick: () -> Unit, // Still keeping for navigation if needed
    viewModel: LibraryViewModel
) {
    val novels by viewModel.novels.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedDomain by remember { mutableStateOf("Any") }
    
    var isSearching by remember { mutableStateOf(false) }

    val filteredNovels by remember(novels, searchQuery, selectedDomain) {
        derivedStateOf {
            novels.filter {
                (selectedDomain == "Any" || it.crawlerName == selectedDomain) &&
                        it.title.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val domains = listOf("Any") + novels.map { it.crawlerName }.distinct()

    Scaffold(
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            ScreenHeader(
                title = "Novels",
                subtitle = if (novels.isNotEmpty()) "${filteredNovels.size} novels" else null,
                isExpanded = isSearching,
                expandedContent = {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search library...", fontSize = 14.sp) },
                        leadingIcon = { 
                            IconButton(onClick = { 
                                isSearching = false
                                searchQuery = ""
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        singleLine = true
                    )
                },
                actions = {
                    IconButton(onClick = { isSearching = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryText)
                    }
                    
                    DomainFilter(selectedDomain, domains) { selectedDomain = it }
                }
            )

            if (filteredNovels.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No novels found.", color = SecondaryText)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(120.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredNovels, key = { it.url }) { novel ->
                        NovelCard(
                            novel = novel,
                            onClick = { onNovelClick(novel.crawlerName, novel.url) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DomainFilter(selected: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryText),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text(selected, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkSurface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = PrimaryText) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
