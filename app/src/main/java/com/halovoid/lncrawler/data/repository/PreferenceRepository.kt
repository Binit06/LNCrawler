package com.halovoid.lncrawler.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import com.halovoid.lncrawler.data.preferences.appDataStore
import kotlinx.coroutines.flow.map

private val EXPORT_FOLDER_URI = stringPreferencesKey("export_folder_uri")

class PreferenceRepository(
    private val context: Context
) {

    val exportFolderUri: Flow<Uri?> =
        context.appDataStore.data.map { preferences ->
            preferences[EXPORT_FOLDER_URI]?.let(Uri::parse)
        }

    suspend fun setExportFolder(uri: Uri) {
        context.appDataStore.edit { preferences ->
            preferences[EXPORT_FOLDER_URI] = uri.toString()
        }
    }

    suspend fun clearExportFolder() {
        context.appDataStore.edit { preferences ->
            preferences.remove(EXPORT_FOLDER_URI)
        }
    }
}