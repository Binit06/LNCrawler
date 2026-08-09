package com.halovoid.lncrawler.ui.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.halovoid.lncrawler.data.repository.PreferenceRepository
import com.halovoid.lncrawler.ui.ViewModelFactory
import com.halovoid.lncrawler.ui.screens.NovelDetailScreen
import com.halovoid.lncrawler.ui.screens.request.RequestScreen
import com.halovoid.lncrawler.ui.screens.request.RequestViewModel
import com.halovoid.lncrawler.ui.screens.request.RequestDetailScreen
import com.halovoid.lncrawler.ui.screens.library.LibraryScreen
import com.halovoid.lncrawler.ui.screens.onboarding.FolderScreen
import com.halovoid.lncrawler.ui.screens.onboarding.FolderViewModel
import com.halovoid.lncrawler.ui.screens.onboarding.PermissionScreen
import com.halovoid.lncrawler.ui.screens.crawler.CrawlerScreen
import com.halovoid.lncrawler.ui.screens.library.LibraryViewModel
import com.halovoid.lncrawler.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.flow.first
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Defines the available navigation destinations in the application.
 */
sealed class Screen(val route: String) {
    object Permissions : Screen("permissions")
    object FolderSelection: Screen("folder_selection")
    object Request : Screen("request")
    object Library : Screen("library")
    object Crawlers : Screen("crawlers")
    object Settings : Screen("settings")
    object RequestDetail : Screen("request_detail/{requestId}") {
        fun createRoute(requestId: String) = "request_detail/${URLEncoder.encode(requestId, "UTF-8")}"
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
    val preferenceRepository = remember { PreferenceRepository(application) }
    
    val requestViewModel: RequestViewModel = viewModel(
        factory = remember { ViewModelFactory(application) }
    )

    val libraryViewModel: LibraryViewModel = viewModel(
        factory = remember { ViewModelFactory(application) }
    )

    val folderViewModel: FolderViewModel = viewModel(
        factory = remember { ViewModelFactory(application) }
    )

    var startRoute by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val folderUri = preferenceRepository.exportFolderUri.first()

        startRoute = if (folderUri != null) {
            Screen.Request.route
        } else {
            Screen.Permissions.route
        }
    }
    startRoute?.let { route ->
        NavHost(navController = navController, startDestination = route) {
            composable(Screen.Permissions.route) {
                PermissionScreen(
                    onNext = {
                        navController.navigate(Screen.FolderSelection.route)
                    }
                )
            }
            composable(Screen.FolderSelection.route) {
                FolderScreen(
                    folderViewModel,
                    onNext = {
                        navController.navigate(Screen.Request.route) {
                            popUpTo(Screen.Permissions.route) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
            composable(Screen.Request.route) {
                RequestScreen(
                    viewModel = requestViewModel,
                    onRequestClick = { requestId ->
                        navController.navigate(Screen.RequestDetail.createRoute(requestId))
                    }
                )
            }
            composable(Screen.Library.route) {
                LibraryScreen(
                    viewModel = libraryViewModel,
                    onNovelClick = { crawlerName, novelUrl ->
                        navController.navigate(
                            Screen.NovelDetail.createRoute(
                                crawlerName,
                                novelUrl
                            )
                        )
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Crawlers.route) {
                CrawlerScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(Screen.RequestDetail.route) { backStackEntry ->
                val encodedId = backStackEntry.arguments?.getString("requestId") ?: ""
                val requestId = URLDecoder.decode(encodedId, "UTF-8")
                RequestDetailScreen(
                    requestId = requestId,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onRequestClick = { requestId ->
                        navController.navigate(Screen.RequestDetail.createRoute(requestId))
                    }
                )
            }
            composable(Screen.NovelDetail.route) { backStackEntry ->
                val crawlerName = backStackEntry.arguments?.getString("crawlerName") ?: ""
                val novelUrl = URLDecoder.decode(
                    backStackEntry.arguments?.getString("novelUrl") ?: "",
                    "UTF-8"
                )
                NovelDetailScreen(
                    novelUrl,
                    onRequestClick = {requestId ->
                        navController.navigate(Screen.RequestDetail.createRoute(requestId))
                    },
                    onBack = {
                        navController.popBackStack()
                    })
            }
        }
    }
}
