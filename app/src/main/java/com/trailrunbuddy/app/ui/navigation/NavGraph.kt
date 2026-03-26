package com.trailrunbuddy.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trailrunbuddy.app.platform.service.SessionServiceConnection
import com.trailrunbuddy.app.ui.activesession.ActiveSessionScreen
import com.trailrunbuddy.app.ui.profiledetail.ProfileDetailScreen
import com.trailrunbuddy.app.ui.profilelist.ProfileListScreen
import com.trailrunbuddy.app.ui.settings.SettingsScreen

@Composable
fun NavGraph(
    sessionServiceConnection: SessionServiceConnection =
        hiltViewModel<NavGraphViewModel>().sessionServiceConnection
) {
    val navController = rememberNavController()
    val activeProfileId by sessionServiceConnection.activeProfileId.collectAsStateWithLifecycle()

    // Start destination: active session → go there directly (RS-2, NI-2)
    val startDestination = if (activeProfileId != null) {
        Screen.ActiveSession(activeProfileId!!).createRoute()
    } else {
        Screen.ProfileList.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.ProfileList.route) {
            ProfileListScreen(
                onNavigateToProfileDetail = { profileId ->
                    navController.navigate(Screen.ProfileDetail(profileId).createRoute())
                },
                onNavigateToNewProfile = {
                    navController.navigate(Screen.ProfileDetail.newProfile)
                },
                onNavigateToActiveSession = { profileId ->
                    navController.navigate(Screen.ActiveSession(profileId).createRoute())
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.ProfileDetail.ROUTE,
            arguments = listOf(
                navArgument(Screen.ProfileDetail.ARG_PROFILE_ID) { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getLong(Screen.ProfileDetail.ARG_PROFILE_ID) ?: -1L
            ProfileDetailScreen(
                profileId = profileId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ActiveSession.ROUTE,
            arguments = listOf(
                navArgument(Screen.ActiveSession.ARG_PROFILE_ID) { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getLong(Screen.ActiveSession.ARG_PROFILE_ID) ?: -1L
            ActiveSessionScreen(
                profileId = profileId,
                onNavigateToProfileList = {
                    navController.navigate(Screen.ProfileList.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
