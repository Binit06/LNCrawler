package com.halovoid.lncrawler.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.halovoid.lncrawler.data.db.AppDatabase
import com.halovoid.lncrawler.data.repository.PreferenceRepository
import com.halovoid.lncrawler.ui.screens.NovelDetailViewModel
import com.halovoid.lncrawler.ui.screens.crawler.CrawlerViewModel
import com.halovoid.lncrawler.ui.screens.library.LibraryViewModel
import com.halovoid.lncrawler.ui.screens.onboarding.FolderViewModel
import com.halovoid.lncrawler.ui.screens.support.SupportViewModel
import com.halovoid.lncrawler.ui.screens.request.RequestDetailViewModel
import com.halovoid.lncrawler.ui.screens.request.RequestViewModel

class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.getDatabase(application)
        return when {
            modelClass.isAssignableFrom(RequestViewModel::class.java) -> {
                RequestViewModel(application, db.requestDao()) as T
            }
            modelClass.isAssignableFrom(RequestDetailViewModel::class.java) -> {
                RequestDetailViewModel(application, db.requestDao()) as T
            }
            modelClass.isAssignableFrom(NovelDetailViewModel::class.java) -> {
                NovelDetailViewModel(application) as T
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
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
