package com.halovoid.lncrawler.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.halovoid.lncrawler.ui.navigation.NavGraph
import com.halovoid.lncrawler.ui.navigation.Screen

/**
 * The primary entry point Composable for the UI.
 * Manages the [NavGraph] within a Scaffold with a [NavigationBar].
 */
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val mainTabs = listOf(
        Triple(Screen.Request, "Requests", Icons.Default.AccountTree),
        Triple(Screen.Library, "Novels", Icons.Default.Book),
        Triple(Screen.Crawlers, "Crawlers", Icons.Default.Extension),
        Triple(Screen.Support, "Support", Icons.Default.Favorite)
    )

    // Only show navigation bar on main screens
    val showNavBar = mainTabs.any { (screen, _, _) -> 
        currentDestination?.hierarchy?.any { it.route == screen.route } == true 
    }

    Scaffold(
        bottomBar = {
            if (showNavBar) {
                NavigationBar {
                    mainTabs.forEach { (screen, label, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            NavGraph(navController = navController)
        }
    }
}
