package com.example.safetrack.session.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.safetrack.core.domain.model.SessionType
import com.example.safetrack.ui.theme.SafeTrackBlue
import com.example.safetrack.ui.theme.SafeTrackDarkBlue
import com.example.safetrack.ui.theme.SafeTrackWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinSessionScreen(
    onSessionJoined: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: JoinSessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is JoinSessionUiState.Success) {
            onSessionJoined((uiState as JoinSessionUiState.Success).session.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Join Session",
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
                    is JoinSessionUiState.Initial -> {
                        // Initial state, show empty form
                    }
                    is JoinSessionUiState.Editing -> {
                        val state = uiState as JoinSessionUiState.Editing

                        // Session Type Selection
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
                                    text = "Session Type",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Solo Session Card
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(120.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (state.selectedType == SessionType.SOLO)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        onClick = { viewModel.updateSelectedType(SessionType.SOLO) }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = if (state.selectedType == SessionType.SOLO)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Solo",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = if (state.selectedType == SessionType.SOLO)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Group Session Card
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(120.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (state.selectedType == SessionType.GROUP)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        onClick = { viewModel.updateSelectedType(SessionType.GROUP) }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Group,
                                                contentDescription = null,
                                                tint = if (state.selectedType == SessionType.GROUP)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Group",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = if (state.selectedType == SessionType.GROUP)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Join Session Card
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
                                    text = "Join Details",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
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
                                    supportingText = { Text("How others will see you in the session") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SafeTrackBlue,
                                        focusedLabelColor = SafeTrackBlue,
                                        cursorColor = SafeTrackBlue
                                    )
                                )

                                OutlinedTextField(
                                    value = state.code,
                                    onValueChange = viewModel::updateCode,
                                    label = { Text("Session Code") },
                                    leadingIcon = { 
                                        Icon(
                                            Icons.Default.Tag,
                                            null,
                                            tint = SafeTrackBlue
                                        )
                                    },
                                    supportingText = { Text("Enter the code shared with you") },
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
                                    label = { Text("Session Password") },
                                    leadingIcon = { 
                                        Icon(
                                            Icons.Default.Lock,
                                            null,
                                            tint = SafeTrackBlue
                                        )
                                    },
                                    supportingText = { Text("Required if the session is password-protected") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SafeTrackBlue,
                                        focusedLabelColor = SafeTrackBlue,
                                        cursorColor = SafeTrackBlue
                                    )
                                )

                                if (state.selectedType == SessionType.GROUP) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Share My Location",
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
                                }
                            }
                        }

                        // Join Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            Button(
                                onClick = viewModel::joinSession,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SafeTrackBlue,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    Icons.Default.Login,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text("Join Session")
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
                    is JoinSessionUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = SafeTrackBlue)
                        }
                    }
                    is JoinSessionUiState.Success -> {
                        // Navigation handled by LaunchedEffect
                    }
                }
            }
        }
    }
}