package com.halovoid.lncrawler.ui.screens.request

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.halovoid.lncrawler.ui.components.NovelCard
import com.halovoid.lncrawler.domain.models.Novel

/**
 * UI Screen for inputting new novel URLs and viewing a list of recently requested novels.
 * Acts as the home screen for the app.
 *
 * @param onNovelClick Callback when a novel is selected (e.g., to view details).
 * @param viewModel ViewModel for managing URL validation and the list of saved novels.
 */
@Composable
fun RequestScreen(
    onNovelClick: (String, String) -> Unit,
    viewModel: RequestViewModel = viewModel()
) {
    var urlInput by remember { mutableStateOf("") }
    val error by viewModel.error.collectAsState()
    val savedNovels by viewModel.savedNovels.collectAsState()
    val isExportingUrl by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val context = LocalContext.current
    
    var novelToExport by remember { mutableStateOf<Novel?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/epub+zip")
    ) { uri ->
        uri?.let {
            novelToExport?.let { novel ->
                viewModel.exportNovelToUri(novel, it) {
                    Toast.makeText(context, "Export complete!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Fetch New Novel",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Novel URL (e.g., novelbins.com/novel/...)") },
            isError = error != null,
            supportingText = {
                if (error != null) {
                    Text(text = error!!, color = MaterialTheme.colorScheme.error)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                val crawlerName = viewModel.validateUrl(urlInput)
                if (crawlerName != null) {
                    onNovelClick(crawlerName, urlInput)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Fetch Novel")
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
        
        Text(
            text = "Recently Requested",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(savedNovels) { novel ->
                NovelCard(
                    novel = novel,
                    onClick = { 
                        onNovelClick(novel.crawlerName ?: "NovelBin", novel.url) 
                    },
                    onExportClick = {
                        novelToExport = novel
                        val fileName = "${novel.title.filter { it.isLetterOrDigit() }}.epub"
                        exportLauncher.launch(fileName)
                    }
                )
                if (isExportingUrl == novel.url) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = exportProgress ?: "Starting export...",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
