package com.example.safetrack.map.presentation

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.safetrack.core.domain.model.ParticipantStatus
import com.example.safetrack.core.domain.model.Session
import com.example.safetrack.core.domain.model.SessionType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    sessionId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToSessionDetails: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableSessions by viewModel.availableSessions.collectAsState()
    val context = LocalContext.current
    
    // Handle location permission
    val locationPermissionState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_FINE_LOCATION
    ) { isGranted ->
        if (isGranted) {
            viewModel.onLocationPermissionGranted()
        }
    }

    // Handle session loading and location updates
    LaunchedEffect(sessionId) {
        if (sessionId != null) {
            viewModel.loadActiveSession(sessionId)
        } else {
            viewModel.clearActiveSession()
        }
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Map") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back"
                        )
                    }
                },
                actions = {
                    var showSessionSelector by remember { mutableStateOf(false) }
                    IconButton(onClick = { showSessionSelector = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            "Select Session"
                        )
                    }
                    SessionSelectorDropdown(
                        expanded = showSessionSelector,
                        onDismiss = { showSessionSelector = false },
                        currentSession = if (uiState is MapUiState.Success) (uiState as MapUiState.Success).session else null,
                        availableSessions = availableSessions,
                        onSelectCurrentLocation = {
                            viewModel.clearActiveSession()
                            showSessionSelector = false
                        },
                        onSelectSession = { sessionId ->
                            viewModel.loadActiveSession(sessionId)
                            showSessionSelector = false
                        }
                    )
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                is MapUiState.Loading -> {
                    LoadingIndicator()
                }
                is MapUiState.LocationDisabled -> {
                    LocationDisabledContent(
                        message = (uiState as MapUiState.LocationDisabled).message,
                        onEnableLocation = {
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        }
                    )
                }
                is MapUiState.Error -> {
                    val state = uiState as MapUiState.Error
                    ErrorContent(
                        error = state.error,
                        currentLocation = state.currentLocation,
                        locationPermissionGranted = locationPermissionState.status.isGranted
                    )
                }
                is MapUiState.Success -> {
                    val state = uiState as MapUiState.Success
                    SuccessContent(
                        currentLocation = state.currentLocation,
                        session = state.session,
                        radiusAlerts = state.radiusAlerts,
                        locationPermissionGranted = locationPermissionState.status.isGranted,
                        onNavigateToSessionDetails = onNavigateToSessionDetails
                    )
                }
            }

            // Show permission request if needed
            if (!locationPermissionState.status.isGranted) {
                LocationPermissionCard(
                    title = "Location Permission Required",
                    message = "Please grant location permission to use the map and enable location tracking",
                    onRequestPermission = {
                        locationPermissionState.launchPermissionRequest()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SessionSelectorDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    currentSession: Session?,
    availableSessions: List<SessionListItem>,
    onSelectCurrentLocation: () -> Unit,
    onSelectSession: (String) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        // Current Location Option
        DropdownMenuItem(
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = if (currentSession == null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Current Location",
                        color = if (currentSession == null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            onClick = onSelectCurrentLocation
        )

        if (availableSessions.isNotEmpty()) {
            // My Sessions
            val mySessions = availableSessions.filter { it.isAdmin }
            if (mySessions.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "My Sessions",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                mySessions.forEach { session ->
                    SessionDropdownItem(
                        session = session,
                        isSelected = currentSession?.id == session.id,
                        onClick = { onSelectSession(session.id) }
                    )
                }
            }

            // Joined Sessions
            val joinedSessions = availableSessions.filter { !it.isAdmin }
            if (joinedSessions.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Joined Sessions",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                joinedSessions.forEach { session ->
                    SessionDropdownItem(
                        session = session,
                        isSelected = currentSession?.id == session.id,
                        onClick = { onSelectSession(session.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun LocationDisabledContent(
    message: String,
    onEnableLocation: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.LocationOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onEnableLocation) {
            Text("Enable Location Services")
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    currentLocation: LatLng?,
    locationPermissionGranted: Boolean
) {
    if (currentLocation != null) {
        MapContent(
            currentLocation = currentLocation,
            session = null,
            radiusAlerts = emptyList(),
            locationPermissionGranted = locationPermissionGranted
        )
    }
    
    // Show error message overlay
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun SuccessContent(
    currentLocation: LatLng,
    session: Session?,
    radiusAlerts: List<RadiusAlert>,
    locationPermissionGranted: Boolean,
    onNavigateToSessionDetails: (String) -> Unit
) {
    MapContent(
        currentLocation = currentLocation,
        session = session,
        radiusAlerts = radiusAlerts,
        locationPermissionGranted = locationPermissionGranted
    )

    // Session info overlay
    session?.let { activeSession ->
        SessionInfoOverlay(
            session = activeSession,
            radiusAlerts = radiusAlerts,
            onViewDetails = { onNavigateToSessionDetails(activeSession.id) }
        )
    }
}

@Composable
private fun SessionInfoOverlay(
    session: Session,
    radiusAlerts: List<RadiusAlert>,
    onViewDetails: () -> Unit
) {
    Column {
        // Session title card
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clickable {
                    onViewDetails()
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (session.type == SessionType.SOLO) 
                            Icons.Default.Person else Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (session.type == SessionType.GROUP) {
                            Text(
                                text = "${session.participants.size + 1} participants",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Radius alerts card
        if (session.type == SessionType.GROUP && radiusAlerts.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Radius Alerts",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    radiusAlerts.forEach { alert ->
                        Text(
                            text = "${alert.participantName}: ${alert.distance}m",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapContent(
    currentLocation: LatLng?,
    session: Session?,
    radiusAlerts: List<RadiusAlert>,
    locationPermissionGranted: Boolean
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            currentLocation ?: LatLng(0.0, 0.0),
            15f
        )
    }

    LaunchedEffect(currentLocation) {
        currentLocation?.let {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLng(it),
                durationMs = 1000
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = locationPermissionGranted,
                mapType = MapType.NORMAL,
                isBuildingEnabled = true,
                isTrafficEnabled = true,
                mapStyleOptions = MapStyleOptions("[]")
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = true,
                compassEnabled = true,
                zoomControlsEnabled = true
            )
        ) {
            // Show current location marker only when no session is selected
            if (currentLocation != null && session == null) {
                Marker(
                    state = MarkerState(position = currentLocation),
                    title = "My Location",
                    snippet = "Current location",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                )
            }

            // Show session markers if a session is active
            if (session != null) {
                when (session.type) {
                    SessionType.SOLO -> {
                        session.adminLocation?.let { location ->
                            Marker(
                                state = MarkerState(
                                    position = LatLng(location.latitude, location.longitude)
                                ),
                                title = session.adminName,
                                snippet = "Admin",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                            )
                        }
                    }
                    SessionType.GROUP -> {
                        // Admin marker
                        session.adminLocation?.let { location ->
                            Marker(
                                state = MarkerState(
                                    position = LatLng(location.latitude, location.longitude)
                                ),
                                title = session.adminName,
                                snippet = "Admin",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                            )
                        }
                        // Participant markers
                        session.participants.forEach { participant ->
                            participant.location?.let { location ->
                                Marker(
                                    state = MarkerState(
                                        position = LatLng(location.latitude, location.longitude)
                                    ),
                                    title = participant.name,
                                    snippet = "Participant",
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                                )
                            }
                        }
                        // Show radius limit circle if set
                        session.radiusLimit?.let { radius ->
                            session.adminLocation?.let { adminLocation ->
                                Circle(
                                    center = LatLng(adminLocation.latitude, adminLocation.longitude),
                                    radius = radius.toDouble(),
                                    fillColor = Color(0x1A0000FF),
                                    strokeColor = Color(0xFF0000FF),
                                    strokeWidth = 2f
                                )
                            }
                        }
                    }
                }
            }
        }

        // Session title indicator
        if (session != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .fillMaxWidth(0.9f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (session.type == SessionType.SOLO) 
                            Icons.Default.Person else Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (session.type == SessionType.GROUP) {
                            Text(
                                text = "${session.participants.size + 1} participants",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Radius alerts
        if (session?.type == SessionType.GROUP && radiusAlerts.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Radius Alerts",
                        style = MaterialTheme.typography.titleMedium
                    )
                    radiusAlerts.forEach { alert ->
                        Text(
                            text = "${alert.participantName}: ${alert.distance}m",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationPermissionCard(
    title: String,
    message: String,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
private fun SessionDropdownItem(
    session: SessionListItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (session.isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Column {
                    Text(
                        text = session.title,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (session.isAdmin) "Admin" else "Participant",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        onClick = onClick
    )
}