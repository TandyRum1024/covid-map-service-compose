package com.vin.covidmapservice.presentation

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.naver.maps.geometry.LatLng
import com.vin.covidmapservice.data.api.CenterAPI
import com.vin.covidmapservice.domain.Center
import com.vin.covidmapservice.data.CenterCacheDB
import com.vin.covidmapservice.domain.centerModelToCenter
import com.vin.covidmapservice.dummyCenterData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.math.max
import kotlin.system.measureTimeMillis

// Viewmodel that contains the state of app. DI'd by Dagger Hilt for ease of injection
@HiltViewModel
open class MainViewModel @Inject constructor(private val cacheDB: CenterCacheDB): ViewModel() {
    // Debug
    var debugEmulateSlowAPI by mutableStateOf(false)

    // Screen UI state
    var isMarkerSelected by mutableStateOf(false)
    var splashProgress by mutableStateOf(0f)

    // Permission state
    var isPermissionGranted by mutableStateOf(false)
    // Last known LatLng position of phone (if available)
    var mapCurrentLocation: LatLng by mutableStateOf(LatLng(37.532, 127.024612)) // Seoul latlng
    // List of centers, fetched from Cache DB
    var cachedCenters = mutableStateListOf<Center>()
    // (currently running cached center collector)
    var currentCenterFetcher: Job? = null
    // Currently selected center's data
    var currentCenter by mutableStateOf( dummyCenterData )

