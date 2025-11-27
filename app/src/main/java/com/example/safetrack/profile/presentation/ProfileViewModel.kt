package com.example.safetrack.profile.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetrack.auth.domain.repository.AuthRepository
import com.example.safetrack.profile.domain.repository.ProfileRepository
import com.example.safetrack.settings.data.UserPreferences
import com.example.safetrack.settings.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow(ProfileState())
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _profileState.update { it.copy(isLoading = true) }
            try {
                profileRepository.getUserProfile().collect { user ->
                    if (user != null) {
                        _profileState.update {
                            it.copy(
                                isLoading = false,
                                displayName = user.displayName ?: userPreferences.value.userName ?: "",
                                email = user.email ?: "",
                                phone = userPreferences.value.userPhone ?: "",
                                profilePictureUrl = user.photoUrl?.toString() ?: userPreferences.value.profilePictureUri
                            )
                        }
                    } else {
                        _profileState.update { it.copy(isLoading = false, error = "User not authenticated") }
                    }
                }
            } catch (e: Exception) {
                _profileState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load profile") }
            }
        }
    }

    fun updateName(name: String) {
        _profileState.update { it.copy(displayName = name) }
    }

    fun updatePhone(phone: String) {
        _profileState.update { it.copy(phone = phone) }
    }

    fun updateProfilePicture(uri: Uri?) {
        _profileState.update { it.copy(selectedImageUri = uri) }
    }

    fun saveProfile() {
        val currentState = _profileState.value
        if (currentState.displayName.isBlank()) {
            _profileState.update { it.copy(error = "Name cannot be empty") }
            return
        }

        viewModelScope.launch {
            _profileState.update { it.copy(isLoading = true, error = null) }
            try {
                profileRepository.updateProfile(
                    displayName = currentState.displayName,
                    photoUrl = currentState.selectedImageUri?.toString()
                ).onSuccess {
                    userPreferencesRepository.updateUserName(currentState.displayName)
                    userPreferencesRepository.updateUserPhone(currentState.phone)
                    currentState.selectedImageUri?.toString()?.let { uri ->
                        userPreferencesRepository.updateProfilePictureUri(uri)
                    }
                    _profileState.update {
                        it.copy(
                            isLoading = false,
                            isSaved = true,
                            selectedImageUri = null,
                            profilePictureUrl = currentState.selectedImageUri?.toString()
                        )
                    }
                }.onFailure { error ->
                    _profileState.update { it.copy(isLoading = false, error = error.message ?: "Failed to save profile") }
                }
            } catch (e: Exception) {
                _profileState.update { it.copy(isLoading = false, error = e.message ?: "Failed to save profile") }
            }
        }
    }

    fun clearSavedState() {
        _profileState.update { it.copy(isSaved = false) }
    }
}