package com.vin.covidmapservice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.*
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.*
import com.vin.covidmapservice.ui.theme.CovidMapServiceTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
                    MainApp()
                }
            }
        }

        // API Test
        // TODO: Move this to the splash screen, caching into the local DB
        CenterAPI.inst.getCenters().enqueue(object: Callback<CenterResponse> {
            override fun onResponse(
                call: Call<CenterResponse>,
                response: Response<CenterResponse>
            ) {
                Log.e("CENTERS", "API CALL RESPONSE:")
                if (response.isSuccessful && response.code() == 200) {
                    viewmodel.centers.clear()
                    for (center in response.body()!!.data) {
                        Log.e("CENTERS", "A\t${center.toString()}")
                        viewmodel.centers.add(center)
                    }
                }
                else {
                    Log.e("CENTERS", "\tFAILED (code: ${response.code()})")
                }
            }

            override fun onFailure(call: Call<CenterResponse>, t: Throwable) {
                Log.e("CENTERS", "API CALL FAILED!")
            }
        })
        //

        // Check for permissions
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
        viewmodel.testValue = 69
    }
}

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun MainApp(
    viewmodel: MainViewModel = hiltViewModel()
) {
    // If in preview, don't call the naver map composable so that it won't break the Preview mode
    val cameraPositionState: CameraPositionState? = if (isInPreview.current) null else rememberCameraPositionState {
        position = CameraPosition(viewmodel.mapCurrentLocation, 11.0)
    }

    Column {
        Text(text = "Viewmodel.testValue = ${viewmodel.testValue}")
        Text(text = "Viewmodel.isPermissionGranted = ${viewmodel.isPermissionGranted}")
        Button(onClick = { viewmodel.showCenterInfo = !viewmodel.showCenterInfo }) {
            Text("DEBUG: Toggle centerinfo()")
        }
        Button(onClick = { cameraPositionState?.move(CameraUpdate.scrollTo(viewmodel.mapCurrentLocation).animate(CameraAnimation.Easing, 1500)) }) {
            Text("To current location")
        }
        Map(
            modifier = Modifier.weight(1f),
            cameraPositionState = cameraPositionState,
            centers = viewmodel.centers,
            onCenterMarkerClick = { marker, center ->
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
            onCurrentPosMarkerClick = {
                cameraPositionState?.move(CameraUpdate.scrollTo(it.position).animate(CameraAnimation.Easing, 1500))
            }
        )
        if (viewmodel.showCenterInfo)
            CenterInfo(viewmodel.currentCenter)
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

@Preview(showBackground = true, widthDp = 320, heightDp = 720)
@Composable
fun DefaultPreview() {
    // Make dummy viewmodel
    val viewmodel = MainViewModelDummy()

    CovidMapServiceTheme {
        CompositionLocalProvider(isInPreview provides true) { // Notify the preview state
            MainApp(viewmodel = viewmodel)
        }
    }
}
