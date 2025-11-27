package com.example.safetrack.core.data.repository

import com.example.safetrack.auth.domain.repository.AuthRepository
import com.example.safetrack.core.domain.model.Session
import com.example.safetrack.core.domain.model.Participant
import com.example.safetrack.core.domain.model.ParticipantRole
import com.example.safetrack.core.domain.model.ParticipantStatus
import com.example.safetrack.core.domain.model.SessionSettings
import com.example.safetrack.core.domain.model.SessionType
import com.example.safetrack.core.domain.repository.SessionRepository
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : SessionRepository {

    init {
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
    }

    private val currentUser
        get() = authRepository.currentUser ?: throw IllegalStateException("User must be logged in to fetch sessions")

    override fun getSessions(): Flow<List<Session>> = callbackFlow {
        // Query for sessions where user is either admin or participant
        val sessionsRef = firestore.collection("sessions")
        val listener = sessionsRef
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Failed to fetch sessions for user ${currentUser.uid}")
                    close(error)
                    return@addSnapshotListener
                }

                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val session = doc.toObject(Session::class.java)?.copy(id = doc.id)
                        // Include session if user is admin or participant
                        if (session != null && (
                            session.adminId == currentUser.uid ||
                            session.participants.any { it.id == currentUser.uid }
                        )) {
                            session
                        } else null
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to parse session: ${doc.id}")
                        null
                    }
                } ?: emptyList()

                Timber.d("Fetched ${sessions.size} total sessions for user ${currentUser.uid}")
                trySend(sessions)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createSession(
        title: String,
        type: SessionType,
        password: String,
        description: String,
        radiusLimit: Double?,
        adminName: String,
        locationSharingEnabled: Boolean,
        maxParticipants: Int?,
        tags: List<String>,
        settings: SessionSettings
    ): Result<Session> = runCatching {
        val sessionId = generateSessionCode()
        val session = Session(
            id = sessionId,
            title = title,
            type = type,
            password = password,
            description = description,
            adminId = currentUser.uid,
            adminEmail = currentUser.email ?: "",
            adminName = adminName,
            radiusLimit = radiusLimit,
            locationSharingEnabled = locationSharingEnabled,
            maxParticipants = maxParticipants,
            tags = tags,
            settings = settings,
            isActive = true,
            startTime = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        try {
            firestore.collection("sessions")
                .document(sessionId)
                .set(session)
                .await()
            
            Timber.d("Session created successfully: $sessionId")
            session
        } catch (e: Exception) {
            Timber.e(e, "Failed to create session")
            throw e
        }
    }

    override suspend fun updateSession(sessionId: String, session: Session): Result<Unit> = runCatching {
        if (session.adminId != currentUser.uid) {
            throw IllegalStateException("Only session admin can update the session")
        }
        firestore.collection("sessions").document(sessionId).set(session).await()
    }

    override suspend fun endSession(sessionId: String): Result<Unit> = runCatching {
        val sessionRef = firestore.collection("sessions").document(sessionId)
        val session = sessionRef.get().await().toObject(Session::class.java)
            ?: throw IllegalStateException("Session not found")

        if (session.adminId != currentUser.uid) {
            throw IllegalStateException("Only session admin can end the session")
        }

        sessionRef.update(
            mapOf(
                "isActive" to false,
                "endTime" to System.currentTimeMillis()
            )
        ).await()
    }

    override suspend fun deleteSession(sessionId: String): Result<Unit> = runCatching {
        val sessionRef = firestore.collection("sessions").document(sessionId)
        val session = sessionRef.get().await().toObject(Session::class.java)
            ?: throw IllegalStateException("Session not found")

        if (session.adminId != currentUser.uid) {
            throw IllegalStateException("Only session admin can delete the session")
        }

        sessionRef.delete().await()
    }

    override suspend fun joinSession(
        code: String,
        password: String,
        participantName: String?
    ): Result<Session> = runCatching {
        val sessionDoc = firestore.collection("sessions")
            .document(code)
            .get()
            .await()
            .toObject(Session::class.java) ?: throw Exception("Session not found")

        if (sessionDoc.password.isNotEmpty() && sessionDoc.password != password) {
            throw Exception("Invalid password")
        }

        val participant = Participant(
            id = currentUser.uid,
            name = participantName ?: currentUser.displayName ?: "Anonymous"
        )

        firestore.collection("sessions")
            .document(code)
            .update("participants", FieldValue.arrayUnion(participant))
            .await()

        sessionDoc.copy(participants = sessionDoc.participants + participant)
    }

    override suspend fun leaveSession(sessionId: String): Result<Unit> = runCatching {
        val session = getSession(sessionId).getOrThrow()

        if (session.adminId == currentUser.uid) {
            endSession(sessionId)
        } else {
            val participant = Participant(
                id = currentUser.uid,
                name = currentUser.displayName ?: "Anonymous"
            )
            firestore.collection("sessions")
                .document(sessionId)
                .update("participants", FieldValue.arrayRemove(participant))
                .await()
        }
    }

    override suspend fun updateParticipantLocation(
        sessionId: String,
        participantId: String,
        location: GeoPoint
    ): Result<Unit> = runCatching {
        val session = getSession(sessionId).getOrThrow()
        val updatedParticipants = session.participants.map { participant ->
            if (participant.id == participantId) {
                participant.copy(location = location, lastUpdated = System.currentTimeMillis())
            } else {
                participant
            }
        }
        firestore.collection("sessions")
            .document(sessionId)
            .update("participants", updatedParticipants)
            .await()
    }

    override suspend fun updateAdminLocation(
        sessionId: String,
        location: GeoPoint
    ): Result<Unit> = runCatching {
        firestore.collection("sessions")
            .document(sessionId)
            .update("adminLocation", location)
            .await()
    }

    override fun getParticipantsFlow(sessionId: String): Flow<List<Participant>> = callbackFlow {
        val listener = firestore.collection("sessions")
            .document(sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val participants = snapshot?.toObject(Session::class.java)?.participants ?: emptyList()
                trySend(participants)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getSession(sessionId: String): Result<Session> = runCatching {
        firestore.collection("sessions")
            .document(sessionId)
            .get()
            .await()
            .toObject(Session::class.java) ?: throw Exception("Session not found")
    }

    override suspend fun updateParticipantStatus(
        sessionId: String,
        participantId: String,
        status: ParticipantStatus
    ): Result<Unit> = runCatching {
        val session = getSession(sessionId).getOrThrow()
        val updatedParticipants = session.participants.map { participant ->
            if (participant.id == participantId) {
                participant.copy(
                    status = status,
                    lastUpdated = System.currentTimeMillis()
                )
            } else {
                participant
            }
        }
        firestore.collection("sessions")
            .document(sessionId)
            .update("participants", updatedParticipants)
            .await()
    }

    override suspend fun updateParticipantRole(
        sessionId: String,
        participantId: String,
        role: ParticipantRole
    ): Result<Unit> = runCatching {
        val session = getSession(sessionId).getOrThrow()
        
        // Only admin can change roles
        if (session.adminId != currentUser.uid) {
            throw IllegalStateException("Only session admin can update participant roles")
        }

        val updatedParticipants = session.participants.map { participant ->
            if (participant.id == participantId) {
                participant.copy(
                    role = role,
                    lastUpdated = System.currentTimeMillis()
                )
            } else {
                participant
            }
        }
        firestore.collection("sessions")
            .document(sessionId)
            .update("participants", updatedParticipants)
            .await()
    }

    override suspend fun searchSessions(query: String): Result<List<Session>> = runCatching {
        firestore.collection("sessions")
            .whereGreaterThanOrEqualTo("title", query)
            .whereLessThanOrEqualTo("title", query + '\uf8ff')
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                try {
                    doc.toObject(Session::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse session: ${doc.id}")
                    null
                }
            }
    }

    override suspend fun getActiveSessions(): Result<List<Session>> = runCatching {
        firestore.collection("sessions")
            .whereEqualTo("isActive", true)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                try {
                    doc.toObject(Session::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse session: ${doc.id}")
                    null
                }
            }
    }

    override suspend fun getRecentSessions(limit: Int): Result<List<Session>> = runCatching {
        firestore.collection("sessions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                try {
                    doc.toObject(Session::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse session: ${doc.id}")
                    null
                }
            }
    }

    override suspend fun rejoinSession(sessionId: String): Result<Unit> = runCatching {
        val session = getSession(sessionId).getOrThrow()
        val currentUserId = currentUser.uid

        // Update the participant's status to ACTIVE
        val updatedParticipants = session.participants.map { participant ->
            if (participant.id == currentUserId) {
                participant.copy(status = ParticipantStatus.ACTIVE)
            } else participant
        }

        // Update the session with the modified participants list
        updateSession(
            sessionId = sessionId,
            session = session.copy(participants = updatedParticipants)
        ).getOrThrow()
    }

    private fun generateSessionCode(): String {
        return buildString {
            val allowedChars = ('A'..'Z') + ('0'..'9')
            repeat(6) {
                append(allowedChars.random())
            }
        }
    }
}