package com.halovoid.lncrawler.ui.screens.novel.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.ui.theme.*

@Composable
fun NovelSynopsisSection(
    novel: Novel,
    isExpanded: Boolean,
    onExpandClick: () -> Unit
) {
    Column(modifier = Modifier
        .animateContentSize()
        .padding(horizontal = 24.dp)
        .padding(bottom = 24.dp)) {
        Text(
            "Synopsis",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = novel.description ?: "No description available.",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText,
            maxLines = if (isExpanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 24.sp // Better readability for long text
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isExpanded) "Show Less" else "Read More",
            color = BrandAccent, // Use sophisticated brand accent for interactive text
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable { onExpandClick() }
                .padding(vertical = 4.dp)
        )
    }
}
