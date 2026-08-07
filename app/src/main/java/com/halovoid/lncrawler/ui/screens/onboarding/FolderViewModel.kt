package com.halovoid.lncrawler.ui.screens.onboarding

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.data.repository.PreferenceRepository
import kotlinx.coroutines.launch

class FolderViewModel(
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    val exportFolderUri =
        preferenceRepository.exportFolderUri

    fun setExportFolder(uri: Uri) {
        viewModelScope.launch {
            preferenceRepository.setExportFolder(uri)
        }
    }
}