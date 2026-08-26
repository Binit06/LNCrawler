package com.halovoid.lncrawler.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import com.halovoid.lncrawler.data.preferences.appDataStore
import kotlinx.coroutines.flow.map

private val EXPORT_FOLDER_URI = stringPreferencesKey("export_folder_uri")
private val ONBOARDING_COMPLETED = stringPreferencesKey("onboarding_completed")
private val LAST_SYNC_TIME = stringPreferencesKey("last_sync_time")
private val CURRENT_DEX_TAG = stringPreferencesKey("current_dex_tag")
private val BETA_MODE_APP = booleanPreferencesKey("beta_mode_app")
private val BETA_MODE_CRAWLERS = booleanPreferencesKey("beta_mode_crawlers")
private val IGNORE_IMAGES = booleanPreferencesKey("ignore_images")
private val MAX_CONCURRENT_JOBS = intPreferencesKey("max_concurrent_jobs")

class PreferenceRepository private constructor(
    private val context: Context
) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: PreferenceRepository? = null

        fun getInstance(context: Context): PreferenceRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferenceRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val exportFolderUri: Flow<Uri?> =
        context.appDataStore.data.map { preferences ->
            preferences[EXPORT_FOLDER_URI]?.let(Uri::parse)
        }

    val isOnboardingCompleted: Flow<Boolean> =
        context.appDataStore.data.map { preferences ->
            preferences[ONBOARDING_COMPLETED]?.toBoolean() ?: false
        }

    val currentDexTag: Flow<String?> =
        context.appDataStore.data.map { preferences ->
            preferences[CURRENT_DEX_TAG]
        }

    val betaModeApp: Flow<Boolean> =
        context.appDataStore.data.map { preferences ->
            preferences[BETA_MODE_APP] ?: false
        }

    val betaModeCrawlers: Flow<Boolean> =
        context.appDataStore.data.map { preferences ->
            preferences[BETA_MODE_CRAWLERS] ?: false
        }

    val ignoreImages: Flow<Boolean> =
        context.appDataStore.data.map { preferences ->
            preferences[IGNORE_IMAGES] ?: false
        }

    val maxConcurrentJobs: Flow<Int> =
        context.appDataStore.data.map { preferences ->
            preferences[MAX_CONCURRENT_JOBS] ?: 3
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed.toString()
        }
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

    suspend fun setCurrentDexTag(tag: String) {
        context.appDataStore.edit { preferences ->
            preferences[CURRENT_DEX_TAG] = tag
        }
    }

    suspend fun setBetaModeApp(enabled: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[BETA_MODE_APP] = enabled
        }
    }

    suspend fun setBetaModeCrawlers(enabled: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[BETA_MODE_CRAWLERS] = enabled
        }
    }

    suspend fun setIgnoreImages(enabled: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[IGNORE_IMAGES] = enabled
        }
    }

    suspend fun setMaxConcurrentJobs(jobs: Int) {
        context.appDataStore.edit { preferences ->
            preferences[MAX_CONCURRENT_JOBS] = jobs
        }
    }
}