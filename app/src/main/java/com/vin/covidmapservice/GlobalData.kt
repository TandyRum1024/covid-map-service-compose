package com.vin.covidmapservice

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.naver.maps.geometry.LatLng
import com.vin.covidmapservice.domain.Center

// Global state for preview flag as Naver map view does not like the Preview mode
val isInPreview = compositionLocalOf { false }

// Dummy Center info for previewing purposes. If you see this information in the final product, then it means something probably went wrong
val dummyCenterData = Center(
    address = "서울특별시 테스트구 테스트로 42길 4242",
    centerName = "테스트",
    facilityName = "테스트의료원 테스트동",
    phoneNumber = "010-XXXX-XXXX",
    updatedAt = "4242-42-42 42:42:42",
    id = -1,
    pos = LatLng(42.0, 128.0),
    centerType = "중앙/권역",
    markerColor = Color.Transparent
)