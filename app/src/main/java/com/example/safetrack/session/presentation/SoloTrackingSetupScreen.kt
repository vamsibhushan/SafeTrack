package com.example.safetrack.session.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.safetrack.ui.theme.SafeTrackBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoloTrackingSetupScreen(
    onSessionCreated: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SoloTrackingSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is SoloTrackingSetupUiState.Success) {
            onSessionCreated((uiState as SoloTrackingSetupUiState.Success).sessionId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Create Solo Session",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (uiState) {
                    is SoloTrackingSetupUiState.Initial -> {
                        viewModel.updateTitle("Solo Session")
                    }
                    is SoloTrackingSetupUiState.Editing -> {
                        val state = uiState as SoloTrackingSetupUiState.Editing
                        
                        // Basic Details Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Session Details",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                OutlinedTextField(
                                    value = state.title,
                                    onValueChange = viewModel::updateTitle,
                                    label = { Text("Session Title") },
                                    leadingIcon = { 
                                        Icon(
                                            Icons.Default.Title,
                                            null,
                                            tint = SafeTrackBlue
                                        )
                                    },
                                    supportingText = { Text("Default title provided, feel free to change") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SafeTrackBlue,
                                        focusedLabelColor = SafeTrackBlue,
                                        cursorColor = SafeTrackBlue
                                    )
                                )

                                OutlinedTextField(
                                    value = state.displayName,
                                    onValueChange = viewModel::updateDisplayName,
                                    label = { Text("Display Name") },
                                    leadingIcon = { 
                                        Icon(
                                            Icons.Default.Person,
                                            null,
                                            tint = SafeTrackBlue
                                        )
                                    },
                                    supportingText = { Text("Your profile name is used by default") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SafeTrackBlue,
                                        focusedLabelColor = SafeTrackBlue,
                                        cursorColor = SafeTrackBlue
                                    )
                                )

                                OutlinedTextField(
                                    value = state.description,
                                    onValueChange = viewModel::updateDescription,
                                    label = { Text("Description (Optional)") },
                                    leadingIcon = { 
                                        Icon(
                                            Icons.Default.Description,
                                            null,
                                            tint = SafeTrackBlue
                                        )
                                    },
                                    supportingText = { Text("Add optional details about this tracking session") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SafeTrackBlue,
                                        focusedLabelColor = SafeTrackBlue,
                                        cursorColor = SafeTrackBlue
                                    )
                                )
                            }
                        }

                        // Timing Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Session Timing",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Start Right Now",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Switch(
                                        checked = state.startNow,
                                        onCheckedChange = viewModel::updateStartNow,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = SafeTrackBlue,
                                            checkedTrackColor = SafeTrackBlue.copy(alpha = 0.5f)
                                        )
                                    )
                                }

                                if (!state.startNow) {
                                    OutlinedTextField(
                                        value = state.scheduledStart ?: "",
                                        onValueChange = viewModel::updateScheduledStart,
                                        label = { Text("Start Time") },
                                        leadingIcon = { 
                                            Icon(
                                                Icons.Default.Schedule,
                                                null,
                                                tint = SafeTrackBlue
                                            )
                                        },
                                        supportingText = { Text("When should the session start?") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = SafeTrackBlue,
                                            focusedLabelColor = SafeTrackBlue,
                                            cursorColor = SafeTrackBlue
                                        )
                                    )
                                }

                                OutlinedTextField(
                                    value = state.duration ?: "",
                                    onValueChange = viewModel::updateDuration,
                                    label = { Text("Duration (minutes)") },
                                    leadingIcon = { 
                                        Icon(
                                            Icons.Default.Timer,
                                            null,
                                            tint = SafeTrackBlue
                                        )
                                    },
                                    supportingText = { Text("Session will automatically end after this duration") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SafeTrackBlue,
                                        focusedLabelColor = SafeTrackBlue,
                                        cursorColor = SafeTrackBlue
                                    )
                                )
                            }
                        }

                        // Security Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Security & Settings",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                OutlinedTextField(
                                    value = state.password,
                                    onValueChange = viewModel::updatePassword,
                                    label = { Text("Password (Optional)") },
                                    leadingIcon = { 
                                        Icon(
                                            Icons.Default.Lock,
                                            null,
                                            tint = SafeTrackBlue
                                        )
                                    },
                                    supportingText = { Text("Add a password to restrict access") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SafeTrackBlue,
                                        focusedLabelColor = SafeTrackBlue,
                                        cursorColor = SafeTrackBlue
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Enable Location Sharing",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Switch(
                                        checked = state.locationSharingEnabled,
                                        onCheckedChange = viewModel::updateLocationSharing,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = SafeTrackBlue,
                                            checkedTrackColor = SafeTrackBlue.copy(alpha = 0.5f)
                                        )
                                    )
                                }

                                if (state.locationSharingEnabled) {
                                    OutlinedTextField(
                                        value = state.radiusLimit?.toString() ?: "",
                                        onValueChange = { viewModel.updateRadiusLimit(it.toDoubleOrNull()) },
                                        label = { Text("Radius Limit (meters)") },
                                        leadingIcon = { 
                                            Icon(
                                                Icons.Default.RadioButtonChecked,
                                                null,
                                                tint = SafeTrackBlue
                                            )
                                        },
                                        supportingText = { Text("Get alerts when exceeding this distance") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = SafeTrackBlue,
                                            focusedLabelColor = SafeTrackBlue,
                                            cursorColor = SafeTrackBlue
                                        )
                                    )
                                }
                            }
                        }

                        // Create Button at the bottom
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            Button(
                                onClick = viewModel::createSession,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SafeTrackBlue,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text("Create Session")
                            }
                        }

                        if (state.error != null) {
                            Text(
                                text = state.error,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    is SoloTrackingSetupUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = SafeTrackBlue)
                        }
                    }
                    is SoloTrackingSetupUiState.Success -> {
                        // Navigation handled by LaunchedEffect
                    }
                }
            }
        }
    }
} 