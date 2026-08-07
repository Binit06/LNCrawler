package com.halovoid.lncrawler.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.halovoid.lncrawler.data.db.AppDatabase
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
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
