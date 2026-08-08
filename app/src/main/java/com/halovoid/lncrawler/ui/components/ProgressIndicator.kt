package com.halovoid.lncrawler.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.halovoid.lncrawler.ui.theme.BorderColor
import com.halovoid.lncrawler.ui.theme.ErrorRed
import com.halovoid.lncrawler.ui.theme.SecondaryText
import com.halovoid.lncrawler.ui.theme.SuccessGreen

// TODO: Add Smooth Animation to the Progress Bar
@Composable
fun ProgressIndicator(
    success: Int,
    failed: Int,
    cancelled: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    // 1. Determine the "track" background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(BorderColor)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Success Segment - Green
            if (success > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(success.toFloat())
                        .background(SuccessGreen)
                )
            }

            // Failed Segment (Red)
            if (failed > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(failed.toFloat())
                        .background(ErrorRed)
                )
            }

            // Canceled Segment (Secondary)
            if (cancelled > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(cancelled.toFloat())
                        .background(SecondaryText)
                )
            }

            // Remaining Space (Transparent)
            val remaining = total - (success + failed + cancelled)
            if (remaining > 0) {
                Spacer(modifier = Modifier.weight(remaining.toFloat()))
            }
        }
    }
}