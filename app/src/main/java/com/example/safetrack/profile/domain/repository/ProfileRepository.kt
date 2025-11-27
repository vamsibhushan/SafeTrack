package com.example.safetrack.profile.domain.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getUserProfile(): Flow<FirebaseUser?>
    suspend fun updateProfile(displayName: String?, photoUrl: String?): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
} 