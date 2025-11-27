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
import com.example.safetrack.ui.theme.SafeTrackDarkBlue
import com.example.safetrack.ui.theme.SafeTrackWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupTrackingSetupScreen(
    onSessionCreated: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: GroupTrackingSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is GroupTrackingSetupUiState.Success) {
            onSessionCreated((uiState as GroupTrackingSetupUiState.Success).sessionId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Create Group Session",
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
                    is GroupTrackingSetupUiState.Initial -> {
                        viewModel.updateTitle("Group Session")
                    }
                    is GroupTrackingSetupUiState.Editing -> {
                        val state = uiState as GroupTrackingSetupUiState.Editing
                        
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
                                    text = "Group Details",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                OutlinedTextField(
                                    value = state.title,
                                    onValueChange = viewModel::updateTitle,
                                    label = { Text("Group Title") },
                                    leadingIcon = { 
                                        Icon(
                                            Icons.Default.Title,
                                            null,
                                            tint = SafeTrackBlue
                                        )
                                    },
                                    supportingText = { Text("Give your group session a name") },
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
                                    label = { Text("Your Display Name") },
                                    leadingIcon = { 
                                        Icon(
                                            Icons.Default.Person,
                                            null,
                                            tint = SafeTrackBlue
                                        )
                                    },
                                    supportingText = { Text("How others will see you in the group") },
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
                                    label = { Text("Group Description") },
                                    leadingIcon = { 
                                        Icon(
                                            Icons.Default.Description,
                                            null,
                                            tint = SafeTrackBlue
                                        )
                                    },
                                    supportingText = { Text("Describe the purpose of this group session") },
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

                        // Group Settings Card
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
                                    text = "Group Settings",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                OutlinedTextField(
                                    value = state.maxParticipants?.toString() ?: "",
                                    onValueChange = { viewModel.updateMaxParticipants(it.toIntOrNull()) },
                                    label = { Text("Maximum Participants") },
                                    leadingIcon = { 
                                        Icon(
                                            Icons.Default.Group,
                                            null,
                                            tint = SafeTrackBlue
                                        )
                                    },
                                    supportingText = { Text("Limit the number of people who can join") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SafeTrackBlue,
                                        focusedLabelColor = SafeTrackBlue,
                                        cursorColor = SafeTrackBlue
                                    )
                                )

                                OutlinedTextField(
                                    value = state.password,
                                    onValueChange = viewModel::updatePassword,
                                    label = { Text("Group Password") },
                                    leadingIcon = { 
                                        Icon(
                                            Icons.Default.Lock,
                                            null,
                                            tint = SafeTrackBlue
                                        )
                                    },
                                    supportingText = { Text("Set a password for joining the group") },
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
                                        supportingText = { Text("Get alerts when participants exceed this distance") },
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
                                    Icons.Default.GroupAdd,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text("Create Group Session")
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
                    is GroupTrackingSetupUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = SafeTrackBlue)
                        }
                    }
                    is GroupTrackingSetupUiState.Success -> {
                        // Navigation handled by LaunchedEffect
                    }
                }
            }
        }
    }
} 