package com.halovoid.lncrawler.ui.screens.support

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halovoid.lncrawler.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen() {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Support Development", fontWeight = FontWeight.Bold, color = PrimaryText) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
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
                title = "Buy Me a Coffee",
                description = "Financial support allows me to dedicate more time to adding new features and fixing bugs.",
                icon = Icons.Default.Coffee,
                buttonText = "Support via Ko-fi",
                onClick = { uriHandler.openUri("https://ko-fi.com/binit06") }
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
                text = "Version 1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun SupportCard(
    title: String,
    description: String,
    icon: ImageVector,
    buttonText: String,
    onClick: () -> Unit
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
            
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(buttonText, color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        }
    }
}
