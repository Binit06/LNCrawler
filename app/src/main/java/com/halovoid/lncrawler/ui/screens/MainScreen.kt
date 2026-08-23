package com.halovoid.lncrawler.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.halovoid.lncrawler.ui.navigation.NavGraph
import com.halovoid.lncrawler.ui.navigation.Screen
import com.halovoid.lncrawler.ui.theme.PrimaryAccent
import com.halovoid.lncrawler.ui.theme.SecondaryText

/**
 * The primary entry point Composable for the UI.
 * Manages the [NavGraph] within a Scaffold with a [NavigationBar].
 */
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val mainTabs = remember {
        listOf(
            TabInfo(Screen.Request, "Browse", Icons.Outlined.Explore, Icons.Filled.Explore),
            TabInfo(Screen.Library, "Library", Icons.AutoMirrored.Outlined.LibraryBooks, Icons.AutoMirrored.Filled.LibraryBooks),
            TabInfo(Screen.Downloads, "Downloads", Icons.Outlined.DownloadForOffline, Icons.Filled.DownloadForOffline),
            TabInfo(Screen.Support, "More", Icons.Outlined.MoreHoriz, Icons.Filled.MoreHoriz)
        )
    }

    // Determine if we should show the nav bar by checking if the current route is a top-level tab
    val showNavBar = remember(currentDestination) {
        mainTabs.any { tab -> 
            currentDestination?.hierarchy?.any { it.route == tab.screen.route } == true 
        }
    }

    Scaffold(
        bottomBar = {
            if (showNavBar) {
                LNCrawlerNavigationBar(
                    mainTabs = mainTabs,
                    currentDestination = currentDestination,
                    onNavigate = { route ->
                        navController.navigate(route) {
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
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
            color = MaterialTheme.colorScheme.background
        ) {
            NavGraph(navController = navController)
        }
    }
}

@Composable
private fun LNCrawlerNavigationBar(
    mainTabs: List<TabInfo>,
    currentDestination: androidx.navigation.NavDestination?,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        mainTabs.forEach { tab ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == tab.screen.route } == true
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = if (isSelected) tab.filledIcon else tab.outlinedIcon,
                        contentDescription = tab.label
                    )
                },
                label = { 
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ) 
                },
                selected = isSelected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryAccent,
                    unselectedIconColor = SecondaryText,
                    selectedTextColor = PrimaryAccent,
                    unselectedTextColor = SecondaryText,
                    indicatorColor = Color.Transparent
                ),
                onClick = { onNavigate(tab.screen.route) }
            )
        }
    }
}

data class TabInfo(
    val screen: Screen,
    val label: String,
    val outlinedIcon: ImageVector,
    val filledIcon: ImageVector
)
