package com.halovoid.lncrawler.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.halovoid.lncrawler.ui.navigation.NavGraph

/**
 * The primary entry point Composable for the UI.
 * Manages the [NavGraph] within a Surface.
 * Bottom navigation has been removed for a cleaner, single-purpose interface.
 * Part of the UI layer.
 */
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    Surface {
        NavGraph(navController = navController)
    }
}
