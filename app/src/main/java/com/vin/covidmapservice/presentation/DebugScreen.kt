package com.vin.covidmapservice.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vin.covidmapservice.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DebugInitScreen(
    viewmodel: MainViewModel = hiltViewModel(),
    onNavigateToSplashScreen: () -> Unit,
) {
    var dbSize by remember{ mutableStateOf(0) }
    val snackbarHostState = remember{ SnackbarHostState() }

    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            dbSize = viewmodel.getDBCount()
        }
    }

    Column {
        Text(text = stringResource(R.string.debug_begin_title))
        Spacer(modifier = Modifier.size(width = 0.dp, height = 16.dp))
        Text(text = "${stringResource(R.string.debug_permission)} = ${viewmodel.isPermissionGranted}")
        Text(text = "${stringResource(R.string.debug_cache_size)} = ${viewmodel.cachedCenters.size}")
        Text(text = "${stringResource(R.string.debug_db_size)} = ${dbSize}")
        Text(text = "${stringResource(R.string.debug_slowapi)} = ${viewmodel.debugEmulateSlowAPI}")
        Checkbox(checked = viewmodel.debugEmulateSlowAPI, onCheckedChange = {
            viewmodel.debugEmulateSlowAPI = it
        })
        Button(
            onClick = {
                // Clear DB in IO coroutine and notify
                CoroutineScope(Dispatchers.IO).launch {
                    viewmodel.clearCacheDB()
                    dbSize = viewmodel.getDBCount()
                    snackbarHostState.showSnackbar("Successfully cleared Room DB (DB size: $dbSize)")
                }
            }
        ) {
            Text(stringResource(R.string.debug_db_clear))
        }
        Button(
            onClick = onNavigateToSplashScreen
        ) {
            Text(stringResource(R.string.debug_begin))
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
fun DebugScreen(
    viewmodel: MainViewModel = hiltViewModel()
) {
    var dbSize by remember{ mutableStateOf(0) }
    val currentCoroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember{ SnackbarHostState() }

    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            dbSize = viewmodel.getDBCount()
        }
    }

    Column {
        Text(text = stringResource(R.string.debug_title))
        Spacer(modifier = Modifier.size(width = 0.dp, height = 16.dp))
        Text(text = "${stringResource(R.string.debug_permission)} = ${viewmodel.isPermissionGranted}")
        Text(text = "${stringResource(R.string.debug_cache_size)} = ${viewmodel.cachedCenters.size}")
        Text(text = "${stringResource(R.string.debug_db_size)} = ${dbSize}")
        Text(text = "${stringResource(R.string.debug_slowapi)} = ${viewmodel.debugEmulateSlowAPI}")
        Checkbox(checked = viewmodel.debugEmulateSlowAPI, onCheckedChange = {
            viewmodel.debugEmulateSlowAPI = it
        })
        Button(
            onClick = {
                // Clear DB in IO coroutine and notify
                CoroutineScope(Dispatchers.IO).launch {
                    if (viewmodel.clearCacheDB()) {
                        dbSize = viewmodel.getDBCount()
                        snackbarHostState.showSnackbar("Successfully cleared Room DB (DB size: $dbSize)")
                    }
                    else {
                        snackbarHostState.showSnackbar("Room DB currently in use!")
                    }
                }
            }
        ) {
            Text(stringResource(R.string.debug_db_clear))
        }
        Button(
            onClick = {
                val res = viewmodel.updateAPICache(
                    onDone = {
                        CoroutineScope(Dispatchers.IO).launch {
                            dbSize = viewmodel.getDBCount()
                            viewmodel.beginCollectingCachedCenter()
                            snackbarHostState.showSnackbar("Successfully updated API cache DB (DB size: $dbSize)")
                        }
                    },
                    onProgressUpdate = {prog, isDone -> }
                )
                if (!res) {
                    currentCoroutineScope.launch {
                        snackbarHostState.showSnackbar("Room DB currently in use!")
                    }
                }
            }
        ) {
            Text(stringResource(R.string.debug_db_recache))
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}
