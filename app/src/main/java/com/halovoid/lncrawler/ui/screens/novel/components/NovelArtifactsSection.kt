package com.halovoid.lncrawler.ui.screens.novel.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.halovoid.lncrawler.domain.models.Artifact
import com.halovoid.lncrawler.ui.components.artifact.ArtifactCard
import com.halovoid.lncrawler.ui.theme.PrimaryText

fun LazyListScope.novelArtifactsSection(
    artifacts: List<Artifact>,
    onDownload: (Artifact) -> Unit
) {
    if (artifacts.isEmpty()) return

    item {
        Text(
            "Artifacts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 8.dp)
        )
    }
    
    items(artifacts, key = { it.id }) { artifact ->
        Box(modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 8.dp)) {
            ArtifactCard (
                artifact = artifact,
                onDownload = { onDownload(it) }
            )
        }
    }
    
    item { Spacer(modifier = Modifier.height(16.dp)) }
}
