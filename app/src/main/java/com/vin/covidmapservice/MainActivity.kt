package com.vin.covidmapservice

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.NaverMap
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
val previewState = compositionLocalOf { false }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewmodel: MainViewModel by viewModels()

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
    }
}

@Composable
fun MainApp(
    viewmodel: MainViewModel = hiltViewModel()
) {
    Column {
        Text(text = "Viewmodel.testValue = ${viewmodel.testValue}")
        Button(onClick = { viewmodel.showCenterInfo = !viewmodel.showCenterInfo }) {
            Text("Toggle centerinfo()")
        }
        Map(modifier = Modifier.weight(1f))
        if (viewmodel.showCenterInfo)
            CenterInfo()
    }
}

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun Map(
    modifier: Modifier = Modifier
) {
    // Naver map does not like the AS's preview mode, so skip it if needed
    if (!previewState.current) {
        // API test
        CenterAPI.inst.getCenters(perPage = 3).enqueue(object: Callback<CenterResponse> {
            override fun onResponse(
                call: Call<CenterResponse>,
                response: Response<CenterResponse>
            ) {
                Log.e("CENTERS", "API CALL RESPONSE:")
                if (response.isSuccessful && response.code() == 200) {
                    for (center in response.body()!!.data) {
                        Log.e("CENTERS", "A\t${center.toString()}")
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

        // Naver map composable wrapper from: https://github.com/fornewid/naver-map-compose
        NaverMap(modifier = modifier.fillMaxWidth())
    }
    else { // (executes in preview)
        Column(modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.primary), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("MAP SCREEN HERE", textAlign = TextAlign.Center)
        }
    }
}

// Vaccination center information display
@Composable
fun CenterInfo(
    // (dummy center data for preview purposes)
    center: Center = Center(
        address = "서울특별시 테스트구 테스트로 42길 4242",
        centerName = "테스트",
        facilityName = "테스트의료원 테스트동",
        phoneNumber = "010-XXXX-XXXX",
        updatedAt = "4242-42-42 42:42:42",
        id = 0,
        sido = "sido",
        sigungu = "sigungu",
        zipCode = "zipcode",
        lat = "42",
        lng = "127",
        createdAt = "1996/05/01",
        centerType = "VaccinationCenter",
        org = "HealthyOrganization",
    )
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
    CovidMapServiceTheme {
        CompositionLocalProvider(previewState provides true) { // Notify the prewview state
            MainApp()
        }
    }
}
