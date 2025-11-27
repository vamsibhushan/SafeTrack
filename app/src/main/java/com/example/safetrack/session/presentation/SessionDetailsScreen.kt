package com.example.safetrack.session.presentation

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.safetrack.core.domain.model.ParticipantStatus
import com.example.safetrack.core.domain.model.SessionType
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.safetrack.ui.theme.SafeTrackBlue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailsScreen(
    onNavigateToMap: () -> Unit,
    onNavigateBack: () -> Unit,
    sessionId: String,
    viewModel: SessionDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is SessionDetailsUiState.Success) {
            val state = uiState as SessionDetailsUiState.Success
            state.shareIntent?.let { intent ->
                context.startActivity(Intent.createChooser(intent, "Share Session"))
                viewModel.clearShareIntent()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Session Details",
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
                actions = {
                    if (uiState is SessionDetailsUiState.Success) {
                        val state = uiState as SessionDetailsUiState.Success
                        val currentUserParticipant = state.session.participants.find { it.id == viewModel.getCurrentUserId() }
                        val hasLeftSession = currentUserParticipant?.status == ParticipantStatus.LEFT

                        if (!hasLeftSession && !state.session.isAdmin(viewModel.getCurrentUserId())) {
                            // Only show remove option for non-admin participants who haven't left
                            IconButton(onClick = { showRemoveDialog = true }) {
                                Icon(
                                    Icons.Default.RemoveCircle,
                                    "Remove Session",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                is SessionDetailsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SafeTrackBlue)
                    }
                }
                is SessionDetailsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = (uiState as SessionDetailsUiState.Error).error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                is SessionDetailsUiState.Success -> {
                    val state = uiState as SessionDetailsUiState.Success
                    val session = state.session
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    val isAdmin = session.adminId == viewModel.getCurrentUserId()
                    val currentUserParticipant = session.participants.find { it.id == viewModel.getCurrentUserId() }
                    val hasLeftSession = currentUserParticipant?.status == ParticipantStatus.LEFT

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Session Info Card
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.elevatedCardElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = session.title,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    SessionTypeChip(type = session.type)
                                }
                                
                                if (session.description?.isNotBlank() == true) {
                                    Text(
                                        text = session.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Divider(modifier = Modifier.padding(vertical = 4.dp))

                                // Admin Info Section
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.AdminPanelSettings,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Column {
                                            Text(
                                                text = "Session Admin",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = session.adminName,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                                
                                InfoRow(
                                    icon = Icons.Default.Schedule,
                                    label = "Created",
                                    value = dateFormat.format(Date(session.createdAt))
                                )
                                
                                InfoRow(
                                    icon = if (session.isActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    label = "Status",
                                    value = if (session.isActive) "Active" else "Inactive",
                                    valueColor = if (session.isActive) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.error
                                )

                                if (session.radiusLimit != null) {
                                    InfoRow(
                                        icon = Icons.Default.RadioButtonChecked,
                                        label = "Radius Limit",
                                        value = "${session.radiusLimit}m"
                                    )
                                }
                            }
                        }

                        // Participants Card (for group sessions)
                        if (session.type == SessionType.GROUP) {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                                elevation = CardDefaults.elevatedCardElevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 4.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Participants",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    session.participants.forEach { participant ->
                                        ParticipantRow(
                                            name = participant.name,
                                            status = participant.status,
                                            isAdmin = participant.id == session.adminId
                                        )
                                    }
                                }
                            }
                        }

                        // Admin Controls Card (for session creators)
                        if (isAdmin) {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                                elevation = CardDefaults.elevatedCardElevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 4.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Session Controls",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Session Status Controls
                                    Button(
                                        onClick = { viewModel.toggleSessionActive() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (session.isActive)
                                                MaterialTheme.colorScheme.errorContainer
                                            else
                                                MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = if (session.isActive)
                                                MaterialTheme.colorScheme.onErrorContainer
                                            else
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                if (session.isActive) 
                                                    Icons.Default.Stop 
                                                else 
                                                    Icons.Default.PlayArrow,
                                                contentDescription = null
                                            )
                                            Text(
                                                if (session.isActive) 
                                                    "End Session" 
                                                else 
                                                    "Restart Session"
                                            )
                                        }
                                    }

                                    // Share Button
                                    Button(
                                        onClick = { viewModel.shareSession() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null)
                                            Text("Share Session Code")
                                        }
                                    }

                                    // Delete Button
                                    Button(
                                        onClick = { showDeleteDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null)
                                            Text("Delete Session")
                                        }
                                    }
                                }
                            }
                        }

                        // Session Statistics Card
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.elevatedCardElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Session Statistics",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (session.type == SessionType.GROUP) {
                                    InfoRow(
                                        icon = Icons.Default.Group,
                                        label = "Total Participants",
                                        value = "${session.participants.size}"
                                    )

                                    InfoRow(
                                        icon = Icons.Default.CheckCircle,
                                        label = "Active Participants",
                                        value = "${session.participants.count { it.status == ParticipantStatus.ACTIVE }}"
                                    )

                                    if (session.maxParticipants != null) {
                                        InfoRow(
                                            icon = Icons.Default.Groups,
                                            label = "Max Participants",
                                            value = "${session.maxParticipants}"
                                        )
                                    }
                                }

                                InfoRow(
                                    icon = Icons.Default.Timer,
                                    label = "Session Duration",
                                    value = formatDuration(System.currentTimeMillis() - session.createdAt)
                                )

                                if (session.password?.isNotBlank() == true) {
                                    InfoRow(
                                        icon = Icons.Default.Lock,
                                        label = "Password Protection",
                                        value = "Enabled"
                                    )
                                }
                            }
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (session.isActive) {
                                Button(
                                    onClick = onNavigateToMap,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SafeTrackBlue,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Map, contentDescription = null)
                                        Text("View Map")
                                    }
                                }
                            }
                            
                            if (!isAdmin && !hasLeftSession) {
                                Button(
                                    onClick = { showLeaveDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ExitToApp, contentDescription = null)
                                        Text("Leave")
                                    }
                                }
                            }
                            
                            if (hasLeftSession && session.isActive) {
                                Button(
                                    onClick = { viewModel.rejoinSession() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Login, contentDescription = null)
                                        Text("Rejoin")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Leave Session Dialog
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Session") },
            text = { Text("Are you sure you want to leave this session? You can rejoin later if the session is still active.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.leaveSession()
                        showLeaveDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Remove Session Dialog (for joined sessions)
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Session") },
            text = { 
                Text("Are you sure you want to remove this session from your list? You can always join it again later using the session code.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeJoinedSession()
                        onNavigateBack() // Navigate back after removing
                        showRemoveDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Session Dialog (for admin)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Session") },
            text = { 
                Text("Are you sure you want to delete this session? This will permanently end the session for all participants and cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession()
                        onNavigateBack() // Navigate back after deleting
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SessionTypeChip(type: SessionType) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (type == SessionType.SOLO) 
                    Icons.Default.Person 
                else 
                    Icons.Default.Group,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = type.name,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ParticipantRow(
    name: String,
    status: ParticipantStatus,
    isAdmin: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                contentDescription = null,
                tint = if (isAdmin) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Surface(
            color = when (status) {
                ParticipantStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                ParticipantStatus.LEFT -> MaterialTheme.colorScheme.errorContainer
                ParticipantStatus.INACTIVE -> MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = when (status) {
                ParticipantStatus.ACTIVE -> MaterialTheme.colorScheme.onPrimaryContainer
                ParticipantStatus.LEFT -> MaterialTheme.colorScheme.onErrorContainer
                ParticipantStatus.INACTIVE -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = status.name,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

private fun formatDuration(durationMillis: Long): String {
    val seconds = durationMillis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "$days days"
        hours > 0 -> "$hours hours"
        minutes > 0 -> "$minutes minutes"
        else -> "$seconds seconds"
    }
} 