package com.vin.covidmapservice.presentation

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.vin.covidmapservice.data.CenterCacheDB
import com.vin.covidmapservice.isInPreview
import com.vin.covidmapservice.ui.theme.CovidMapServiceTheme
import dagger.hilt.android.AndroidEntryPoint

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
                    MainAppScreen(viewmodel, onLocationPermissionRequest = {
                        updatePermissions()
                    })
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

@Preview(showBackground = true, widthDp = 320, heightDp = 720)
@Composable
fun DefaultPreview() {
    // Make dummy viewmodel
    val viewmodel = MainViewModelDummy(CenterCacheDB(null))

    CovidMapServiceTheme {
        CompositionLocalProvider(isInPreview provides true) { // Notify the preview state
            MapScreen(viewmodel = viewmodel, onNavigateToDebugMenu = {}, onLocationPermissionRequest = {})
        }
    }
}