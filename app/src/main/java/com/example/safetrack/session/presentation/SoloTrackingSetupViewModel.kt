package com.example.safetrack.session.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetrack.auth.domain.repository.AuthRepository
import com.example.safetrack.core.domain.model.SessionSettings
import com.example.safetrack.core.domain.repository.SessionRepository
import com.example.safetrack.core.domain.model.SessionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class SoloTrackingSetupViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SoloTrackingSetupUiState>(SoloTrackingSetupUiState.Initial)
    val uiState: StateFlow<SoloTrackingSetupUiState> = _uiState.asStateFlow()

    init {
        // Initialize with default values
        _uiState.value = SoloTrackingSetupUiState.Editing(
            title = "Solo Session",
            displayName = authRepository.currentUser?.displayName ?: ""
        )
    }

    fun updateTitle(title: String) {
        val currentState = _uiState.value as? SoloTrackingSetupUiState.Editing ?: return
        _uiState.value = currentState.copy(title = title.ifBlank { "Solo Session" }, error = null)
    }

    fun updateDisplayName(name: String) {
        val currentState = _uiState.value as? SoloTrackingSetupUiState.Editing ?: return
        _uiState.value = currentState.copy(
            displayName = name.ifBlank { authRepository.currentUser?.displayName ?: "" },
            error = null
        )
    }

    fun updateDescription(description: String) {
        val currentState = _uiState.value as? SoloTrackingSetupUiState.Editing ?: return
        _uiState.value = currentState.copy(description = description, error = null)
    }

    fun updatePassword(password: String) {
        val currentState = _uiState.value as? SoloTrackingSetupUiState.Editing ?: return
        _uiState.value = currentState.copy(password = password, error = null)
    }

    fun updateLocationSharing(enabled: Boolean) {
        val currentState = _uiState.value as? SoloTrackingSetupUiState.Editing ?: return
        _uiState.value = currentState.copy(
            locationSharingEnabled = enabled,
            radiusLimit = if (!enabled) null else currentState.radiusLimit,
            error = null
        )
    }

    fun updateRadiusLimit(limit: Double?) {
        val currentState = _uiState.value as? SoloTrackingSetupUiState.Editing ?: return
        _uiState.value = currentState.copy(radiusLimit = limit, error = null)
    }

    fun updateStartNow(startNow: Boolean) {
        val currentState = _uiState.value as? SoloTrackingSetupUiState.Editing ?: return
        _uiState.value = currentState.copy(
            startNow = startNow,
            scheduledStart = if (startNow) null else currentState.scheduledStart,
            error = null
        )
    }

    fun updateScheduledStart(time: String) {
        val currentState = _uiState.value as? SoloTrackingSetupUiState.Editing ?: return
        _uiState.value = currentState.copy(scheduledStart = time, error = null)
    }

    fun updateDuration(duration: String) {
        val currentState = _uiState.value as? SoloTrackingSetupUiState.Editing ?: return
        _uiState.value = currentState.copy(duration = duration, error = null)
    }

    fun createSession() {
        val currentState = _uiState.value as? SoloTrackingSetupUiState.Editing ?: return
        
        if (currentState.title.isBlank()) {
            _uiState.value = currentState.copy(error = "Title cannot be empty")
            return
        }

        if (currentState.displayName.isBlank()) {
            _uiState.value = currentState.copy(error = "Display name cannot be empty")
            return
        }

        if (currentState.locationSharingEnabled && currentState.radiusLimit != null && currentState.radiusLimit <= 0) {
            _uiState.value = currentState.copy(error = "Radius limit must be greater than 0")
            return
        }

        viewModelScope.launch {
            _uiState.value = SoloTrackingSetupUiState.Loading
            val adminName = currentState.displayName.ifBlank { 
                authRepository.currentUser?.displayName ?: "Anonymous" 
            }
            val adminId = authRepository.currentUser?.uid ?: run {
                _uiState.value = currentState.copy(error = "User not authenticated")
                return@launch
            }

            val durationInMinutes = currentState.duration?.toIntOrNull()
            val settings = SessionSettings(
                autoEndSession = durationInMinutes != null,
                autoEndDuration = durationInMinutes?.let { it * 60 * 1000L }
            )

            sessionRepository.createSession(
                title = currentState.title,
                type = SessionType.SOLO,
                password = currentState.password,
                description = currentState.description,
                radiusLimit = if (currentState.locationSharingEnabled) currentState.radiusLimit else null,
                adminName = adminName,
                locationSharingEnabled = currentState.locationSharingEnabled,
                settings = settings
            ).fold(
                onSuccess = { session ->
                    Timber.d("Solo session created: ${session.id}")
                    _uiState.value = SoloTrackingSetupUiState.Success(session.id)
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to create solo session")
                    _uiState.value = currentState.copy(
                        error = error.message ?: "Failed to create session"
                    )
                }
            )
        }
    }
}

sealed class SoloTrackingSetupUiState {
    object Initial : SoloTrackingSetupUiState()
    object Loading : SoloTrackingSetupUiState()
    data class Editing(
        val title: String = "",
        val displayName: String = "",
        val description: String = "",
        val password: String = "",
        val locationSharingEnabled: Boolean = true,
        val radiusLimit: Double? = 100.0,
        val startNow: Boolean = true,
        val scheduledStart: String? = null,
        val duration: String? = null,
        val error: String? = null
    ) : SoloTrackingSetupUiState()
    data class Success(val sessionId: String) : SoloTrackingSetupUiState()
}