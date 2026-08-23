package com.halovoid.lncrawler.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.halovoid.lncrawler.data.repository.PreferenceRepository
import com.halovoid.lncrawler.data.repository.RequestRepository
import com.halovoid.lncrawler.ui.screens.novel.GroupedRequestsViewModel
import com.halovoid.lncrawler.ui.screens.novel.NovelDetailViewModel
import com.halovoid.lncrawler.ui.screens.crawler.CrawlerViewModel
import com.halovoid.lncrawler.ui.screens.library.LibraryViewModel
import com.halovoid.lncrawler.ui.screens.onboarding.FolderViewModel
import com.halovoid.lncrawler.ui.screens.support.SupportViewModel
import com.halovoid.lncrawler.ui.screens.request.RequestDetailViewModel
import com.halovoid.lncrawler.ui.screens.request.RequestViewModel
import com.halovoid.lncrawler.ui.screens.search.SearchViewModel
import com.halovoid.lncrawler.ui.screens.download.DownloadViewModel
import com.halovoid.lncrawler.ui.screens.reader.ReaderViewModel

class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val requestRepository = RequestRepository.getInstance(application)
        return when {
            modelClass.isAssignableFrom(RequestViewModel::class.java) -> {
                RequestViewModel(application, requestRepository) as T
            }
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                SearchViewModel(application) as T
            }
            modelClass.isAssignableFrom(RequestDetailViewModel::class.java) -> {
                RequestDetailViewModel(application, requestRepository) as T
            }
            modelClass.isAssignableFrom(NovelDetailViewModel::class.java) -> {
                NovelDetailViewModel(application, requestRepository) as T
            }
            modelClass.isAssignableFrom(FolderViewModel::class.java) -> {
                FolderViewModel(application, PreferenceRepository(application)) as T
            }
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> {
                LibraryViewModel(application) as T
            }
            modelClass.isAssignableFrom(CrawlerViewModel::class.java) -> {
                CrawlerViewModel(application) as T
            }
            modelClass.isAssignableFrom(SupportViewModel::class.java) -> {
                SupportViewModel(application) as T
            }
            modelClass.isAssignableFrom(GroupedRequestsViewModel::class.java) -> {
                GroupedRequestsViewModel(application, requestRepository) as T
            }
            modelClass.isAssignableFrom(DownloadViewModel::class.java) -> {
                DownloadViewModel(application, requestRepository) as T
            }
            modelClass.isAssignableFrom(ReaderViewModel::class.java) -> {
                ReaderViewModel(application) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
