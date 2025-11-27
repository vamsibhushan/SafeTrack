package com.example.safetrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.safetrack.navigation.AppNavGraph
import com.example.safetrack.settings.data.UserPreferencesRepository
import com.example.safetrack.ui.theme.SafeTrackTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val userPreferences = userPreferencesRepository.userPreferencesFlow.collectAsStateWithLifecycle(
                initialValue = null
            ).value

            SafeTrackTheme(
                darkTheme = userPreferences?.isDarkTheme ?: false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(
                        onNavigateToProfile = { /* Profile navigation will be handled within AppNavGraph */ },
                        onSignOut = {
                            // Clear all preferences and navigate to login
                            activityScope.launch {
                                userPreferencesRepository.updateLastActiveSessionId(null)
                                userPreferencesRepository.updateLocationTrackingEnabled(false)
                            }
                        }
                    )
                }
            }
        }
    }
}