package com.example.safetrack.auth.domain.model

sealed class AuthState {
    data object Initial : AuthState()
    data object Loading : AuthState()
    data class Success(val userId: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

data class AuthFormState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val rememberMe: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null
)