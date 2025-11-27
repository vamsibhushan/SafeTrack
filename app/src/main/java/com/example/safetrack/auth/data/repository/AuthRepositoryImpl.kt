package com.example.safetrack.auth.data.repository

import com.example.safetrack.auth.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override suspend fun signIn(email: String, password: String): Result<FirebaseUser> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await().user
            ?: throw Exception("Sign in failed")
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Result<FirebaseUser> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception("Sign up failed")

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()

        user.updateProfile(profileUpdates).await()
        user
    }

    override suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await().user
            ?: throw Exception("Google sign in failed")
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        auth.signOut()
    }

    override suspend fun resetPassword(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }

    override suspend fun updateProfile(displayName: String?, photoUrl: String?): Result<Unit> = runCatching {
        val user = currentUser ?: throw Exception("User not authenticated")
        val profileUpdates = UserProfileChangeRequest.Builder().apply {
            displayName?.let { setDisplayName(it) }
            photoUrl?.let { setPhotoUri(android.net.Uri.parse(it)) }
        }.build()
        user.updateProfile(profileUpdates).await()
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        val user = currentUser ?: throw Exception("User not authenticated")
        user.delete().await()
    }
}