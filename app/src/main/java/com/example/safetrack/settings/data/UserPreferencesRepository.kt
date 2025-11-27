package com.example.safetrack.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
        val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        val LOCATION_TRACKING_ENABLED_KEY = booleanPreferencesKey("location_tracking_enabled")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_PHONE_KEY = stringPreferencesKey("user_phone")
        val PROFILE_PICTURE_URI_KEY = stringPreferencesKey("profile_picture_uri")
        val LAST_ACTIVE_SESSION_ID = stringPreferencesKey("last_active_session_id")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { e ->
            if (e is IOException) {
                emit(emptyPreferences())
            } else {
                throw e
            }
        }
        .map { preferences ->
            UserPreferences(
                isDarkTheme = preferences[DARK_THEME_KEY] ?: false,
                notificationsEnabled = preferences[NOTIFICATIONS_ENABLED_KEY] ?: true,
                locationTrackingEnabled = preferences[LOCATION_TRACKING_ENABLED_KEY] ?: false,
                userName = preferences[USER_NAME_KEY] ?: "",
                userPhone = preferences[USER_PHONE_KEY] ?: "",
                profilePictureUri = preferences[PROFILE_PICTURE_URI_KEY],
                lastActiveSessionId = preferences[LAST_ACTIVE_SESSION_ID]
            )
        }

    suspend fun updateDarkTheme(isDarkTheme: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_THEME_KEY] = isDarkTheme
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    suspend fun updateLocationTrackingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[LOCATION_TRACKING_ENABLED_KEY] = enabled
        }
    }

    suspend fun updateUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
        }
    }

    suspend fun updateUserPhone(phone: String) {
        dataStore.edit { preferences ->
            preferences[USER_PHONE_KEY] = phone
        }
    }

    suspend fun updateProfilePictureUri(uri: String) {
        dataStore.edit { preferences ->
            preferences[PROFILE_PICTURE_URI_KEY] = uri
        }
    }

    suspend fun updateLastActiveSessionId(sessionId: String?) {
        dataStore.edit { preferences ->
            if (sessionId != null) {
                preferences[LAST_ACTIVE_SESSION_ID] = sessionId
            } else {
                preferences.remove(LAST_ACTIVE_SESSION_ID)
            }
        }
    }
}

data class UserPreferences(
    val isDarkTheme: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val locationTrackingEnabled: Boolean = false,
    val userName: String = "",
    val userPhone: String = "",
    val profilePictureUri: String? = null,
    val lastActiveSessionId: String? = null
)