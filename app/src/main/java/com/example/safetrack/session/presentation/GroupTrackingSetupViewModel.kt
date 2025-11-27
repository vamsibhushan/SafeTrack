package com.example.safetrack.session.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetrack.auth.domain.repository.AuthRepository
import com.example.safetrack.core.domain.repository.SessionRepository
import com.example.safetrack.core.domain.model.SessionType
import com.example.safetrack.core.domain.model.SessionSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed class GroupTrackingSetupUiState {
    object Initial : GroupTrackingSetupUiState()
    object Loading : GroupTrackingSetupUiState()
    data class Editing(
        val title: String = "",
        val displayName: String = "",
        val description: String = "",
        val password: String = "",
        val locationSharingEnabled: Boolean = true,
        val radiusLimit: Double? = null,
        val maxParticipants: Int? = null,
        val startNow: Boolean = true,
        val scheduledStart: String? = null,
        val duration: String? = null,
        val error: String? = null
    ) : GroupTrackingSetupUiState()
    data class Success(val sessionId: String) : GroupTrackingSetupUiState()
}

@HiltViewModel
class GroupTrackingSetupViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupTrackingSetupUiState>(GroupTrackingSetupUiState.Initial)
    val uiState: StateFlow<GroupTrackingSetupUiState> = _uiState.asStateFlow()

    init {
        // Initialize with default values
        _uiState.value = GroupTrackingSetupUiState.Editing(
            title = "Group Session",
            displayName = authRepository.currentUser?.displayName ?: "",
            locationSharingEnabled = true,
            description = "",
            password = "",
            radiusLimit = 100.0,
            maxParticipants = 10,
            startNow = true,
            scheduledStart = null,
            duration = "60",
            error = null
        )
    }

    fun updateTitle(title: String) {
        val currentState = _uiState.value as? GroupTrackingSetupUiState.Editing ?: return
        _uiState.value = currentState.copy(title = title, error = null)
    }

    fun updateDisplayName(name: String) {
        val currentState = _uiState.value as? GroupTrackingSetupUiState.Editing ?: return
        _uiState.value = currentState.copy(
            displayName = name.ifBlank { authRepository.currentUser?.displayName ?: "" },
            error = null
        )
    }

    fun updateDescription(description: String) {
        val currentState = _uiState.value as? GroupTrackingSetupUiState.Editing ?: return
        _uiState.value = currentState.copy(description = description, error = null)
    }

    fun updatePassword(password: String) {
        val currentState = _uiState.value as? GroupTrackingSetupUiState.Editing ?: return
        _uiState.value = currentState.copy(password = password, error = null)
    }

    fun updateMaxParticipants(count: Int?) {
        val currentState = _uiState.value as? GroupTrackingSetupUiState.Editing ?: return
        _uiState.value = currentState.copy(maxParticipants = count, error = null)
    }

    fun updateLocationSharing(enabled: Boolean) {
        val currentState = _uiState.value as? GroupTrackingSetupUiState.Editing ?: return
        _uiState.value = currentState.copy(
            locationSharingEnabled = enabled,
            radiusLimit = if (!enabled) null else currentState.radiusLimit,
            error = null
        )
    }

    fun updateRadiusLimit(radius: Double?) {
        val currentState = _uiState.value as? GroupTrackingSetupUiState.Editing ?: return
        _uiState.value = currentState.copy(radiusLimit = radius, error = null)
    }

    fun updateStartNow(startNow: Boolean) {
        val currentState = _uiState.value as? GroupTrackingSetupUiState.Editing ?: return
        _uiState.value = currentState.copy(
            startNow = startNow,
            scheduledStart = if (startNow) null else currentState.scheduledStart,
            error = null
        )
    }

    fun updateScheduledStart(time: String) {
        val currentState = _uiState.value as? GroupTrackingSetupUiState.Editing ?: return
        _uiState.value = currentState.copy(scheduledStart = time, error = null)
    }

    fun updateDuration(duration: String) {
        val currentState = _uiState.value as? GroupTrackingSetupUiState.Editing ?: return
        _uiState.value = currentState.copy(duration = duration, error = null)
    }

    fun createSession() {
        val currentState = _uiState.value as? GroupTrackingSetupUiState.Editing ?: return
        
        if (currentState.title.isBlank()) {
            _uiState.value = currentState.copy(error = "Title cannot be empty")
            return
        }

        if (currentState.displayName.isBlank()) {
            _uiState.value = currentState.copy(error = "Display name cannot be empty")
            return
        }

        if (currentState.radiusLimit != null && currentState.radiusLimit <= 10) {
            _uiState.value = currentState.copy(error = "Radius must be greater than 10 meters")
            return
        }

        viewModelScope.launch {
            _uiState.value = GroupTrackingSetupUiState.Loading
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
                type = SessionType.GROUP,
                password = currentState.password,
                description = currentState.description,
                radiusLimit = currentState.radiusLimit,
                adminName = adminName,
                maxParticipants = currentState.maxParticipants,
                settings = settings
            ).fold(
                onSuccess = { session ->
                    Timber.d("Group session created: ${session.id}")
                    _uiState.value = GroupTrackingSetupUiState.Success(session.id)
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to create group session")
                    _uiState.value = currentState.copy(
                        error = error.message ?: "Failed to create session"
                    )
                }
            )
        }
    }
}