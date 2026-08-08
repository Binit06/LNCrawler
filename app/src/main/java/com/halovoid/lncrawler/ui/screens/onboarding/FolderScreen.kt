package com.halovoid.lncrawler.ui.screens.onboarding

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.halovoid.lncrawler.ui.theme.DarkSurface
import com.halovoid.lncrawler.ui.theme.SecondaryText
import kotlinx.coroutines.launch

@Composable
fun FolderScreen(
    viewModel: FolderViewModel,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedFolder by viewModel.exportFolderUri.collectAsState(initial = null)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            // Take persistable permissions
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            scope.launch {
                viewModel.setExportFolder(it)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Storage Location",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Choose a directory where your light novels, covers, and exported artifacts will be stored.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = SecondaryText
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { launcher.launch(null) },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(text = if (selectedFolder == null) "Grant Folder Access" else "Change Folder")
        }

        if (selectedFolder != null) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Selected Folder:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedFolder.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Continue")
            }
        }
    }
}
