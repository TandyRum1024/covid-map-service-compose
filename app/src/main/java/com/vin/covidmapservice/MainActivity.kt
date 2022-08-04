package com.vin.covidmapservice

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.CameraPositionState
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.rememberCameraPositionState
import com.vin.covidmapservice.ui.theme.CovidMapServiceTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Global state for preview flag as Naver map view does not like the Preview mode
val isInPreview = compositionLocalOf { false }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewmodel: MainViewModel by viewModels()
    // list of permissions to check on startup
    private val locationPermissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
    private val PERMISSION_REQ_ID = 4242

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CovidMapServiceTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    MainAppScreen(viewmodel)
                }
            }
        }

        updatePermissions()
    }

    fun updatePermissions () {
        // Check for permissions, and requests it if needed
        // https://se-jung-h.tistory.com/entry/AndroidKotlin-%EB%84%A4%EC%9D%B4%EB%B2%84-%EC%A7%80%EB%8F%84-%ED%98%84%EC%9E%AC-%EC%9C%84%EC%B9%98
        var isPermitted = true
        for (perm in locationPermissions)
        {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                isPermitted = false
                break
            }
        }
        if (isPermitted) {
            viewmodel.isPermissionGranted = true
            viewmodel.updateCurrentLocation(this)
        }
        else {
            ActivityCompat.requestPermissions(this, locationPermissions, PERMISSION_REQ_ID)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var permResult = true
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                permResult = false
                break
            }
        }

        var callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.latitude
            }
        }

        viewmodel.updateCurrentLocation(this)
        viewmodel.isPermissionGranted = permResult
    }
}

@Composable
fun MainAppScreen(viewmodel: MainViewModel = hiltViewModel()) {
    AppNavigationHost(viewmodel = viewmodel, startNavDest = Screen.SplashScreen.navDest)
}

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun MapScreen(
    viewmodel: MainViewModel = hiltViewModel(),
    onNavigateToDebugMenu: () -> Unit,
) {
    // If in preview, don't call the naver map composable so that it won't break the Preview mode
    val cameraPositionState: CameraPositionState? = if (isInPreview.current) null else rememberCameraPositionState {
        position = CameraPosition(viewmodel.mapCurrentLocation, 11.0)
    }

    Column {
        Button(onClick = { viewmodel.showCenterInfo = !viewmodel.showCenterInfo }) {
            Text("DEBUG: Toggle centerinfo()")
        }
        Button(onClick = onNavigateToDebugMenu) {
            Text("DEBUG: Menu")
        }
        Button(onClick = { cameraPositionState?.move(CameraUpdate.scrollTo(viewmodel.mapCurrentLocation).animate(CameraAnimation.Easing, 1500)) }) {
            Text("To current location")
        }
        Map(
            viewmodel = viewmodel,
            modifier = Modifier.weight(1f),
            cameraPositionState = cameraPositionState,
            centers = if (isInPreview.current) listOf<Center>() else viewmodel.cachedCenters,
            onCenterMarkerClick = if (isInPreview.current) { marker, center ->  } else { marker, center ->
                // Toggle the center info screen
                if (viewmodel.showCenterInfo && viewmodel.currentCenter.id == center.id) {
                    viewmodel.showCenterInfo = false
                }
                else {
                    viewmodel.showCenterInfo = true
                    viewmodel.currentCenter = center
                }
                cameraPositionState?.move(CameraUpdate.zoomIn().animate(CameraAnimation.Easing, 1500))
                cameraPositionState?.move(CameraUpdate.scrollTo(marker.position).animate(CameraAnimation.Easing, 1500))
            },
            onCurrentPosMarkerClick = if (isInPreview.current) { marker ->  } else { marker ->
                cameraPositionState?.move(CameraUpdate.scrollTo(marker.position).animate(CameraAnimation.Easing, 1500))
            }
        )
        // Surface wrapper for 'animated' slide-in of center information
        Surface (modifier = Modifier.animateContentSize()) {
            if (viewmodel.showCenterInfo)
                CenterInfo(center = viewmodel.currentCenter)
        }
    }
}

// Vaccination center information display
@Composable
fun CenterInfo(
    // (dummy center data for preview purposes)
    center: Center = dummyCenterData
) {
    Surface() {
        Column {
            Text("Address: ${center.address}")
            Text("Name: ${center.centerName}")
            Text("Facility: ${center.facilityName}")
            Text("TEL: ${center.phoneNumber}")
            Text("Last updated: ${center.updatedAt}")
        }
    }
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
                            snackbarHostState.showSnackbar("Successfully updated API cache DB (DB size: $dbLen)", duration = SnackbarDuration.Long)
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

@Preview(showBackground = true, widthDp = 320, heightDp = 720)
@Composable
fun DefaultPreview() {
    // Make dummy viewmodel
    val viewmodel = MainViewModelDummy(CenterCacheDB(null))

    CovidMapServiceTheme {
        CompositionLocalProvider(isInPreview provides true) { // Notify the preview state
            MapScreen(viewmodel = viewmodel, onNavigateToDebugMenu = {})
        }
    }
}

