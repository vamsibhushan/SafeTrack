package com.example.safetrack.profile.data.repository

import com.example.safetrack.auth.domain.repository.AuthRepository
import com.example.safetrack.profile.domain.repository.ProfileRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject



class ProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : ProfileRepository {

    override fun getUserProfile(): Flow<FirebaseUser?> = callbackFlow {
        val currentUser = authRepository.currentUser
        if (currentUser == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(currentUser)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun updateProfile(displayName: String?, photoUrl: String?): Result<Unit> = runCatching {
        val user = authRepository.currentUser ?: throw Exception("User not authenticated")

        // Validate photo URL format if provided
        photoUrl?.let { url ->
            try {
                android.net.Uri.parse(url)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid photo URL format")
            }
        }

        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder().apply {
            displayName?.let { setDisplayName(it) }
            photoUrl?.let { setPhotoUri(android.net.Uri.parse(it)) }
        }.build()

        try {
            user.updateProfile(profileUpdates).await()
        } catch (e: Exception) {
            throw Exception("Failed to update profile: ${e.message}")
        }
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        val user = authRepository.currentUser ?: throw Exception("User not authenticated")

        try {
            // Delete user data from Firestore first
            firestore.collection("users")
                .document(user.uid)
                .delete()
                .await()

            // Then delete the Firebase Auth account
            user.delete().await()
        } catch (e: Exception) {
            throw Exception("Failed to delete account: ${e.message}")
        }
    }
}