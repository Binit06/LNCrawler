package com.halovoid.lncrawler.ui.screens.support

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.lncrawler.BuildConfig
import com.halovoid.lncrawler.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    viewModel: SupportViewModel
) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Support Development",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (updateState is AppUpdateState.UpdateAvailable) {
                val state = updateState as AppUpdateState.UpdateAvailable
                UpdateBox(
                    tagName = state.tagName,
                    onClick = { uriHandler.openUri(state.releaseUrl) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = PrimaryAccent,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Enjoying LNCrawler?",
                style = MaterialTheme.typography.headlineMedium,
                color = PrimaryText,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "LNCrawler is an open-source project maintained by a single developer. Your support helps keep the crawlers updated and the app free of ads.",
                style = MaterialTheme.typography.bodyLarge,
                color = SecondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            SupportCard(
                title = "Star on GitHub",
                description = "Show your support by starring the repository. It helps more people discover the project.",
                icon = Icons.Default.Star,
                buttonText = "GitHub Repo",
                onClick = { uriHandler.openUri("https://github.com/Binit06/LNCrawler") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SupportCard(
                title = "Support Using UPI",
                description = "If you are in India, you can support development directly via UPI. Every contribution helps!",
                icon = Icons.Default.Payments,
                buttonText = "Pay via UPI",
                onClick = { 
                    try {
                        uriHandler.openUri("upi://pay?pa=binitlenka@okhdfcbank&pn=LNCrawler%20Developer")
                    } catch (e: Exception) {
                        scope.launch {
                            snackbarHostState.showSnackbar("No UPI app found. Please copy the ID.")
                        }
                    }
                },
                secondaryButtonText = "Copy ID",
                onSecondaryClick = {
                    clipboardManager.setText(AnnotatedString("binitlenka@okhdfcbank"))
                    scope.launch {
                        snackbarHostState.showSnackbar("UPI ID copied to clipboard")
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SupportCard(
                title = "Contribute Sources",
                description = "Are you a developer? You can help by writing new crawlers for missing novel sites.",
                icon = Icons.Default.Terminal,
                buttonText = "Source Guide",
                onClick = { uriHandler.openUri("https://github.com/Binit06/LNCrawlerSources") }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun UpdateBox(
    tagName: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PrimaryAccent.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "App Update Available",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryAccent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "A new version $tagName of LNCrawler is available!",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Update", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SupportCard(
    title: String,
    description: String,
    icon: ImageVector,
    buttonText: String,
    onClick: () -> Unit,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(buttonText, color = DarkBackground, fontWeight = FontWeight.Bold)
                }
                
                if (secondaryButtonText != null && onSecondaryClick != null) {
                    OutlinedButton(
                        onClick = onSecondaryClick,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(secondaryButtonText, color = PrimaryAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
