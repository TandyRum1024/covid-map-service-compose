package com.vin.covidmapservice.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.vin.covidmapservice.navigation.AppNavigationHost
import com.vin.covidmapservice.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MainAppScreen(viewmodel: MainViewModel = hiltViewModel(), onLocationPermissionRequest: () -> Unit) {
    AppNavigationHost(viewmodel = viewmodel, startNavDest = Screen.DebugBeginScreen.navDest, onLocationPermissionRequest = onLocationPermissionRequest)
}