    // Exposed DB
    var isDBInUse = false
    open fun getDBCount (): Int {
        return cacheDB.getCount()
    }
    open fun clearCacheDB (): Boolean {
        // In normal use cases rapid DB use won't probably happen, but check if the DB is currently in use just in case:
        if (isDBInUse) return false
        isDBInUse = true
        cacheDB.deleteAll()
        isDBInUse = false
        return true
    }
    // (for debug!)
    open fun dumpDB () {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("MainViewModel", "-------- CACHE DB DUMP --------")
            val list: MutableList<Center> = mutableListOf()
            cacheDB.getAll().collectLatest() {
                list.clear()
                list.addAll(it)
                for (center in list) {
                    Log.d("MainViewModel", "\t${center.toString()}")
                }
                Log.d("MainViewModel", "-------- CACHE DB DUMP END --------")
                cancel()
            }
        }
    }

    // Updates current location of the phone
    open fun updateCurrentLocation (context: Activity? = null) {
        if (context == null)
            return
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener {
                    this.mapCurrentLocation = LatLng(it.latitude, it.longitude)
                }
        }
    }

    // Updates cached list (and DB) of vaccination centers. Returns Flow of Pair<Progress: Float, IsDone: Boolean>
    // If you intend to update the API Cache, please use updateAPICache() instead as they offer callback based operation instead of manually invocating coroutines
    open fun updateAPICacheAndReturnFlow (
        emulateSlowAPICall: Boolean = false,
        dataReady: Boolean = false,
    ): Flow<Pair<Float, Boolean>> = flow {
        var isDataWaiting = false
        var isDataReady = dataReady

        // Launch API cacher coroutine if data is not ready
        if (!isDataReady) {
            CoroutineScope(Dispatchers.IO).launch {
                Log.e("CENTERS", "API CALL COROUTINE BEGIN:")
                cacheDB.deleteAll()
                cachedCenters.clear()

                val timeTaken = measureTimeMillis {
                    CenterAPI.callAPI(
                        onAPIPageDone = { page, res ->
                            // Insert transformed data to cache DB
                            CoroutineScope(Dispatchers.IO).launch {
                                Log.d("CENTERS", "\tPAGE #$page")
                                for (res in res) {
                                    cacheDB.insert(centerModelToCenter(res))
                                }
                            }
                        },
                        onAPIFailed = { page, res ->
                            Log.e("CENTERS", "\tAPI CALL FAILED ON PAGE $page (code: ${res})")
                        },
                        emulateSlowAPICall = emulateSlowAPICall
                    )
                }

                CoroutineScope(Dispatchers.IO).launch {
                    Log.e("CENTERS", "DATA READY; TOTAL ${cacheDB.getCount()} ITEMS (TOOK ${timeTaken * 0.001} s TOTAL)")
                }
                isDataReady = true
            }
        }

        // Increase the progress over the span of 2 seconds
        val progressDelay: Long = 10
        var progressTargetMillis: Long = 2000
        val progressTargetPercent = 1f
        // (calculate the necessary progress counter related values)
        var progress = 0f
        var progressStepNum: Long // = progressTargetMillis / progressDelay
        var progressStep: Float // = (progressTargetPercent - progress) / progressStepNum // calculated in loop!

        Log.e("WAIT", "WAIT BEGIN! CURRENT PROGRESS: $progress")
        val timeBegin = System.currentTimeMillis()
        var timeElapsedRef = timeBegin
        var timeElapsed: Long
        while (progress < 1f || !isDataReady) {
            delay(progressDelay)
            emit(Pair(progress, false))

            // Check for data wait, around the 80%
            while (progress >= 0.8f && !isDataReady) {
                if (!isDataWaiting) // show once
                    Log.e("WAIT", "Data is not ready -- Waiting for data! (elapsed: ${(System.currentTimeMillis() - timeBegin) * 0.001}s, step time: ${progressDelay * 0.001}s)")

                isDataWaiting = true
                progress = 0.8f
                delay(progressDelay)
                emit(Pair(progress, false))
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
            progress += progressStep
        }

        Log.e("WAIT", "WAIT ENDED! CURRENT PROGRESS: $progress, (elapsed: ${(System.currentTimeMillis() - timeBegin) * 0.001}s total)")
        emit(Pair(1f, true))
    }
    // Updates list of vaccination centers. Calls corresponding callbacks
    // Wrapper of updateAPICacheAndReturnFlow()
    open fun updateAPICache (onProgressUpdate: (Float, Boolean) -> Unit, onDone: () -> Unit, skipAPICall: Boolean = false, emulateSlowAPICall: Boolean = false): Boolean {
        // In normal use cases rapid DB use won't probably happen, but check if the DB is currently in use just in case:
        if (isDBInUse) return false
        isDBInUse = true
        // Launch API cacher coroutine in which we get the flow of progress from updateAPICacheFlow():
        CoroutineScope(Dispatchers.IO).launch {
            updateAPICacheAndReturnFlow(dataReady = skipAPICall, emulateSlowAPICall =  emulateSlowAPICall).collectLatest() {
                onProgressUpdate(it.first, it.second)
                if (it.second) { // done
                    onDone()
                    isDBInUse = false
                }
            }
        }
        return true
    }

    // Updates list of vaccination centers. Returns Flow of Pair<Progress: Float, IsDone: Boolean>
    open fun beginCollectingCachedCenter () {
        if (currentCenterFetcher == null || currentCenterFetcher!!.isActive) {
            // Halt previously running fetcher coroutine
            if (currentCenterFetcher != null)
                currentCenterFetcher!!.cancel()
            // Launch API cache fetcher coroutine
            currentCenterFetcher = viewModelScope.launch(Dispatchers.IO) {
                // Add the DB contents to the cache list
                cacheDB.getAll().collectLatest {
                    cachedCenters.clear()
                    cachedCenters.addAll(it)
                }
            }
        }
    }
}

class MainViewModelDummy @Inject constructor(cacheDB: CenterCacheDB): MainViewModel(cacheDB) {
    override fun updateCurrentLocation (context: Activity?) {
        Log.d("CovidMapService", "DUMMY LOCATION UPDATE CALLED")
    }

    override fun updateAPICache(
        onProgressUpdate: (Float, Boolean) -> Unit,
        onDone: () -> Unit,
        skipAPICall: Boolean,
        emulateSlowAPICall: Boolean
    ): Boolean { return false }
    override fun beginCollectingCachedCenter() { }

    override fun clearCacheDB(): Boolean { return false }
    override fun getDBCount (): Int { return 0 }
    override fun dumpDB () { }
}