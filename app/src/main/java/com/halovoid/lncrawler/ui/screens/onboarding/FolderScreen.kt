package com.halovoid.lncrawler.ui.screens.onboarding

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.halovoid.lncrawler.ui.theme.PrimaryAccent
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
    val friendlyPath by viewModel.friendlyPath.collectAsState(initial = "")

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

    OnboardingStep(
        title = "Storage Location",
        subtitle = "Choose where your light novels, covers, and artifacts will be stored.",
        buttonText = "Proceed",
        onNext = onNext,
        isNextEnabled = selectedFolder != null
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center

        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(PrimaryAccent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (selectedFolder == null) Icons.Default.CreateNewFolder else Icons.Default.Folder,
                    contentDescription = null,
                    tint = PrimaryAccent,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (selectedFolder != null) {
                Text(
                    text = "Selected Path",
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryAccent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = friendlyPath,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            } else {
                Text(
                    text = "No folder selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedButton(
                onClick = { launcher.launch(null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryAccent)
            ) {
                Text(
                    text = if (selectedFolder == null) "Choose Directory" else "Change Directory",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
