package com.vin.covidmapservice.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vin.covidmapservice.presentation.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/*
    Define all available navigation / screen states
*/
sealed class Screen(val navDest: String) {
    object SplashScreen: Screen("splash")
    object MapScreen: Screen("map")
    object DebugScreen: Screen("debugmenu")
    object DebugBeginScreen: Screen("debugbegin")
}

// Navigation host
@Composable
fun AppNavigationHost (
    viewmodel: MainViewModel = hiltViewModel(),
    startNavDest: String,
    onLocationPermissionRequest: () -> Unit,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startNavDest) {
        composable(Screen.SplashScreen.navDest) {
            SplashScreen(viewmodel = viewmodel, onNavigateToMapScreen = {
                // Make sure to update the navigation on the main thread -- as onNavigateToMapScreen executes on the Dispatchers.IO scope
                CoroutineScope(Dispatchers.Main).launch {
                    navController.navigate(Screen.MapScreen.navDest) {
                        popUpTo(Screen.SplashScreen.navDest) { inclusive = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            })
        }
        composable(Screen.MapScreen.navDest) {
            MapScreen(viewmodel = viewmodel, onNavigateToDebugMenu = {
                // Make sure to update the navigation on the main thread
                CoroutineScope(Dispatchers.Main).launch {
                    navController.navigate(Screen.DebugScreen.navDest) {
                        popUpTo(Screen.MapScreen.navDest)
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            onLocationPermissionRequest = onLocationPermissionRequest)
        }
        composable(Screen.DebugBeginScreen.navDest) {
            DebugInitScreen(viewmodel = viewmodel, onNavigateToSplashScreen = {
                // Make sure to update the navigation on the main thread
                CoroutineScope(Dispatchers.Main).launch {
                    navController.navigate(Screen.SplashScreen.navDest) {
                        popUpTo(Screen.DebugBeginScreen.navDest) { inclusive = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            })
        }
        composable(Screen.DebugScreen.navDest) { DebugScreen(viewmodel = viewmodel) }
    }
}