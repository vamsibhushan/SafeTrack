package com.example.safetrack.map.presentation

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safetrack.auth.domain.repository.AuthRepository
import com.example.safetrack.core.domain.model.ParticipantStatus
import com.example.safetrack.core.domain.model.Session
import com.example.safetrack.core.service.LocationService
import com.example.safetrack.core.domain.repository.SessionRepository
import com.example.safetrack.core.util.LocationPermissionUtil
import com.example.safetrack.settings.data.UserPreferencesRepository
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val locationService: LocationService,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    private val _availableSessions = MutableStateFlow<List<SessionListItem>>(emptyList())
    val availableSessions: StateFlow<List<SessionListItem>> = _availableSessions.asStateFlow()

    private val currentUserId: String
        get() = authRepository.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    init {
        viewModelScope.launch {
            try {
                // First check if we have location permissions and services enabled
                val locationEnabled = LocationPermissionUtil.isLocationEnabled(context)
                val hasPermissions = LocationPermissionUtil.hasForegroundLocationPermissions(context)
                
                // Get initial preferences
                val preferences = userPreferencesRepository.userPreferencesFlow.first()
                
                // Start location updates if everything is enabled
                if (locationEnabled && hasPermissions && preferences.locationTrackingEnabled) {
                    startLocationUpdates()
                } else {
                    // Only show location disabled message if we're sure location is disabled
                    when {
                        !locationEnabled -> {
                            _uiState.value = MapUiState.LocationDisabled("Please enable location services in device settings")
                            if (preferences.locationTrackingEnabled) {
                                userPreferencesRepository.updateLocationTrackingEnabled(false)
                            }
                        }
                        !hasPermissions -> {
                            _uiState.value = MapUiState.LocationDisabled("Location permissions are required")
                            if (preferences.locationTrackingEnabled) {
                                userPreferencesRepository.updateLocationTrackingEnabled(false)
                            }
                        }
                        !preferences.locationTrackingEnabled -> {
                            _uiState.value = MapUiState.LocationDisabled("Location tracking is disabled in settings")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking location state")
                _uiState.value = MapUiState.Error("Failed to check location state")
            }
        }
        loadAvailableSessions()
    }

    private fun isUserPartOfSession(session: Session): Boolean {
        return session.adminId == currentUserId || 
               session.participants.any { 
                   it.id == currentUserId && 
                   it.status == ParticipantStatus.ACTIVE 
               }
    }

    private fun loadAvailableSessions() {
        viewModelScope.launch {
            try {
                sessionRepository.getSessions()
                    .collect { sessions ->
                        val filteredSessions = sessions.filter { session ->
                            session.isActive && isUserPartOfSession(session)
                        }
                        _availableSessions.value = filteredSessions.map { session ->
                            SessionListItem(
                                id = session.id,
                                title = session.title,
                                isActive = session.isActive,
                                isAdmin = session.adminId == currentUserId
                            )
                        }

                        // Handle session selection based on availability
                        if (filteredSessions.isEmpty()) {
                            clearActiveSession()
                        } else {
                            val currentSessionId = _activeSessionId.value
                            if (currentSessionId != null) {
                                // If current session is not in available sessions, show current location
                                if (!filteredSessions.any { it.id == currentSessionId }) {
                                    clearActiveSession()
                                }
                            } else {
                                // If no session is selected, show current location
                                clearActiveSession()
                            }
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load available sessions")
                clearActiveSession()
            }
        }
    }

    fun clearActiveSession() {
        _activeSessionId.value = null
        val currentState = _uiState.value
        if (currentState is MapUiState.Success) {
            _uiState.value = currentState.copy(
                session = null,
                radiusAlerts = emptyList()
            )
        }
    }

    fun onLocationPermissionGranted() {
        viewModelScope.launch {
            checkLocationAndStartUpdates()
        }
    }

    private suspend fun checkLocationAndStartUpdates() {
        val locationEnabled = LocationPermissionUtil.isLocationEnabled(context)
        val hasPermissions = LocationPermissionUtil.hasForegroundLocationPermissions(context)
        
        when {
            !locationEnabled -> {
                _uiState.value = MapUiState.LocationDisabled("Please enable location services in device settings")
                userPreferencesRepository.updateLocationTrackingEnabled(false)
            }
            !hasPermissions -> {
                _uiState.value = MapUiState.LocationDisabled("Location permissions are required")
                userPreferencesRepository.updateLocationTrackingEnabled(false)
            }
            else -> {
                userPreferencesRepository.updateLocationTrackingEnabled(true)
                startLocationUpdates()
            }
        }
    }

    private fun observeLocationSettings() {
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.collect { preferences ->
                val locationEnabled = LocationPermissionUtil.isLocationEnabled(context)
                val hasPermissions = LocationPermissionUtil.hasForegroundLocationPermissions(context)

                // Only update state if it's different from current state
                val currentState = _uiState.value
                when {
                    !locationEnabled -> {
                        if (currentState !is MapUiState.LocationDisabled || 
                            (currentState as MapUiState.LocationDisabled).message != "Please enable location services in device settings") {
                            _uiState.value = MapUiState.LocationDisabled("Please enable location services in device settings")
                            if (preferences.locationTrackingEnabled) {
                                userPreferencesRepository.updateLocationTrackingEnabled(false)
                            }
                        }
                    }
                    !hasPermissions -> {
                        if (currentState !is MapUiState.LocationDisabled || 
                            (currentState as MapUiState.LocationDisabled).message != "Location permissions are required") {
                            _uiState.value = MapUiState.LocationDisabled("Location permissions are required")
                            if (preferences.locationTrackingEnabled) {
                                userPreferencesRepository.updateLocationTrackingEnabled(false)
                            }
                        }
                    }
                    !preferences.locationTrackingEnabled -> {
                        if (currentState !is MapUiState.LocationDisabled || 
                            (currentState as MapUiState.LocationDisabled).message != "Location tracking is disabled in settings") {
                            _uiState.value = MapUiState.LocationDisabled("Location tracking is disabled in settings")
                        }
                    }
                    else -> {
                        // If everything is enabled and we're not already showing the map, start location updates
                        if (currentState is MapUiState.LocationDisabled) {
                            startLocationUpdates()
                        }
                    }
                }
            }
        }
    }

    private fun startLocationUpdates() {
        viewModelScope.launch {
            try {
                // First try to get current location
                locationService.getCurrentLocation()?.let { location ->
                    handleLocationUpdate(location)
                } ?: run {
                    // If we couldn't get current location, show error
                    _uiState.value = MapUiState.Error("Failed to get current location")
                }
                
                // Then start continuous updates
                locationService.getLocationUpdates(true)
                    .catch { e ->
                        Timber.e(e, "Error in location updates")
                        val currentState = _uiState.value
                        if (currentState is MapUiState.Success) {
                            _uiState.value = MapUiState.Error(
                                error = "Failed to get location updates",
                                currentLocation = currentState.currentLocation
                            )
                        } else {
                            _uiState.value = MapUiState.Error(
                                error = "Failed to get location updates"
                            )
                        }
                    }
                    .collect { location ->
                        handleLocationUpdate(location)
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to start location updates")
                _uiState.value = MapUiState.Error(
                    error = "Failed to get location updates"
                )
            }
        }
    }

    private fun handleLocationUpdate(location: Location) {
        val currentState = _uiState.value
        val latLng = LatLng(location.latitude, location.longitude)
        
        // If we get a location update and we're in LocationDisabled state, switch to Success
        if (currentState is MapUiState.LocationDisabled) {
            _uiState.value = MapUiState.Success(
                currentLocation = latLng,
                session = null,
                radiusAlerts = emptyList()
            )
            return
        }
        
        when {
            _activeSessionId.value != null -> {
                // Update session location and UI state
                updateSessionLocation(GeoPoint(location.latitude, location.longitude))
                if (currentState is MapUiState.Success) {
                    val session = currentState.session
                    if (session != null && (!session.isActive || !isUserPartOfSession(session))) {
                        // If session became inactive or user was removed, clear it
                        clearActiveSession()
                        return
                    }
                    
                    val radiusAlerts = if (session != null) {
                        calculateRadiusAlerts(session)
                    } else {
                        emptyList()
                    }
                    
                    _uiState.value = currentState.copy(
                        currentLocation = latLng,
                        radiusAlerts = radiusAlerts
                    )
                } else {
                    // If we were in error state but got location, switch to success
                    _uiState.value = MapUiState.Success(
                        currentLocation = latLng,
                        session = null,
                        radiusAlerts = emptyList()
                    )
                }
            }
            currentState is MapUiState.Success -> {
                // Just update current location when no session is selected
                _uiState.value = currentState.copy(
                    currentLocation = latLng,
                    session = null,
                    radiusAlerts = emptyList()
                )
            }
            currentState is MapUiState.Error -> {
                // If we were in error state but got location, switch to success
                _uiState.value = MapUiState.Success(
                    currentLocation = latLng,
                    session = null,
                    radiusAlerts = emptyList()
                )
            }
            else -> {
                // Initial state with just location
                _uiState.value = MapUiState.Success(
                    currentLocation = latLng,
                    session = null,
                    radiusAlerts = emptyList()
                )
            }
        }
    }

    fun loadActiveSession(sessionId: String) {
        viewModelScope.launch {
            try {
                val sessionResult = sessionRepository.getSession(sessionId)
                val session = sessionResult.getOrNull()
                if (session != null && isUserPartOfSession(session)) {
                    _activeSessionId.value = sessionId
                    val currentState = _uiState.value
                    if (currentState is MapUiState.Success) {
                        _uiState.value = currentState.copy(
                            session = session,
                            radiusAlerts = calculateRadiusAlerts(session)
                        )
                    } else {
                        // If we don't have a current location yet, create a new success state
                        _uiState.value = MapUiState.Success(
                            currentLocation = LatLng(0.0, 0.0), // This will be updated when we get location
                            session = session,
                            radiusAlerts = calculateRadiusAlerts(session)
                        )
                    }
                } else {
                    clearActiveSession()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load session")
                clearActiveSession()
            }
        }
    }

    private fun updateSessionLocation(location: GeoPoint) {
        val sessionId = _activeSessionId.value ?: return
        viewModelScope.launch {
            try {
                val sessionResult = sessionRepository.getSession(sessionId)
                val session = sessionResult.getOrNull() ?: return@launch
                
                if (!session.isActive || !isUserPartOfSession(session)) {
                    clearActiveSession()
                    return@launch
                }
                
                if (session.adminId == currentUserId) {
                    sessionRepository.updateAdminLocation(sessionId, location)
                } else {
                    sessionRepository.updateParticipantLocation(sessionId, currentUserId, location)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update location")
                // Don't show error UI, just log it
            }
        }
    }

    private fun updateUiState(session: Session) {
        val currentState = _uiState.value
        if (currentState is MapUiState.Success) {
            _uiState.value = currentState.copy(
                session = session,
                radiusAlerts = calculateRadiusAlerts(session, currentState.currentLocation)
            )
        }
    }

    private fun calculateRadiusAlerts(session: Session): List<RadiusAlert> {
        if (session.radiusLimit == null || session.adminLocation == null) return emptyList()

        return session.participants.mapNotNull { participant ->
            participant.location?.let { location ->
                val distance = calculateDistance(
                    session.adminLocation.latitude,
                    session.adminLocation.longitude,
                    location.latitude,
                    location.longitude
                )
                if (distance > session.radiusLimit) {
                    RadiusAlert(participant.id, participant.name, distance)
                } else null
            }
        }
    }

    private fun calculateRadiusAlerts(session: Session, currentLocation: LatLng): List<RadiusAlert> {
        if (session.radiusLimit == null || session.adminLocation == null) return emptyList()

        val distance = calculateDistance(
            session.adminLocation.latitude,
            session.adminLocation.longitude,
            currentLocation.latitude,
            currentLocation.longitude
        )
        if (distance > session.radiusLimit) {
            return listOf(RadiusAlert(session.adminId, session.title, distance))
        } else {
            return emptyList()
        }
    }

    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371e3 // Earth's radius in meters
        val φ1 = lat1 * Math.PI / 180
        val φ2 = lat2 * Math.PI / 180
        val Δφ = (lat2 - lat1) * Math.PI / 180
        val Δλ = (lon2 - lon1) * Math.PI / 180

        val a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
                Math.cos(φ1) * Math.cos(φ2) *
                Math.sin(Δλ / 2) * Math.sin(Δλ / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return r * c
    }
}

data class SessionListItem(
    val id: String,
    val title: String,
    val isActive: Boolean,
    val isAdmin: Boolean
)

sealed class MapUiState {
    object Loading : MapUiState()
    data class LocationDisabled(val message: String) : MapUiState()
    data class Error(val error: String, val currentLocation: LatLng? = null) : MapUiState()
    data class Success(
        val currentLocation: LatLng,
        val session: Session? = null,
        val radiusAlerts: List<RadiusAlert> = emptyList()
    ) : MapUiState()
}

data class RadiusAlert(
    val participantId: String,
    val participantName: String,
    val distance: Double
) 