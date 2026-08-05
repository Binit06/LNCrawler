package com.halovoid.lncrawler.ui.theme

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Dark color scheme following Tachiyomi/Mihon design principles.
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryAccent,
    secondary = PrimaryAccent,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.Black, // Icons on cyan buttons should be black
    onBackground = PrimaryText,
    onSurface = PrimaryText,
    onSurfaceVariant = SecondaryText,
    outline = BorderColor
)

/**
 * Light color scheme (Simplified for now, as the user requested a dark theme focus).
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryAccent,
    background = PureWhite,
    surface = LightGray
)

/**
 * Main theme for LNCrawler.
 * Defaults to dark mode for the requested Tachiyomi aesthetic.
 */
@Composable
fun LNCrawlerTheme(
    darkTheme: Boolean = true, // Default to dark theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
