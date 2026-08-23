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
import com.halovoid.lncrawler.ui.screens.request.NovelPreviewScreen
import com.halovoid.lncrawler.ui.screens.download.DownloadScreen
import com.halovoid.lncrawler.ui.screens.download.DownloadViewModel
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
import com.halovoid.lncrawler.ui.screens.search.SearchViewModel
import com.halovoid.lncrawler.ui.screens.reader.ReaderScreen
import com.halovoid.lncrawler.ui.screens.reader.ReaderViewModel
import com.halovoid.lncrawler.ui.screens.novel.GroupedRequestsViewModel
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
    object Downloads : Screen("downloads")
    object Crawlers : Screen("crawlers")
    object Support : Screen("support")
    object RequestDetail : Screen("request_detail/{requestId}") {
        fun createRoute(requestId: String) = "request_detail/${URLEncoder.encode(requestId, "UTF-8")}"
    }
    object NovelDetail : Screen("novel_detail/{crawlerName}/{novelUrl}") {
        fun createRoute(crawlerName: String, novelUrl: String) = "novel_detail/$crawlerName/${URLEncoder.encode(novelUrl, "UTF-8")}"
    }
    object NovelPreview : Screen("novel_preview")
    object GroupedRequests : Screen("grouped_requests/{contextType}/{contextValue}/{type}") {
        fun createRoute(contextType: String, contextValue: String, type: String) = 
            "grouped_requests/$contextType/${URLEncoder.encode(contextValue, "UTF-8")}/$type"
    }
    object Reader : Screen("reader/{novelUrl}/{initialChapterId}") {
        fun createRoute(novelUrl: String, initialChapterId: Int) = 
            "reader/${URLEncoder.encode(novelUrl, "UTF-8")}/$initialChapterId"
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
                fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(400))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(400))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(400))
            }
        ) {
            composable(Screen.FolderSelection.route) {
                val folderViewModel: FolderViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )
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
            composable(Screen.NovelPreview.route) { backStackEntry ->
                // Shared with Request screen
                val requestEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.Request.route)
                }
                val requestViewModel: RequestViewModel = viewModel(
                    viewModelStoreOwner = requestEntry,
                    factory = remember { ViewModelFactory(application) }
                )
                NovelPreviewScreen(
                    viewModel = requestViewModel,
                    onBack = {
                        requestViewModel.clearPreview()
                        navController.popBackStack()
                    },
                    onConfirm = { crawlerName: String, url: String ->
                        requestViewModel.startNovelCrawl(crawlerName, url)
                        requestViewModel.clearPreview()
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Request.route) { backStackEntry ->
                val searchUrl = backStackEntry.savedStateHandle.get<String>("search_url")
                val requestViewModel: RequestViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )
                val searchViewModel: SearchViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )
                RequestScreen(
                    viewModel = requestViewModel,
                    searchViewModel = searchViewModel,
                    onNavigateToPreview = {
                        navController.navigate(Screen.NovelPreview.route)
                    },
                    onCrawlerClick = {
                        navController.navigate(Screen.Crawlers.route)
                    },
                    searchUrl = searchUrl
                )
            }
            composable(Screen.Library.route) {
                val libraryViewModel: LibraryViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )
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
                val crawlerViewModel: CrawlerViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )
                CrawlerScreen(viewModel = crawlerViewModel)
            }
            composable(Screen.Downloads.route) {
                val downloadViewModel: DownloadViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )
                DownloadScreen(
                    viewModel = downloadViewModel,
                    onRequestClick = { requestId ->
                        navController.navigate(Screen.RequestDetail.createRoute(requestId))
                    },
                    onGroupClick = { type ->
                        navController.navigate(Screen.GroupedRequests.createRoute("ALL", "all", type.name))
                    }
                )
            }
            composable(Screen.Support.route) {
                val supportViewModel: SupportViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )
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
                    onGroupClick = { type ->
                        navController.navigate(Screen.GroupedRequests.createRoute("DEPENDENCY", requestId, type.name))
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
                        navController.navigate(Screen.GroupedRequests.createRoute("NOVEL", novelUrl, type.name))
                    },
                    onChapterClick = { url, chapterId ->
                        navController.navigate(Screen.Reader.createRoute(url, chapterId))
                    }
                )
            }
            composable(Screen.GroupedRequests.route) { backStackEntry ->
                val contextType = backStackEntry.arguments?.getString("contextType") ?: ""
                val contextValue = URLDecoder.decode(
                    backStackEntry.arguments?.getString("contextValue") ?: "",
                    "UTF-8"
                )
                val typeName = backStackEntry.arguments?.getString("type") ?: ""
                val type = RequestType.valueOf(typeName)

                val viewModel: GroupedRequestsViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )

                LaunchedEffect(contextType, contextValue) {
                    viewModel.loadRequests(contextType, contextValue)
                }

                val requests by viewModel.requests.collectAsStateWithLifecycle()
                val cancellingRequestIds by viewModel.cancellingRequestIds.collectAsStateWithLifecycle()
                val activeActionIds by viewModel.activeActionIds.collectAsStateWithLifecycle()

                GroupedRequestsScreen(
                    type = type,
                    requests = requests,
                    onBack = { navController.popBackStack() },
                    onRequestClick = { requestId ->
                        navController.navigate(Screen.RequestDetail.createRoute(requestId))
                    },
                    onReplay = { viewModel.replayRequest(it) },
                    onCancel = { viewModel.cancelRequest(it) },
                    onContinue = { viewModel.resumeRequest(it) },
                    onResolveCloudflare = { requestId, url ->
                        viewModel.resolveCloudflare(requestId, url)
                    },
                    cancellingRequestIds = cancellingRequestIds,
                    activeActionIds = activeActionIds,
                    allowAction = contextType == "ALL" || contextType == "DEPENDENCY"
                )
            }
            composable(Screen.Reader.route) { backStackEntry ->
                val novelUrl = URLDecoder.decode(
                    backStackEntry.arguments?.getString("novelUrl") ?: "",
                    "UTF-8"
                )
                val initialChapterId = backStackEntry.arguments?.getString("initialChapterId")?.toIntOrNull() ?: -1

                val viewModel: ReaderViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )

                ReaderScreen(
                    novelUrl = novelUrl,
                    initialChapterId = initialChapterId,
                    onBack = { navController.popBackStack() },
                    viewModel = viewModel
                )
            }
        }
    }
}
