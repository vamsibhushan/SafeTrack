package com.example.safetrack.settings.presentation

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetrack.auth.domain.repository.AuthRepository
import com.example.safetrack.core.util.LocationPermissionUtil
import com.example.safetrack.settings.data.UserPreferences
import com.example.safetrack.settings.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.collect { preferences ->
                val currentState = _uiState.value
                if (currentState is SettingsUiState.Success) {
                    // Only update UI state if it's not already in the desired state
                    if (currentState.isLocationTrackingEnabled != preferences.locationTrackingEnabled ||
                        currentState.isDarkTheme != preferences.isDarkTheme) {
                        updateLocationState(preferences)
                    }
                } else {
                    updateLocationState(preferences)
                }
            }
        }
    }

    private suspend fun updateLocationState(preferences: UserPreferences) {
        val locationEnabled = LocationPermissionUtil.isLocationEnabled(context)
        val hasPermissions = LocationPermissionUtil.hasLocationPermissions(context)
        
        // If location is disabled in device settings or permissions are revoked, 
        // automatically disable tracking
        if (preferences.locationTrackingEnabled && (!locationEnabled || !hasPermissions)) {
            userPreferencesRepository.updateLocationTrackingEnabled(false)
            _uiState.value = SettingsUiState.Success(
                isDarkTheme = preferences.isDarkTheme,
                isLocationTrackingEnabled = false,
                showLocationWarning = true,
                isDeviceLocationEnabled = locationEnabled,
                hasLocationPermissions = hasPermissions
            )
            return
        }
        
        _uiState.value = SettingsUiState.Success(
            isDarkTheme = preferences.isDarkTheme,
            isLocationTrackingEnabled = preferences.locationTrackingEnabled,
            showLocationWarning = preferences.locationTrackingEnabled && (!locationEnabled || !hasPermissions),
            isDeviceLocationEnabled = locationEnabled,
            hasLocationPermissions = hasPermissions
        )
    }

    fun toggleDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            // Update UI state immediately
            _uiState.value = (_uiState.value as? SettingsUiState.Success)?.copy(
                isDarkTheme = enabled
            ) ?: return@launch
            
            // Update the actual theme state
            userPreferencesRepository.updateDarkTheme(enabled)
        }
    }

    fun toggleLocationTracking(enabled: Boolean) {
        viewModelScope.launch {
            val locationEnabled = LocationPermissionUtil.isLocationEnabled(context)
            val hasPermissions = LocationPermissionUtil.hasLocationPermissions(context)
            
            // Update UI state immediately
            _uiState.value = (_uiState.value as? SettingsUiState.Success)?.copy(
                isLocationTrackingEnabled = enabled,
                showLocationWarning = enabled && (!locationEnabled || !hasPermissions),
                isDeviceLocationEnabled = locationEnabled,
                hasLocationPermissions = hasPermissions
            ) ?: return@launch
            
            if (enabled && (!locationEnabled || !hasPermissions)) {
                // If we can't enable tracking, revert the UI state
                _uiState.value = (_uiState.value as? SettingsUiState.Success)?.copy(
                    isLocationTrackingEnabled = false,
                    showLocationWarning = true,
                    isDeviceLocationEnabled = locationEnabled,
                    hasLocationPermissions = hasPermissions
                ) ?: return@launch
                return@launch
            }
            
            // Update the actual tracking state
            userPreferencesRepository.updateLocationTrackingEnabled(enabled)
        }
    }

    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                // Clear any active sessions or preferences if needed
                userPreferencesRepository.updateLastActiveSessionId(null)
                userPreferencesRepository.updateLocationTrackingEnabled(false)
                // Sign out from Firebase
                authRepository.signOut()
            } catch (e: Exception) {
                Timber.e(e, "Error during sign out")
            }
        }
    }

    fun getCurrentUserId(): String? {
        return authRepository.currentUser?.uid
    }
}

sealed class SettingsUiState {
    object Loading : SettingsUiState()
    data class Success(
        val isDarkTheme: Boolean = false,
        val isLocationTrackingEnabled: Boolean = false,
        val showLocationWarning: Boolean = false,
        val isDeviceLocationEnabled: Boolean = false,
        val hasLocationPermissions: Boolean = false
    ) : SettingsUiState()
}