package com.example.safetrack.core.domain.repository

import com.example.safetrack.core.domain.model.Session
import com.example.safetrack.core.domain.model.Participant
import com.example.safetrack.core.domain.model.SessionType
import com.example.safetrack.core.domain.model.SessionSettings
import com.example.safetrack.core.domain.model.ParticipantStatus
import com.example.safetrack.core.domain.model.ParticipantRole
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getSessions(): Flow<List<Session>>
    suspend fun createSession(
        title: String,
        type: SessionType,
        password: String,
        description: String = "",
        radiusLimit: Double? = null,
        adminName: String,
        locationSharingEnabled: Boolean = true,
        maxParticipants: Int? = null,
        tags: List<String> = emptyList(),
        settings: SessionSettings = SessionSettings()
    ): Result<Session>
    suspend fun updateSession(sessionId: String, session: Session): Result<Unit>
    suspend fun endSession(sessionId: String): Result<Unit>
    suspend fun deleteSession(sessionId: String): Result<Unit>
    suspend fun joinSession(
        code: String, 
        password: String,
        participantName: String? = null
    ): Result<Session>
    suspend fun leaveSession(sessionId: String): Result<Unit>
    suspend fun rejoinSession(sessionId: String): Result<Unit>
    suspend fun updateParticipantLocation(
        sessionId: String,
        participantId: String,
        location: GeoPoint
    ): Result<Unit>
    suspend fun updateAdminLocation(
        sessionId: String,
        location: GeoPoint
    ): Result<Unit>
    suspend fun updateParticipantStatus(
        sessionId: String,
        participantId: String,
        status: ParticipantStatus
    ): Result<Unit>
    suspend fun updateParticipantRole(
        sessionId: String,
        participantId: String,
        role: ParticipantRole
    ): Result<Unit>
    fun getParticipantsFlow(sessionId: String): Flow<List<Participant>>
    suspend fun getSession(sessionId: String): Result<Session>
    suspend fun searchSessions(query: String): Result<List<Session>>
    suspend fun getActiveSessions(): Result<List<Session>>
    suspend fun getRecentSessions(limit: Int = 10): Result<List<Session>>
}