package com.example.safetrack.session.presentation

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetrack.core.domain.repository.SessionRepository
import com.example.safetrack.core.domain.model.Session
import com.example.safetrack.auth.domain.repository.AuthRepository
import com.example.safetrack.core.domain.model.ParticipantStatus
import com.example.safetrack.core.domain.model.SessionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class SessionDetailsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionDetailsUiState>(SessionDetailsUiState.Loading)
    val uiState: StateFlow<SessionDetailsUiState> = _uiState.asStateFlow()

    private var shareIntent: Intent? = null
    private var sessionId: String? = null

    init {
        // Get sessionId from savedStateHandle
        sessionId = savedStateHandle.get<String>("sessionId")
        if (sessionId != null) {
            loadSession(sessionId!!)
        } else {
            _uiState.value = SessionDetailsUiState.Error("Session ID not provided")
        }
    }

    private fun loadSession(sessionId: String) {
        viewModelScope.launch {
            try {
                sessionRepository.getSession(sessionId)
                    .onSuccess { session ->
                        _uiState.value = SessionDetailsUiState.Success(session = session)
                    }
                    .onFailure { error ->
                        _uiState.value = SessionDetailsUiState.Error(error.message ?: "Failed to load session")
                    }
            } catch (e: Exception) {
                _uiState.value = SessionDetailsUiState.Error(e.message ?: "Failed to load session")
            }
        }
    }

    fun getCurrentUserId(): String {
        return authRepository.currentUser?.uid ?: throw IllegalStateException("User not logged in")
    }

    fun deleteSession() {
        val currentState = _uiState.value as? SessionDetailsUiState.Success ?: return
        viewModelScope.launch {
            try {
                sessionRepository.deleteSession(currentState.session.id)
                _uiState.value = SessionDetailsUiState.Success(
                    session = currentState.session.copy(isActive = false),
                    shareIntent = null
                )
            } catch (e: Exception) {
                _uiState.value = SessionDetailsUiState.Error(e.message ?: "Failed to delete session")
            }
        }
    }

    fun removeJoinedSession() {
        val currentState = _uiState.value as? SessionDetailsUiState.Success ?: return
        viewModelScope.launch {
            try {
                // First leave the session
                sessionRepository.leaveSession(currentState.session.id)
                
                // Then update the local state to show the session as left
                val updatedParticipants = currentState.session.participants.map { participant ->
                    if (participant.id == getCurrentUserId()) {
                        participant.copy(status = ParticipantStatus.LEFT)
                    } else {
                        participant
                    }
                }
                
                _uiState.value = SessionDetailsUiState.Success(
                    session = currentState.session.copy(
                        participants = updatedParticipants
                    )
                )
            } catch (e: Exception) {
                _uiState.value = SessionDetailsUiState.Error(e.message ?: "Failed to remove session")
            }
        }
    }

    fun leaveSession() {
        val currentState = _uiState.value as? SessionDetailsUiState.Success ?: return
        viewModelScope.launch {
            try {
                // First leave the session
                sessionRepository.leaveSession(currentState.session.id)
                
                // Then update the local state to show the session as left
                val updatedParticipants = currentState.session.participants.map { participant ->
                    if (participant.id == getCurrentUserId()) {
                        participant.copy(status = ParticipantStatus.LEFT)
                    } else {
                        participant
                    }
                }
                
                _uiState.value = SessionDetailsUiState.Success(
                    session = currentState.session.copy(
                        participants = updatedParticipants
                    )
                )
            } catch (e: Exception) {
                _uiState.value = SessionDetailsUiState.Error(e.message ?: "Failed to leave session")
            }
        }
    }

    fun toggleSessionActive() {
        val currentState = _uiState.value as? SessionDetailsUiState.Success ?: return
        viewModelScope.launch {
            try {
                if (currentState.session.isActive) {
                    sessionRepository.endSession(currentState.session.id)
                } else {
                    sessionRepository.updateSession(
                        sessionId = currentState.session.id,
                        session = currentState.session.copy(isActive = true)
                    )
                }
                _uiState.value = currentState.copy(
                    session = currentState.session.copy(isActive = !currentState.session.isActive)
                )
            } catch (e: Exception) {
                _uiState.value = SessionDetailsUiState.Error(e.message ?: "Failed to update session status")
            }
        }
    }

    fun shareSession() {
        val currentState = _uiState.value as? SessionDetailsUiState.Success ?: return
        val session = currentState.session
        
        val shareText = buildString {
            append("Join my SafeTrack session!\n")
            append("Session: ${session.title}\n")
            append("Code: ${session.id}\n")
            if (session.password?.isNotBlank() == true) {
                append("Password: ${session.password}\n")
            }
        }

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        _uiState.value = SessionDetailsUiState.Success(
            session = session,
            shareIntent = intent
        )
    }

    fun clearShareIntent() {
        val currentState = _uiState.value as? SessionDetailsUiState.Success ?: return
        _uiState.value = currentState.copy(shareIntent = null)
    }

    fun rejoinSession() {
        val currentState = _uiState.value as? SessionDetailsUiState.Success ?: return
        viewModelScope.launch {
            try {
                sessionRepository.rejoinSession(currentState.session.id)
                loadSession(currentState.session.id) // Reload to get updated state
            } catch (e: Exception) {
                _uiState.value = SessionDetailsUiState.Error(e.message ?: "Failed to rejoin session")
            }
        }
    }
}

sealed class SessionDetailsUiState {
    object Loading : SessionDetailsUiState()
    data class Success(
        val session: Session,
        val shareIntent: Intent? = null
    ) : SessionDetailsUiState()
    data class Error(val error: String) : SessionDetailsUiState()
} 