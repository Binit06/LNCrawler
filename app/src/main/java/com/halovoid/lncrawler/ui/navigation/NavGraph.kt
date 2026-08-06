package com.halovoid.lncrawler.ui.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.halovoid.lncrawler.ui.ViewModelFactory
import com.halovoid.lncrawler.ui.screens.request.RequestScreen
import com.halovoid.lncrawler.ui.screens.request.RequestViewModel
import com.halovoid.lncrawler.ui.screens.request.RequestDetailScreen
import com.halovoid.lncrawler.ui.screens.library.LibraryScreen

/**
 * Defines the available navigation destinations in the application.
 */
sealed class Screen(val route: String) {
    object Request : Screen("request")
    object Library : Screen("library")
    object RequestDetail : Screen("request_detail/{recordId}") {
        fun createRoute(recordId: Int) = "request_detail/$recordId"
    }
    object NovelDetail : Screen("novel_detail/{crawlerName}/{novelUrl}") {
        fun createRoute(crawlerName: String, novelUrl: String) = "novel_detail/$crawlerName/${java.net.URLEncoder.encode(novelUrl, "UTF-8")}"
    }
}

/**
 * Main navigation graph setup for the app.
 */
@Composable
fun NavGraph(navController: NavHostController) {
    val application = LocalContext.current.applicationContext as Application
    
    // Using remember to ensure the factory is stable and the shared ViewModel
    // is correctly scoped to the navigation lifecycle.
    val requestViewModel: RequestViewModel = viewModel(
        factory = remember { ViewModelFactory(application) }
    )

    NavHost(navController = navController, startDestination = Screen.Request.route) {
        composable(Screen.Request.route) {
            RequestScreen(
                viewModel = requestViewModel,
                onNovelClick = { crawlerName, novelUrl ->
                    navController.navigate(Screen.NovelDetail.createRoute(crawlerName, novelUrl))
                },
                onHistoryClick = { recordId ->
                    navController.navigate(Screen.RequestDetail.createRoute(recordId))
                },
                onLibraryClick = {
                    navController.navigate(Screen.Library.route)
                }
            )
        }
        composable(Screen.Library.route) {
            LibraryScreen(
                viewModel = requestViewModel,
                onNovelClick = { crawlerName, novelUrl ->
                    navController.navigate(Screen.NovelDetail.createRoute(crawlerName, novelUrl))
                },
                onBackClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }
        composable(Screen.RequestDetail.route) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getString("recordId")?.toInt() ?: -1
            RequestDetailScreen(
                recordId = recordId,
                onBackClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                onOpenUrl = { url ->
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    application.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
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
