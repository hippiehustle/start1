package com.kaos.evcryptoscanner.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kaos_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val BASE_URL = stringPreferencesKey("base_url")
        val API_KEY = stringPreferencesKey("api_key")
        val AUTO_REFRESH_INTERVAL = intPreferencesKey("auto_refresh_interval")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val SMART_NOTIFICATIONS = booleanPreferencesKey("smart_notifications")
        val FCM_TOKEN = stringPreferencesKey("fcm_token")
        val LAST_SCAN_TIMESTAMP = longPreferencesKey("last_scan_timestamp")
        val CACHED_SCAN_DATA = stringPreferencesKey("cached_scan_data")
    }

    val baseUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[BASE_URL] ?: ""
    }

    val apiKey: Flow<String> = dataStore.data.map { preferences ->
        preferences[API_KEY] ?: ""
    }

    val autoRefreshInterval: Flow<Int> = dataStore.data.map { preferences ->
        preferences[AUTO_REFRESH_INTERVAL] ?: 0 // 0 = off
    }

    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED] ?: false
    }

    val smartNotifications: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SMART_NOTIFICATIONS] ?: true
    }

    val fcmToken: Flow<String> = dataStore.data.map { preferences ->
        preferences[FCM_TOKEN] ?: ""
    }

    val cachedScanData: Flow<String?> = dataStore.data.map { preferences ->
        preferences[CACHED_SCAN_DATA]
    }

    val lastScanTimestamp: Flow<Long> = dataStore.data.map { preferences ->
        preferences[LAST_SCAN_TIMESTAMP] ?: 0L
    }

    suspend fun setBaseUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[BASE_URL] = url
        }
    }

    suspend fun setApiKey(key: String) {
        dataStore.edit { preferences ->
            preferences[API_KEY] = key
        }
    }

    suspend fun setAutoRefreshInterval(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[AUTO_REFRESH_INTERVAL] = minutes
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setSmartNotifications(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SMART_NOTIFICATIONS] = enabled
        }
    }

    suspend fun setFcmToken(token: String) {
        dataStore.edit { preferences ->
            preferences[FCM_TOKEN] = token
        }
    }

    suspend fun setCachedScanData(data: String) {
        dataStore.edit { preferences ->
            preferences[CACHED_SCAN_DATA] = data
            preferences[LAST_SCAN_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun clearCache() {
        dataStore.edit { preferences ->
            preferences.remove(CACHED_SCAN_DATA)
            preferences.remove(LAST_SCAN_TIMESTAMP)
        }
    }
}
