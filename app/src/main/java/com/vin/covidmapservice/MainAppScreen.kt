package com.vin.covidmapservice

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MainAppScreen(viewmodel: MainViewModel = hiltViewModel(), onLocationPermissionRequest: () -> Unit) {
    AppNavigationHost(viewmodel = viewmodel, startNavDest = Screen.SplashScreen.navDest, onLocationPermissionRequest = onLocationPermissionRequest)
}

@Composable
fun DebugScreen(
    viewmodel: MainViewModel = hiltViewModel()
) {
    val currentCoroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember{ SnackbarHostState() }

    Column {
        Text(text = "viewmodel.isPermissionGranted = ${viewmodel.isPermissionGranted}")
        Text(text = "viewmodel.cachedCenters.size = ${viewmodel.cachedCenters.size}")
        Button(
            onClick = {
                // Clear DB in IO coroutine and notify
                CoroutineScope(Dispatchers.IO).launch {
                    viewmodel.clearCacheDB()
                    val dbLen = viewmodel.getDBCount()
                    snackbarHostState.showSnackbar("Successfully cleared Room DB (DB size: $dbLen)")
                }
            }
        ) {
            Text("Clear cache Room DB")
        }
        Button(
            onClick = {
                viewmodel.updateAPICache(
                    onDone = {
                        CoroutineScope(Dispatchers.IO).launch {
                            val dbLen = viewmodel.getDBCount()
                            snackbarHostState.showSnackbar("Successfully updated API cache DB (DB size: $dbLen)")
                        }
                    },
                    onProgressUpdate = {prog, isDone -> }
                )
            }
        ) {
            Text("Force DB re-cache")
        }
    }
    SnackbarHost(hostState = snackbarHostState)
}
