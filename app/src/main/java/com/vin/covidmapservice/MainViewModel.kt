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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject
import kotlin.math.max
import kotlin.system.measureTimeMillis

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
    var splashProgress by mutableStateOf(0f)
    var splashIsDone by mutableStateOf(false)

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
        if (context == null)
            return
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

    // Updates list of vaccination centers. Returns Flow of Pair<Progress: Float, ResultingCentersList: List<Center>?>
    fun updateAPICache (emulateSlowAPICall: Boolean = false): Flow<Pair<Float, List<Center>?>> = flow {
        var resultData: MutableList<Center> = mutableListOf()
        var isDataReady = false
        var isDataWaiting = false
        var apiPageIndex = 0

        // Launch API cacher coroutine
        CoroutineScope(Dispatchers.IO).launch {
            Log.e("CENTERS", "API CALL COROUTINE BEGIN:")
            resultData.clear()

            val timeTaken = measureTimeMillis {
                for (i in 1..10) {
                    Log.e("CENTERS", "\tPAGE #$i")
                    CenterAPI.inst.getCenters(page = i).enqueue(object: Callback<CenterResponse> {
                        override fun onResponse(
                            call: Call<CenterResponse>,
                            response: Response<CenterResponse>
                        ) {
//                        Log.e("CENTERS", "API CALL RESPONSE:")
                            if (response.isSuccessful && response.code() == 200) {
                                resultData.addAll(response.body()!!.data)
                            }
                            else {
                                Log.e("CENTERS", "\tFAILED (code: ${response.code()})")
                            }
                        }

                        override fun onFailure(call: Call<CenterResponse>, t: Throwable) {
                            Log.e("CENTERS", "API CALL FAILED!")
                        }
                    })

                    // Emulate slow network / API response by... simply waiting longer. (wow!)
                    if (emulateSlowAPICall)
                        delay(500)
                }
            }

            Log.e("CENTERS", "DATA READY; TOTAL ${centers.size} ITEMS (TOOK ${timeTaken * 0.001} s TOTAL)")
            isDataReady = true
        }

        // Increase the progress over the span of 2 seconds
        val progressDelay: Long = 10
        var progressTargetMillis: Long = 2000
        val progressTargetPercent = 1f
        // (calculate the necessary progress counter related values)
        var progress = 0f
        var progressStepNum = progressTargetMillis / progressDelay
        var progressStep: Float // = (progressTargetPercent - progress) / progressStepNum // calculated in loop!

        Log.e("WAIT", "WAIT BEGIN! CURRENT PROGRESS: $progress")
        var timeBegin = System.currentTimeMillis()
        var timeElapsedRef = timeBegin
        var timeElapsed: Long = 0
        while (progress < 1f || !isDataReady) {
            delay(progressDelay)
            emit(Pair(progress, null))

            // Check for data wait, around the 80%
            while (progress >= 0.8f && !isDataReady) {
                if (!isDataWaiting) // show once
                    Log.e("WAIT", "Data is not ready -- Waiting for data! (elapsed: ${(System.currentTimeMillis() - timeBegin) * 0.001}s, step time: ${progressDelay * 0.001}s)")

                isDataWaiting = true
                progress = 0.8f
                delay(progressDelay)
                emit(Pair(progress, null))
            }

            // If we got out of the waiting time, then calculate new progress delays so that it raeches 100% in 0.7 seconds
            if (isDataWaiting) {
                Log.e("WAIT", "Data is FINALLY ready! (progress: ${progress*100}%, elapsed: ${(System.currentTimeMillis() - timeElapsedRef) * 0.001}s, step time: ${progressDelay * 0.001}s)")
                isDataWaiting = false
                timeElapsedRef = System.currentTimeMillis()
                progress = 0.8f
                progressTargetMillis = 700
            }

            timeElapsed = System.currentTimeMillis() - timeElapsedRef
            // (re-calculate the necessary progress counter related values, accounting misc. delays from the code)
            progressStepNum = max(progressTargetMillis - timeElapsed, 1) / progressDelay
            progressStep = (progressTargetPercent - progress) / max(progressStepNum, 1)

//            Log.e("WAIT", "Progress increment! (progress: ${progress*100}% (+ ${progressStep*100}%), elapsed: ${(System.currentTimeMillis() - timeBegin) * 0.001}s, step time: ${progressDelay * 0.001}s)")
            progress += progressStep
        }

        Log.e("WAIT", "WAIT ENDED! CURRENT PROGRESS: $progress, (elapsed: ${(System.currentTimeMillis() - timeBegin) * 0.001}s total)")
        Log.e("WAIT", "DATA PREVIEW: ")
        for (i in 0..9) {
            Log.e("WAIT", "\t${resultData[i].toString()}")
        }
        emit(Pair(1f, resultData))
    }
}

class MainViewModelDummy @Inject constructor(): MainViewModel() {
    override fun updateCurrentLocation (context: Activity?) {
        Log.d("CovidMapService", "DUMMY LOCATION UPDATE CALLED")
    }
}