package com.vin.covidmapservice

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.compose.CameraPositionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// Dummy Center info for previewing purposes
val dummyCenterData = Center(
    address = "서울특별시 테스트구 테스트로 42길 4242",
    centerName = "테스트",
    facilityName = "테스트의료원 테스트동",
    phoneNumber = "010-XXXX-XXXX",
    updatedAt = "4242-42-42 42:42:42",
    id = -1,
    sido = "sido",
    sigungu = "sigungu",
    zipCode = "zipcode",
    lat = "42",
    lng = "127",
    createdAt = "1996/05/01",
    centerType = "VaccinationCenter",
    org = "HealthyOrganization",
)

@HiltViewModel
open class MainViewModel @Inject constructor(): ViewModel() {
    // screen UI state
    var testValue by mutableStateOf(42)
    var showCenterInfo by mutableStateOf(false)
    // permission state
    var isPermissionGranted by mutableStateOf(false)
    // map related
    var mapCurrentLocation: LatLng by mutableStateOf(LatLng(37.532, 127.024612)) // Seoul latlng
    // list of centers
    var centers = mutableStateListOf<Center>()
        private set
    // currently selected center
    var currentCenter by mutableStateOf( dummyCenterData )

    // Updates current location of the phone
    open fun updateCurrentLocation (context: Activity? = null) {
        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            LocationServices.getFusedLocationProviderClient(context!!)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener {
                    this.mapCurrentLocation = LatLng(it.latitude, it.longitude)
                }
        }
    }
}

class MainViewModelDummy @Inject constructor(): MainViewModel() {
    override fun updateCurrentLocation (context: Activity?) {
        Log.d("CovidMapService", "DUMMY LOCATION UPDATE CALLED")
    }
}