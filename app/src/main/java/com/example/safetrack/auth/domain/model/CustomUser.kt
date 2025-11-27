package com.example.safetrack.auth.domain.model

data class CustomUser(
    val uid: String,
    val email: String?,
    val displayName: String?
)