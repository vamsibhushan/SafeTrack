package com.example.safetrack.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.safetrack.auth.presentation.login.LoginScreen
import com.example.safetrack.auth.presentation.register.RegisterScreen
import com.example.safetrack.auth.presentation.forgotpassword.ForgotPasswordScreen
import com.example.safetrack.home.presentation.HomeScreen
import com.example.safetrack.map.presentation.MapScreen
import com.example.safetrack.profile.presentation.ProfileScreen
import com.example.safetrack.session.presentation.*
import com.example.safetrack.settings.presentation.SettingsScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object Map : Screen("map/{sessionId}") {
        fun createRoute(sessionId: String = "default") = "map/$sessionId"
        const val deepLinkUrl = "safetrack://map/{sessionId}"
    }
    object GroupTrackingSetup : Screen("group_tracking_setup")
    object SoloTrackingSetup : Screen("solo_tracking_setup")
    object SessionDetails : Screen("session_details/{sessionId}") {
        fun createRoute(sessionId: String) = "session_details/$sessionId"
        const val deepLinkUrl = "safetrack://session/{sessionId}"
    }
    object JoinSession : Screen("join_session")
}

private const val ANIMATION_DURATION = 300

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    onNavigateToProfile: () -> Unit,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route?.substringBefore("/")

    // Track active session for Map tab
    var activeSessionId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar(currentRoute)) {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "Home",
                                modifier = Modifier.size(26.dp)
                            )
                        },
                        label = { Text("Home", style = MaterialTheme.typography.labelMedium) },
                        selected = currentRoute == Screen.Home.route,
                        onClick = {
                            navController.navigateToTopLevel(Screen.Home.route)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = "Map",
                                modifier = Modifier.size(26.dp)
                            )
                        },
                        label = { Text("Map", style = MaterialTheme.typography.labelMedium) },
                        selected = currentRoute?.startsWith(Screen.Map.route.substringBefore("/")) == true,
                        onClick = {
                            navController.navigateToTopLevel(Screen.Map.createRoute(activeSessionId ?: "default"))
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                modifier = Modifier.size(26.dp)
                            )
                        },
                        label = { Text("Settings", style = MaterialTheme.typography.labelMedium) },
                        selected = currentRoute == Screen.Settings.route,
                        onClick = {
                            navController.navigateToTopLevel(Screen.Settings.route)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(
                route = Screen.Login.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(ANIMATION_DURATION))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(ANIMATION_DURATION))
                }
            ) {
                LoginScreen(
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onLoginSuccess = { 
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) }
                )
            }

            composable(
                route = Screen.Register.route,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(ANIMATION_DURATION)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(ANIMATION_DURATION)
                    )
                }
            ) {
                RegisterScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.ForgotPassword.route,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(ANIMATION_DURATION)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(ANIMATION_DURATION)
                    )
                }
            ) {
                ForgotPasswordScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Home.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(ANIMATION_DURATION))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(ANIMATION_DURATION))
                }
            ) {
                HomeScreen(
                    onNavigateToSoloSetup = { navController.navigate(Screen.SoloTrackingSetup.route) },
                    onNavigateToGroupSetup = { navController.navigate(Screen.GroupTrackingSetup.route) },
                    onNavigateToJoinSession = { navController.navigate(Screen.JoinSession.route) },
                    onNavigateToSessionDetails = { sessionId ->
                        navController.navigate(Screen.SessionDetails.createRoute(sessionId))
                    }
                )
            }

            composable(
                route = Screen.Profile.route,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(ANIMATION_DURATION)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(ANIMATION_DURATION)
                    )
                }
            ) {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Settings.route
            ) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onSignOut = {
                        // Clear all back stack and navigate to login
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.Map.route,
                arguments = listOf(
                    navArgument("sessionId") { 
                        type = NavType.StringType 
                        nullable = true 
                        defaultValue = null 
                    }
                ),
                deepLinks = listOf(
                    navDeepLink { uriPattern = Screen.Map.deepLinkUrl }
                ),
                enterTransition = {
                    fadeIn(animationSpec = tween(ANIMATION_DURATION))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(ANIMATION_DURATION))
                }
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId")
                MapScreen(
                    sessionId = sessionId,
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToSessionDetails = { sessionId ->
                        navController.navigate(Screen.SessionDetails.createRoute(sessionId))
                    }
                )
            }

            composable(
                route = Screen.GroupTrackingSetup.route,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(ANIMATION_DURATION)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(ANIMATION_DURATION)
                    )
                }
            ) {
                GroupTrackingSetupScreen(
                    onSessionCreated = { sessionId -> 
                        activeSessionId = sessionId
                        navController.navigate(Screen.SessionDetails.createRoute(sessionId)) {
                            popUpTo(Screen.GroupTrackingSetup.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.SoloTrackingSetup.route,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(ANIMATION_DURATION)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(ANIMATION_DURATION)
                    )
                }
            ) {
                SoloTrackingSetupScreen(
                    onSessionCreated = { sessionId -> 
                        activeSessionId = sessionId
                        navController.navigate(Screen.SessionDetails.createRoute(sessionId)) {
                            popUpTo(Screen.SoloTrackingSetup.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.SessionDetails.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
                deepLinks = listOf(
                    navDeepLink { uriPattern = Screen.SessionDetails.deepLinkUrl }
                ),
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(ANIMATION_DURATION)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(ANIMATION_DURATION)
                    )
                }
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
                activeSessionId = sessionId
                SessionDetailsScreen(
                    sessionId = sessionId,
                    onNavigateToMap = { 
                        navController.navigate(Screen.Map.createRoute(sessionId))
                    },
                    onNavigateBack = { 
                        navController.popBackStack()
                        // Clear active session if we're going back to Home
                        if (navController.previousBackStackEntry?.destination?.route == Screen.Home.route) {
                            activeSessionId = null
                        }
                    }
                )
            }

            composable(
                route = Screen.JoinSession.route,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(ANIMATION_DURATION)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(ANIMATION_DURATION)
                    )
                }
            ) {
                JoinSessionScreen(
                    onSessionJoined = { sessionId -> 
                        activeSessionId = sessionId
                        navController.navigate(Screen.SessionDetails.createRoute(sessionId)) {
                            popUpTo(Screen.JoinSession.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun shouldShowBottomBar(currentRoute: String?): Boolean {
    return currentRoute in setOf(
        Screen.Home.route,
        "map", // Match the base route without sessionId
        Screen.Settings.route
    )
}

private fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}