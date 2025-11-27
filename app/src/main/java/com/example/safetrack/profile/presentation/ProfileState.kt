package com.example.safetrack.profile.presentation

import android.net.Uri

data class ProfileState(
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val profilePictureUrl: String? = null,
    val selectedImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)