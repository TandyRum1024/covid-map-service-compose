@file:OptIn(ExperimentalNaverMapApi::class)

package com.vin.covidmapservice

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.*
import com.naver.maps.map.overlay.Marker
import kotlinx.coroutines.launch

@OptIn(ExperimentalNaverMapApi::class, ExperimentalMaterialApi::class)
@Composable
fun MapScreen(
    viewmodel: MainViewModel = hiltViewModel(),
    onNavigateToDebugMenu: () -> Unit,
    onLocationPermissionRequest: () -> Unit,
) {
    // If in preview, don't call the naver map composable so that it won't break the Preview mode
    val cameraPositionState: CameraPositionState? = if (isInPreview.current) null else rememberCameraPositionState {
        position = CameraPosition(viewmodel.mapCurrentLocation, 11.0)
    }
    // For bottom sheet scaffold
    val scaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()

    // Collapse bottom sheet on composition
    LaunchedEffect(Unit) {
        Log.e("MainActivity", "COLLAPSING!")
        scope.launch {
            // https://developer.android.com/jetpack/compose/layouts/material#scaffold
            scaffoldState.bottomSheetState.apply {
                collapse()
            }
        }

        // Begin updating the fetched DB cache if we hadn't been updating yet
        viewmodel.beginCollectingCachedCenter()
    }

    Column {
        Button(onClick = onNavigateToDebugMenu) {
            Text("DEBUG: Menu")
        }
        // Scaffold for easier positioning of the 'move to your position' FAB
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        if (viewmodel.isPermissionGranted)
                            cameraPositionState?.move(
                                CameraUpdate.toCameraPosition(CameraPosition(viewmodel.mapCurrentLocation, 15.0)).animate(
                                    CameraAnimation.Easing, 1500))
                        else
                        {
                            scope.launch {
                                scaffoldState.snackbarHostState.showSnackbar("In order to use this feature, you must permit the permissions!")
                            }
                            onLocationPermissionRequest()
                        }
                    }
                ) {
                    Icon(Icons.Filled.Place, null)
                }
            },
            sheetPeekHeight = 0.dp,
            sheetContent = {
                CenterDisplay(center = viewmodel.currentCenter)
            },
            content = {
                Map(
                    viewmodel = viewmodel,
                    modifier = Modifier
                        .weight(1f)
                        .padding(it),
                    cameraPositionState = cameraPositionState,
                    centers = if (isInPreview.current) listOf<Center>() else viewmodel.cachedCenters,
                    onCenterMarkerClick = if (isInPreview.current) { marker, center ->  } else { marker, center ->
                        // Toggle center info if selected marker has been clicked again
                        viewmodel.isMarkerSelected = if (center.id == viewmodel.currentCenter.id) !viewmodel.isMarkerSelected else true
                        // Set currently displaying center data to markers center data
                        viewmodel.currentCenter = center

                        // Toggle the center info screen
                        scope.launch {
                            // https://developer.android.com/jetpack/compose/layouts/material#scaffold
                            scaffoldState.bottomSheetState.apply {
                                if (viewmodel.isMarkerSelected) expand() else collapse()
                            }
                        }
                        cameraPositionState?.move(
                            CameraUpdate.toCameraPosition(CameraPosition(marker.position, 15.0)).animate(
                                CameraAnimation.Easing, 1500))
                    },
                    onCurrentPosMarkerClick = if (isInPreview.current) { marker ->  } else { marker ->
                        cameraPositionState?.move(
                            CameraUpdate.toCameraPosition(CameraPosition(marker.position, 15.0)).animate(
                                CameraAnimation.Easing, 1500))
                    }
                )
            })
    }
}

// Displays the map using Naver map API
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
        // Naver map composable wrapper/helper from: https://github.com/fornewid/naver-map-compose
        NaverMap(
            modifier = modifier.fillMaxWidth(),
            cameraPositionState = cameraPositionState!!
        ) {
            if (viewmodel.isPermissionGranted) {
                MarkerCurrentPos(
                    currentLocation = viewmodel.mapCurrentLocation,
                    onClick = onCurrentPosMarkerClick
                )
            }
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
        state = MarkerState(position = center.pos),
        iconTintColor = center.markerColor,
        captionText = center.centerType,
        captionColor = Color.Green,
        onClick = {
            onClick(it, center)
            true
        }
    )
}