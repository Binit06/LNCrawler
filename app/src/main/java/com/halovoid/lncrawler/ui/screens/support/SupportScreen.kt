package com.halovoid.lncrawler.ui.screens.support

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    viewModel: SupportViewModel,
    onNavigateToUpdate: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Unified Identity & Status Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "Support & Community",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryText
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        color = SecondaryText,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " · ",
                        color = SecondaryText.copy(alpha = 0.5f)
                    )
                    AppUpdateStatus(
                        state = updateState,
                        onUpdateClick = { _ ->
                            onNavigateToUpdate()
                        },
                        onInstallClick = { uri ->
                            viewModel.installUpdate(context, uri)
                        },
                        onRetryClick = { viewModel.checkForUpdates() }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Prominent Update Card
                if (updateState is AppUpdateState.UpdateAvailable || updateState is AppUpdateState.ReadyToInstall) {
                    UpdateProminentCard(
                        state = updateState,
                        onClick = {
                            if (updateState is AppUpdateState.UpdateAvailable) {
                                onNavigateToUpdate()
                            } else if (updateState is AppUpdateState.ReadyToInstall) {
                                viewModel.installUpdate(context, (updateState as AppUpdateState.ReadyToInstall).uri)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 2. The Emotional Message (The "Why")
                AboutSection()

                Spacer(modifier = Modifier.height(40.dp))

                // 3. Ways to Engage (The "How")
                SectionLabel("Community")
                EngagementGroup {
                    SupportItem(
                        title = "Discord Server",
                        subtitle = "Get help, report bugs, and chat with users.",
                        icon = Icons.Default.Forum,
                        iconTint = DiscordBlurple,
                        onClick = { uriHandler.openUri("https://discord.gg/A6cY7pN6Y") }
                    )
                    ItemDivider()
                    SupportItem(
                        title = "GitHub Repository",
                        subtitle = "Follow development or star the project.",
                        icon = Icons.Default.Star,
                        iconTint = GitHubOrange,
                        onClick = { uriHandler.openUri("https://github.com/Binit06/LNCrawler") }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                SectionLabel("Support Development")
                EngagementGroup {
                    SupportItem(
                        title = "Buy Me A Coffee",
                        subtitle = "Help keep LNCrawler Active",
                        icon = Icons.Default.Payments,
                        iconTint = BrandAccent,
                        onClick = { uriHandler.openUri("https://buymeacoffee.com/halovoid") }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                SectionLabel("For Developers")
                EngagementGroup {
                    SupportItem(
                        title = "Contribute Sources",
                        subtitle = "Write new crawlers for missing novel sites.",
                        icon = Icons.Default.Terminal,
                        iconTint = PrimaryAccent,
                        onClick = { uriHandler.openUri("https://github.com/Binit06/LNCrawlerSources") }
                    )
                }
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun AppUpdateStatus(
    state: AppUpdateState,
    onUpdateClick: (AppUpdateState.UpdateAvailable) -> Unit,
    onInstallClick: (String) -> Unit,
    onRetryClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val (text, color, isLoading) = when (state) {
            is AppUpdateState.Loading, is AppUpdateState.Idle -> 
                Triple("Checking updates...", SecondaryText, true)
            is AppUpdateState.UpdateAvailable -> 
                Triple("Update available", BrandAccent, false)
            is AppUpdateState.Downloading ->
                Triple("Downloading...", BrandAccent, true)
            is AppUpdateState.ReadyToInstall ->
                Triple("Ready to install", SuccessGreen, false)
            is AppUpdateState.Installing ->
                Triple("Installing...", BrandAccent, true)
            is AppUpdateState.UpToDate -> 
                Triple("Up to date", SuccessGreen.copy(alpha = 0.7f), false)
            is AppUpdateState.Error -> 
                Triple("Check failed", ErrorRed, false)
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                color = color,
                strokeWidth = 1.5.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
        } else if (state !is AppUpdateState.UpToDate) {
            Icon(
                imageVector = if (state is AppUpdateState.Error) Icons.Default.Error else Icons.Default.Info,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = if (state is AppUpdateState.UpToDate) FontWeight.Normal else FontWeight.Bold,
            modifier = Modifier.clickable(enabled = state is AppUpdateState.UpdateAvailable || state is AppUpdateState.ReadyToInstall || state is AppUpdateState.Error) {
                when (state) {
                    is AppUpdateState.UpdateAvailable -> onUpdateClick(state)
                    is AppUpdateState.ReadyToInstall -> onInstallClick(state.uri)
                    is AppUpdateState.Error -> onRetryClick()
                    else -> {}
                }
            }
        )
    }
}

@Composable
fun EngagementGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
        content = { Column(content = content) }
    )
}

@Composable
fun ItemDivider() {
    HorizontalDivider(
        color = BorderColor.copy(alpha = 0.4f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}


@Composable
fun UpdateProminentCard(
    state: AppUpdateState,
    onClick: () -> Unit
) {
    val isReady = state is AppUpdateState.ReadyToInstall
    val tagName = if (state is AppUpdateState.UpdateAvailable) state.tagName else "New Version"
    
    Surface(
        onClick = onClick,
        color = if (isReady) SuccessGreen.copy(alpha = 0.1f) else BrandAccent.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isReady) SuccessGreen.copy(alpha = 0.3f) else BrandAccent.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isReady) SuccessGreen.copy(alpha = 0.2f) else BrandAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = if (isReady) SuccessGreen else BrandAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isReady) "Ready to Install" else "Update Available",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Text(
                    text = if (isReady) "Tap to finish installation" else "New version $tagName is out",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SecondaryText.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun AboutSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandAccent.copy(alpha = 0.05f))
            .border(1.dp, BrandAccent.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = SupportRose,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Enjoying LNCrawler?",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryText,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "LNCrawler is an open-source project maintained by a single developer. Your support helps keep the crawlers updated and the app free of ads.",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = SecondaryText,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun SupportItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = iconTint.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title, 
                style = MaterialTheme.typography.bodyLarge, 
                fontWeight = FontWeight.Bold,
                color = PrimaryText 
            ) 
            Text(
                text = subtitle, 
                style = MaterialTheme.typography.bodySmall, 
                color = SecondaryText 
            ) 
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = SecondaryText.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}
