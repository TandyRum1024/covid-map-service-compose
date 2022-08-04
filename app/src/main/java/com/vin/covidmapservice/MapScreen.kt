@file:OptIn(ExperimentalNaverMapApi::class)

package com.vin.covidmapservice

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.*
import com.naver.maps.map.overlay.Marker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun Map(
    modifier: Modifier = Modifier,
    viewmodel: MainViewModel = hiltViewModel(),
    centers: List<Center> = emptyList<Center>(),
    onCurrentPosMarkerClick: (Marker) -> Unit,
    onCenterMarkerClick: (Marker, Center) -> Unit,
    cameraPositionState: CameraPositionState? = null
) {
    // Naver map does not like the AS's Compose preview mode, so skip it if needed
    if (!isInPreview.current) {
        // Naver map composable wrapper from: https://github.com/fornewid/naver-map-compose
        NaverMap(modifier = modifier.fillMaxWidth(), cameraPositionState = cameraPositionState!!) {
            MarkerCurrentPos(currentLocation = viewmodel.mapCurrentLocation, onClick = onCurrentPosMarkerClick)
            for (center in centers) {
                MarkerCenter(center = center, onClick = onCenterMarkerClick)
            }
        }
    }
    else { // (executes in preview)
        Column(modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.primary), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("MAP SCREEN HERE", textAlign = TextAlign.Center)
        }
    }
}

// Marker: Current pos.
@Composable
fun MarkerCurrentPos (
    modifier: Modifier = Modifier,
    currentLocation: LatLng = LatLng(37.532, 127.024612), // Seoul latlng
    onClick: (Marker) -> Unit
) {
    Marker(
        state = MarkerState(position = currentLocation),
        iconTintColor = Color.Red,
        captionText = "HERE!",
        onClick = {
            onClick(it)
            true
        }
    )
}

// Marker: Vaccination centers
@Composable
fun MarkerCenter (
    modifier: Modifier = Modifier,
    center: Center = dummyCenterData,
    onClick: (Marker, Center) -> Unit
) {
    Marker(
        state = MarkerState(position = CenterAPI.getCenterLatLng(center)),
        iconTintColor = CenterAPI.getCenterTypeColour(center),
        captionText = center.centerType,
        captionColor = Color.Green,
        onClick = {
            onClick(it, center)
            true
        }
    )
}