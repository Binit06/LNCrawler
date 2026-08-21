package com.halovoid.lncrawler.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.halovoid.lncrawler.ui.screens.novel.GroupedRequestsScreen
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.ui.screens.novel.NovelDetailScreen
import com.halovoid.lncrawler.ui.screens.novel.NovelDetailViewModel
import com.halovoid.lncrawler.ui.screens.request.RequestScreen
import com.halovoid.lncrawler.ui.screens.request.RequestViewModel
import com.halovoid.lncrawler.ui.screens.request.RequestDetailScreen
import com.halovoid.lncrawler.ui.screens.library.LibraryScreen
import com.halovoid.lncrawler.ui.screens.onboarding.FolderScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halovoid.lncrawler.ui.screens.onboarding.FolderViewModel
import androidx.compose.runtime.rememberCoroutineScope
import com.halovoid.lncrawler.ui.screens.onboarding.PermissionScreen
import com.halovoid.lncrawler.ui.screens.onboarding.SourceSyncScreen
import com.halovoid.lncrawler.ui.screens.crawler.CrawlerScreen
import com.halovoid.lncrawler.ui.screens.crawler.CrawlerViewModel
import kotlinx.coroutines.launch
import com.halovoid.lncrawler.ui.screens.library.LibraryViewModel
import com.halovoid.lncrawler.ui.screens.support.SupportScreen
import com.halovoid.lncrawler.ui.screens.support.SupportViewModel
import kotlinx.coroutines.flow.first
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Defines the available navigation destinations in the application.
 */
sealed class Screen(val route: String) {
    object Permissions : Screen("permissions")
    object FolderSelection: Screen("folder_selection")
    object SourceSync: Screen("source_sync")
    object Request : Screen("request")
    object Library : Screen("library")
    object Crawlers : Screen("crawlers")
    object Support : Screen("support")
    object RequestDetail : Screen("request_detail/{requestId}") {
        fun createRoute(requestId: String) = "request_detail/${URLEncoder.encode(requestId, "UTF-8")}"
    }
    object NovelDetail : Screen("novel_detail/{crawlerName}/{novelUrl}") {
        fun createRoute(crawlerName: String, novelUrl: String) = "novel_detail/$crawlerName/${URLEncoder.encode(novelUrl, "UTF-8")}"
    }
    object GroupedRequests : Screen("grouped_requests/{novelUrl}/{type}") {
        fun createRoute(novelUrl: String, type: String) = "grouped_requests/${URLEncoder.encode(novelUrl, "UTF-8")}/$type"
    }
}

/**
 * Main navigation graph setup for the app.
 */
@Composable
fun NavGraph(navController: NavHostController) {
    val application = LocalContext.current.applicationContext as Application
    val preferenceRepository = remember { PreferenceRepository(application) }
    val scope = rememberCoroutineScope()
    
    val requestViewModel: RequestViewModel = viewModel(
        factory = remember { ViewModelFactory(application) }
    )

    val libraryViewModel: LibraryViewModel = viewModel(
        factory = remember { ViewModelFactory(application) }
    )

    val folderViewModel: FolderViewModel = viewModel(
        factory = remember { ViewModelFactory(application) }
    )

    val crawlerViewModel: CrawlerViewModel = viewModel(
        factory = remember { ViewModelFactory(application) }
    )

    val supportViewModel: SupportViewModel = viewModel(
        factory = remember { ViewModelFactory(application) }
    )

    var startRoute by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val folderUri = preferenceRepository.exportFolderUri.first()
        val onboardingCompleted = preferenceRepository.isOnboardingCompleted.first()

        startRoute = if (onboardingCompleted && folderUri != null) {
            Screen.Request.route
        } else {
            Screen.FolderSelection.route
        }
    }
    startRoute?.let { route ->
        NavHost(
            navController = navController,
            startDestination = route,
            enterTransition = {
                fadeIn(animationSpec = tween(400)) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(400)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(400)) + slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(400)
                )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(400)) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(400)
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(400)) + slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(400)
                )
            }
        ) {
            composable(Screen.FolderSelection.route) {
                FolderScreen(
                    folderViewModel,
                    onNext = {
                        navController.navigate(Screen.Permissions.route)
                    }
                )
            }
            composable(Screen.Permissions.route) {
                PermissionScreen(
                    onNext = {
                        navController.navigate(Screen.SourceSync.route)
                    }
                )
            }
            composable(Screen.SourceSync.route) {
                SourceSyncScreen(
                    onComplete = {
                        scope.launch {
                            preferenceRepository.setOnboardingCompleted(true)
                            navController.navigate(Screen.Request.route) {
                                popUpTo(Screen.FolderSelection.route) {
                                    inclusive = true
                                }
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
                    },
                    onGroupClick = { type ->
                        navController.navigate(Screen.GroupedRequests.createRoute("all", type.name))
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
                CrawlerScreen(viewModel = crawlerViewModel)
            }
            composable(Screen.Support.route) {
                SupportScreen(viewModel = supportViewModel)
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
                    },
                    onGroupClick = { type ->
                        navController.navigate(Screen.GroupedRequests.createRoute(novelUrl, type.name))
                    }
                )
            }
            composable(Screen.GroupedRequests.route) { backStackEntry ->
                val novelUrl = URLDecoder.decode(
                    backStackEntry.arguments?.getString("novelUrl") ?: "",
                    "UTF-8"
                )
                val typeName = backStackEntry.arguments?.getString("type") ?: ""
                val type = RequestType.valueOf(typeName)

                val requests: List<Request>
                val cancellingRequestIds: Set<String>
                val allowAction: Boolean

                if (novelUrl == "all") {
                    requests = requestViewModel.requestHistory.collectAsStateWithLifecycle().value
                    cancellingRequestIds = requestViewModel.cancellingRequestIds.collectAsStateWithLifecycle().value
                    allowAction = true
                } else {
                    val viewModel: NovelDetailViewModel = viewModel(
                        factory = remember { ViewModelFactory(application) }
                    )
                    LaunchedEffect(novelUrl) {
                        viewModel.loadNovel(novelUrl)
                    }
                    requests = viewModel.rootRequests.collectAsStateWithLifecycle().value
                    cancellingRequestIds = emptySet()
                    allowAction = false
                }

                GroupedRequestsScreen(
                    type = type,
                    requests = requests,
                    onBack = { navController.popBackStack() },
                    onRequestClick = { requestId ->
                        navController.navigate(Screen.RequestDetail.createRoute(requestId))
                    },
                    onReplay = { requestId ->
                        if (novelUrl == "all") requestViewModel.replayRequest(requestId)
                    },
                    onCancel = { requestId ->
                        if (novelUrl == "all") requestViewModel.cancelRequest(requestId)
                    },
                    onResolveCloudflare = { requestId, url ->
                        if (novelUrl == "all") requestViewModel.resolveCloudflare(requestId, url)
                    },
                    cancellingRequestIds = cancellingRequestIds,
                    allowAction = allowAction
                )
            }
        }
    }
}
