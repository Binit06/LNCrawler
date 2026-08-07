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
import com.halovoid.lncrawler.ui.screens.NovelDetailScreen
import com.halovoid.lncrawler.ui.screens.request.RequestScreen
import com.halovoid.lncrawler.ui.screens.request.RequestViewModel
import com.halovoid.lncrawler.ui.screens.request.RequestDetailScreen
import com.halovoid.lncrawler.ui.screens.library.LibraryScreen
import com.halovoid.lncrawler.ui.screens.onboarding.FolderScreen
import com.halovoid.lncrawler.ui.screens.onboarding.FolderViewModel
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Defines the available navigation destinations in the application.
 */
sealed class Screen(val route: String) {
    object FolderSelection: Screen("folder_selection")
    object Request : Screen("request")
    object Library : Screen("library")
    object RequestDetail : Screen("request_detail/{recordId}") {
        fun createRoute(recordId: Int) = "request_detail/$recordId"
    }
    object NovelDetail : Screen("novel_detail/{crawlerName}/{novelUrl}") {
        fun createRoute(crawlerName: String, novelUrl: String) = "novel_detail/$crawlerName/${URLEncoder.encode(novelUrl, "UTF-8")}"
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

    val folderViewModel: FolderViewModel = viewModel(
        factory = remember { ViewModelFactory(application) }
    )

    NavHost(navController = navController, startDestination = Screen.Request.route) {
        composable(Screen.FolderSelection.route) {
            FolderScreen(
                folderViewModel,
                onNext = {
                    navController.navigate(Screen.Request.route) {
                        popUpTo(Screen.FolderSelection.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable(Screen.Request.route) {
            RequestScreen(
                viewModel = requestViewModel,
                onHistoryClick = { requestId ->
                    navController.navigate(Screen.RequestDetail.createRoute(requestId))
                },
                onLibraryClick = {
                    navController.navigate(Screen.Library.route)
                },
                onFetchComplete = { requestId ->
                    navController.navigate(Screen.RequestDetail.createRoute((requestId)))
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
            val requestId = backStackEntry.arguments?.getString("requestId")?.toIntOrNull()
            RequestDetailScreen(
                requestId = requestId,
                onBackClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }
        composable(Screen.NovelDetail.route) { backStackEntry ->
            val crawlerName = backStackEntry.arguments?.getString("crawlerName") ?: ""
            val novelUrl = URLDecoder.decode(backStackEntry.arguments?.getString("novelUrl") ?: "", "UTF-8")
            NovelDetailScreen(crawlerName, novelUrl)
        }
    }
}
