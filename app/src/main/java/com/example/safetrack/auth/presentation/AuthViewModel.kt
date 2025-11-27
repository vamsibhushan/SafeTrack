package com.example.safetrack.auth.presentation

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetrack.auth.domain.model.AuthFormState
import com.example.safetrack.auth.domain.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _formState = MutableStateFlow(AuthFormState())
    val formState: StateFlow<AuthFormState> = _formState.asStateFlow()

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("94430480689-139fm82tpbc1arhpgf8s5ifso2q24fcq.apps.googleusercontent.com")
        .requestEmail()
        .build()

    private val googleSignInClient = GoogleSignIn.getClient(context, gso)

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            val user = authRepository.currentUser
            _authState.value = if (user != null) {
                AuthState.Authenticated(user)
            } else {
                AuthState.Unauthenticated
            }
        }
    }

    fun onEmailChange(email: String) {
        _formState.update { it.copy(
            email = email,
            emailError = validateEmail(email)
        ) }
    }

    fun onPasswordChange(password: String) {
        _formState.update { it.copy(
            password = password,
            passwordError = validatePassword(password)
        ) }
    }

    fun onPasswordVisibilityChange() {
        _formState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onRememberMeChange(rememberMe: Boolean) {
        _formState.update { it.copy(rememberMe = rememberMe) }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = authRepository.signIn(email, password)
                result.fold(
                    onSuccess = { user ->
                        _authState.value = AuthState.Authenticated(user)
                    },
                    onFailure = { exception ->
                        _authState.value = AuthState.Error(exception.message ?: "Sign in failed")
                    }
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            }
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = authRepository.signUp(email, password, displayName)
                result.fold(
                    onSuccess = { user ->
                        _authState.value = AuthState.Authenticated(user)
                    },
                    onFailure = { exception ->
                        _authState.value = AuthState.Error(exception.message ?: "Sign up failed")
                    }
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = authRepository.signInWithGoogle(idToken)
                result.fold(
                    onSuccess = { user ->
                        _authState.value = AuthState.Authenticated(user)
                    },
                    onFailure = { exception ->
                        _authState.value = AuthState.Error(exception.message ?: "Google sign in failed")
                    }
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google sign in failed")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = authRepository.signOut()
                result.fold(
                    onSuccess = {
                        _authState.value = AuthState.Unauthenticated
                    },
                    onFailure = { exception ->
                        _authState.value = AuthState.Error(exception.message ?: "Sign out failed")
                    }
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign out failed")
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = authRepository.resetPassword(email)
                result.fold(
                    onSuccess = {
                        _authState.value = AuthState.PasswordResetSent
                    },
                    onFailure = { exception ->
                        _authState.value = AuthState.Error(exception.message ?: "Password reset failed")
                    }
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Password reset failed")
            }
        }
    }

    fun getGoogleSignInIntent(): Intent {
        googleSignInClient.signOut()
        return googleSignInClient.signInIntent
    }

    fun handleGoogleSignInResult(idToken: String) {
        signInWithGoogle(idToken)
    }

    fun handleSignInError(errorMessage: String) {
        _authState.value = AuthState.Error(errorMessage)
    }

    private fun validateForm(): Boolean {
        val currentState = _formState.value
        val emailError = validateEmail(currentState.email)
        val passwordError = validatePassword(currentState.password)

        _formState.update { it.copy(
            emailError = emailError,
            passwordError = passwordError
        ) }

        return emailError == null && passwordError == null
    }

    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email cannot be empty"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email format"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password cannot be empty"
            password.length < 8 -> "Password must be at least 8 characters"
            !password.any { it.isDigit() } -> "Password must contain at least one number"
            !password.any { it.isUpperCase() } -> "Password must contain at least one uppercase letter"
            else -> null
        }
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    object Unauthenticated : AuthState()
    object PasswordResetSent : AuthState()
    data class Error(val message: String) : AuthState()
}