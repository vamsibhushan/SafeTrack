package com.example.safetrack.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetrack.auth.domain.repository.AuthRepository
import com.example.safetrack.core.domain.model.ParticipantStatus
import com.example.safetrack.core.domain.model.Session
import com.example.safetrack.core.domain.model.SessionType
import com.example.safetrack.core.domain.repository.SessionRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = authRepository.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    init {
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            try {
                sessionRepository.getSessions()
                    .onEach { sessions ->
                        val mySessions = sessions.filter { it.adminId == currentUserId }
                        val joinedSessions = sessions.filter { session -> 
                            session.adminId != currentUserId && 
                            session.participants.any { 
                                it.id == currentUserId && 
                                it.status == ParticipantStatus.ACTIVE 
                            }
                        }
                        
                        _uiState.value = HomeUiState.Success(
                            mySessions = mySessions.sortedByDescending { it.startTime },
                            joinedSessions = joinedSessions.sortedByDescending { it.startTime }
                        )
                    }
                    .catch { e ->
                        Timber.e(e, "Error loading sessions")
                        _uiState.value = HomeUiState.Error(e.message ?: "Failed to load sessions")
                    }
                    .collect()
            } catch (e: Exception) {
                Timber.e(e, "Error loading sessions")
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to load sessions")
            }
        }
    }

    fun retryLoadingSessions() {
        loadSessions()
    }

    fun toggleSessionActive(sessionId: String) {
        viewModelScope.launch {
            try {
                val session = sessionRepository.getSession(sessionId).getOrThrow()
                if (session.isActive) {
                    sessionRepository.endSession(sessionId)
                } else {
                    sessionRepository.updateSession(
                        sessionId = sessionId,
                        session = session.copy(isActive = true)
                    )
                }
            } catch (e: Exception) {
                // Optionally update UI with error, but keep it simple for now
                Timber.e(e, "Failed to toggle session $sessionId")
            }
        }
    }

    fun shareSession(session: Session) {
        viewModelScope.launch {
            try {
                val shareText = buildString {
                    append("Join my SafeTrack session!\n")
                    append("Title: ${session.title}\n")
                    append("Code: ${session.id}\n")
                    if (session.password.isNotEmpty()) {
                        append("Password: ${session.password}\n")
                    }
                    append("Type: ${session.type}\n")
                    if (session.type == SessionType.GROUP) {
                        append("Max Participants: ${session.maxParticipants ?: "Unlimited"}\n")
                    }
                }
                // The actual sharing will be handled by the UI layer
                _uiState.value = (_uiState.value as? HomeUiState.Success)?.copy(
                    shareText = shareText,
                    shareSession = session
                ) ?: return@launch
            } catch (e: Exception) {
                Timber.e(e, "Failed to prepare session sharing")
            }
        }
    }

    fun removeJoinedSession(session: Session) {
        viewModelScope.launch {
            try {
                sessionRepository.leaveSession(session.id)
                // The session list will be automatically updated through the flow
            } catch (e: Exception) {
                Timber.e(e, "Failed to leave session ${session.id}")
                _uiState.value = HomeUiState.Error("Failed to leave session")
            }
        }
    }

    fun clearShareSession() {
        _uiState.value = (_uiState.value as? HomeUiState.Success)?.copy(
            shareText = null,
            shareSession = null
        ) ?: return
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val mySessions: List<Session> = emptyList(),
        val joinedSessions: List<Session> = emptyList(),
        val shareText: String? = null,
        val shareSession: Session? = null
    ) : HomeUiState()
    data class Error(val error: String) : HomeUiState()
}