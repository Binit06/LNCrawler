package com.halovoid.lncrawler.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.halovoid.lncrawler.ui.screens.request.RequestScreen

/**
 * Defines the available navigation destinations in the application.
 * Part of the UI layer's navigation logic.
 */
sealed class Screen(val route: String) {
    /** The main screen for inputting URLs and viewing recent requests. */
    object Request : Screen("request")
    /** Detailed view of a specific novel, allowing for chapter browsing and export. */
    object NovelDetail : Screen("novel_detail/{crawlerName}/{novelUrl}") {
        /** Creates a route string for the novel detail screen with encoded parameters. */
        fun createRoute(crawlerName: String, novelUrl: String) = "novel_detail/$crawlerName/${java.net.URLEncoder.encode(novelUrl, "UTF-8")}"
    }
}

/**
 * Main navigation graph setup for the app.
 * Configures the [NavHost] and maps [Screen] routes to Composable screens.
 */
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Request.route) {
        composable(Screen.Request.route) {
            RequestScreen(
                onNovelClick = { crawlerName, novelUrl ->
                    navController.navigate(Screen.NovelDetail.createRoute(crawlerName, novelUrl))
                }
            )
        }
        composable(Screen.NovelDetail.route) { backStackEntry ->
            val crawlerName = backStackEntry.arguments?.getString("crawlerName") ?: ""
            val novelUrl = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("novelUrl") ?: "", "UTF-8")
            com.halovoid.lncrawler.ui.screens.NovelDetailScreen(crawlerName, novelUrl)
        }
    }
}
