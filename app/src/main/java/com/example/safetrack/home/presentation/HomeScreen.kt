package com.example.safetrack.home.presentation

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.safetrack.core.domain.model.Session
import com.example.safetrack.core.domain.model.SessionType
import java.text.SimpleDateFormat
import java.util.*
import timber.log.Timber
import com.example.safetrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSoloSetup: () -> Unit,
    onNavigateToGroupSetup: () -> Unit,
    onNavigateToJoinSession: () -> Unit,
    onNavigateToSessionDetails: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Handle sharing
    LaunchedEffect(uiState) {
        if (uiState is HomeUiState.Success) {
            val state = uiState as HomeUiState.Success
            state.shareText?.let { text ->
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Session"))
                viewModel.clearShareSession()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SafeTrack",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = SafeTrackDarkBlue
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SafeTrackWhite,
                    titleContentColor = SafeTrackDarkBlue
                )
            )
        }
    ) { padding ->
        when (uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SafeTrackBlue)
                }
            }
            is HomeUiState.Error -> {
                val state = uiState as HomeUiState.Error
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = viewModel::retryLoadingSessions,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafeTrackBlue,
                            contentColor = SafeTrackWhite
                        )
                    ) {
                        Text("Retry")
                    }
                }
            }
            is HomeUiState.Success -> {
                val state = uiState as HomeUiState.Success
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Action Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ActionCard(
                            title = "Solo Session",
                            icon = Icons.Default.Person,
                            containerColor = SafeTrackBlue,
                            contentColor = SafeTrackWhite,
                            onClick = onNavigateToSoloSetup,
                            modifier = Modifier.weight(1f)
                        )
                        ActionCard(
                            title = "Group Session",
                            icon = Icons.Default.Group,
                            containerColor = SafeTrackDarkBlue,
                            contentColor = SafeTrackWhite,
                            onClick = onNavigateToGroupSetup,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Join Session Button
                    Button(
                        onClick = onNavigateToJoinSession,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafeTrackLightBlue,
                            contentColor = SafeTrackDarkBlue
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Join")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Join Session")
                        }
                    }

                    // Sessions Lists
                    SessionsList(
                        title = "My Sessions",
                        sessions = state.mySessions,
                        onSessionClick = onNavigateToSessionDetails,
                        emptyMessage = "No sessions created yet",
                        isMySession = true,
                        onShareSession = { session -> viewModel.shareSession(session) }
                    )

                    SessionsList(
                        title = "Joined Sessions",
                        sessions = state.joinedSessions,
                        onSessionClick = onNavigateToSessionDetails,
                        emptyMessage = "No sessions joined yet",
                        isMySession = false,
                        onDeleteSession = { session -> viewModel.removeJoinedSession(session) }
                    )

                    // Add bottom padding for better scrolling experience
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(120.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp,
            focusedElevation = 4.dp,
            hoveredElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun SessionsList(
    title: String,
    sessions: List<Session>,
    onSessionClick: (String) -> Unit,
    emptyMessage: String,
    isMySession: Boolean = false,
    onShareSession: ((Session) -> Unit)? = null,
    onDeleteSession: ((Session) -> Unit)? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (sessions.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = SafeTrackDarkBlue,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            sessions.forEach { session ->
                SessionCard(
                    session = session,
                    onClick = { onSessionClick(session.id) },
                    isMySession = isMySession,
                    onShareSession = onShareSession,
                    onDeleteSession = onDeleteSession
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = SafeTrackLightBlue.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isMySession) Icons.Default.AddCircle else Icons.Default.Group,
                        contentDescription = null,
                        tint = SafeTrackBlue.copy(alpha = 0.7f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = SafeTrackDarkBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SafeTrackBlue.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: Session,
    onClick: () -> Unit,
    isMySession: Boolean = false,
    onShareSession: ((Session) -> Unit)? = null,
    onDeleteSession: ((Session) -> Unit)? = null
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SafeTrackWhite
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp,
            focusedElevation = 4.dp,
            hoveredElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = SafeTrackDarkBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (session.type) {
                                SessionType.SOLO -> Icons.Default.Person
                                SessionType.GROUP -> Icons.Default.Group
                            },
                            contentDescription = null,
                            tint = SafeTrackBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when (session.type) {
                                SessionType.SOLO -> "Solo Session"
                                SessionType.GROUP -> "${session.participants.size} participants"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = SafeTrackBlue
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SafeTrackBlue.copy(alpha = 0.5f)
                        )
                        Text(
                            text = formatDate(session.startTime),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SafeTrackBlue
                        )
                    }
                }
                SessionStatusChip(isActive = session.isActive)
            }
        }
    }
}

@Composable
private fun SessionStatusChip(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isActive) 
            SafeTrackBlue
        else 
            SafeTrackLightBlue,
        contentColor = if (isActive)
            SafeTrackWhite
        else
            SafeTrackDarkBlue,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.Circle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(8.dp)
            )
            Text(
                text = if (isActive) "Active" else "Inactive",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return format.format(date)
}