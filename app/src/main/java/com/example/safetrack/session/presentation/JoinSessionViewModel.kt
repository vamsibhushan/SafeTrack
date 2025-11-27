package com.example.safetrack.session.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetrack.core.domain.repository.SessionRepository
import com.example.safetrack.core.domain.model.Session
import com.example.safetrack.auth.domain.repository.AuthRepository
import com.example.safetrack.core.domain.model.SessionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JoinSessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<JoinSessionUiState>(JoinSessionUiState.Initial)
    val uiState: StateFlow<JoinSessionUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = JoinSessionUiState.Editing(
            displayName = authRepository.currentUser?.displayName ?: "",
            selectedType = SessionType.SOLO,
            locationSharingEnabled = false
        )
    }

    fun updateDisplayName(name: String) {
        val currentState = _uiState.value as? JoinSessionUiState.Editing ?: return
        _uiState.value = currentState.copy(displayName = name, error = null)
    }

    fun updateCode(code: String) {
        val currentState = _uiState.value as? JoinSessionUiState.Editing ?: return
        _uiState.value = currentState.copy(code = code, error = null)
    }

    fun updatePassword(password: String) {
        val currentState = _uiState.value as? JoinSessionUiState.Editing ?: return
        _uiState.value = currentState.copy(password = password, error = null)
    }

    fun updateSelectedType(type: SessionType) {
        val currentState = _uiState.value as? JoinSessionUiState.Editing ?: return
        _uiState.value = currentState.copy(
            selectedType = type,
            locationSharingEnabled = if (type == SessionType.SOLO) false else currentState.locationSharingEnabled,
            error = null
        )
    }

    fun updateLocationSharing(enabled: Boolean) {
        val currentState = _uiState.value as? JoinSessionUiState.Editing ?: return
        _uiState.value = currentState.copy(locationSharingEnabled = enabled, error = null)
    }

    fun joinSession() {
        val currentState = _uiState.value as? JoinSessionUiState.Editing ?: return
        
        if (currentState.code.isBlank()) {
            _uiState.value = currentState.copy(error = "Please enter a session code")
            return
        }

        if (currentState.displayName.isBlank()) {
            _uiState.value = currentState.copy(error = "Please enter your display name")
            return
        }

        viewModelScope.launch {
            _uiState.value = JoinSessionUiState.Loading
            try {
                sessionRepository.joinSession(
                    code = currentState.code,
                    password = currentState.password,
                    participantName = currentState.displayName
                ).onSuccess { session ->
                    _uiState.value = JoinSessionUiState.Success(session)
                }.onFailure { error ->
                    _uiState.value = currentState.copy(error = error.message ?: "Failed to join session")
                }
            } catch (e: Exception) {
                _uiState.value = currentState.copy(error = e.message ?: "Failed to join session")
            }
        }
    }
}

sealed class JoinSessionUiState {
    object Initial : JoinSessionUiState()
    object Loading : JoinSessionUiState()
    data class Success(val session: Session) : JoinSessionUiState()
    data class Editing(
        val displayName: String = "",
        val code: String = "",
        val password: String = "",
        val selectedType: SessionType = SessionType.SOLO,
        val locationSharingEnabled: Boolean = false,
        val error: String? = null
    ) : JoinSessionUiState()
} 