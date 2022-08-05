package com.vin.covidmapservice.domain

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.naver.maps.geometry.LatLng
import com.vin.covidmapservice.data.api.CenterAPIModel
import com.vin.covidmapservice.data.DB_TABLE_NAME

// Center entity representing a row within the DB
@Entity(tableName = DB_TABLE_NAME)
data class Center(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val pos: LatLng,
    val address: String,
    val centerName: String,
    val facilityName: String,
    val phoneNumber: String,
    val updatedAt: String,
    val centerType: String,
    @Ignore
    val markerColor: Color, // Set inside of the constructor. No need to be stored within the DB
) {
    constructor(id: Int, pos: LatLng, address: String, centerName: String, facilityName: String, phoneNumber: String, updatedAt: String, centerType: String): this(id, pos, address, centerName, facilityName, phoneNumber, updatedAt, centerType, centerTypeToColor(centerType))
}

// Converts Center type to (tint) color
fun centerTypeToColor (centerType: String): Color {
    return when (centerType) {
        "중앙/권역" -> Color.Transparent
        "지역" -> Color.Blue
        else -> Color.Magenta
    }
}

// Converts CenterAPIModel to Center class
fun centerModelToCenter (response: CenterAPIModel): Center {
    return Center(
        id = response.id,
        pos = LatLng(response.lat.toDouble(), response.lng.toDouble()),
        address = response.address,
        centerName = response.centerName,
        facilityName = response.facilityName,
        phoneNumber = response.phoneNumber,
        updatedAt = response.updatedAt,
        centerType = response.centerType
    )
}