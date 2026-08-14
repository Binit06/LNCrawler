package com.halovoid.lncrawler.ui.screens.support

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.lncrawler.BuildConfig
import com.halovoid.lncrawler.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    viewModel: SupportViewModel
) {
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    val context = LocalContext.current

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
            AppUpdateCard(
                state = updateState,
                onUpdateClick = { update ->
                    if (update.apkDownloadUrl != null) {
                        viewModel.startUpdateDownload(update.apkDownloadUrl)
                    } else {
                        uriHandler.openUri(update.releaseUrl)
                    }
                },
                onInstallClick = { uri ->
                    viewModel.installUpdate(context, uri)
                },
                onRetryClick = { viewModel.checkForUpdates() }
            )

            Spacer(modifier = Modifier.height(24.dp))

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
            
            Spacer(modifier = Modifier.height(24.dp))

            // Community Section
            SectionHeader("Community")
            
            SupportCard(
                title = "Join Discord",
                description = "Get help, report bugs, suggest features, and talk directly with the developer and other users.",
                icon = Icons.Default.Forum,
                buttonText = "Join Discord",
                onClick = { uriHandler.openUri("https://discord.gg/A6cY7pN6Y") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SupportCard(
                title = "Star on GitHub",
                description = "Show your support by starring the repository. It helps more people discover the project.",
                icon = Icons.Default.Star,
                buttonText = "GitHub Repo",
                onClick = { uriHandler.openUri("https://github.com/Binit06/LNCrawler") }
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Support Section
            SectionHeader("Support Development")
            
            SupportCard(
                title = "Buy Me A Coffee",
                description = "Help support the development by buying me a coffee! Your contributions help keep the project alive and free of ads.",
                icon = Icons.Default.Payments,
                buttonText = "Support",
                onClick = { uriHandler.openUri("https://buymeacoffee.com/halovoid") }
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Development Section
            SectionHeader("For Developers")
            
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
fun AppUpdateCard(
    state: AppUpdateState,
    onUpdateClick: (AppUpdateState.UpdateAvailable) -> Unit,
    onInstallClick: (String) -> Unit,
    onRetryClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (state is AppUpdateState.UpdateAvailable) PrimaryAccent.copy(alpha = 0.5f) else BorderColor
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (title, color, icon) = when (state) {
                        is AppUpdateState.Loading, is AppUpdateState.Idle -> 
                            Triple("Checking Updates", PrimaryText, Icons.Default.Refresh)
                        is AppUpdateState.UpdateAvailable -> 
                            Triple("Update Available", PrimaryAccent, Icons.Default.SystemUpdate)
                        is AppUpdateState.Downloading ->
                            Triple("Downloading...", PrimaryAccent, Icons.Default.SystemUpdate)
                        is AppUpdateState.ReadyToInstall ->
                            Triple("Ready to Install", PrimaryAccent, Icons.Default.CheckCircle)
                        is AppUpdateState.Installing ->
                            Triple("Installing...", PrimaryAccent, Icons.Default.Refresh)
                        is AppUpdateState.UpToDate -> 
                            Triple("Latest Version", PrimaryText, Icons.Default.CheckCircle)
                        is AppUpdateState.Error -> 
                            Triple("Check Failed", MaterialTheme.colorScheme.error, Icons.Default.Error)
                    }
                    
                    if (state !is AppUpdateState.Loading) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = when (state) {
                        is AppUpdateState.Loading, is AppUpdateState.Idle -> "Connecting to GitHub..."
                        is AppUpdateState.UpdateAvailable -> "Version ${state.tagName} is now ready."
                        is AppUpdateState.Downloading -> "Fetching latest APK..."
                        is AppUpdateState.ReadyToInstall -> "Download complete. Tap to install."
                        is AppUpdateState.Installing -> "The installer should open shortly."
                        is AppUpdateState.UpToDate -> "You are on the latest release."
                        is AppUpdateState.Error -> "Please check your internet connection."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            when (state) {
                is AppUpdateState.Loading, is AppUpdateState.Idle, is AppUpdateState.Installing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = PrimaryAccent,
                        strokeWidth = 2.dp
                    )
                }
                is AppUpdateState.Downloading -> {
                    LinearProgressIndicator(
                        modifier = Modifier.width(64.dp),
                        color = PrimaryAccent,
                        trackColor = PrimaryAccent.copy(alpha = 0.1f)
                    )
                }
                is AppUpdateState.UpdateAvailable -> {
                    Button(
                        onClick = { onUpdateClick(state) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Update", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                is AppUpdateState.ReadyToInstall -> {
                    Button(
                        onClick = { onInstallClick(state.uri) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Install", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                is AppUpdateState.UpToDate -> {
                    // No trailing icon needed if we have one in the title
                }
                is AppUpdateState.Error -> {
                    IconButton(onClick = onRetryClick) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = PrimaryAccent,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
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
