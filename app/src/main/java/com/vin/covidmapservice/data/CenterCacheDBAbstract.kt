package com.vin.covidmapservice.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.room.*
import com.naver.maps.geometry.LatLng
import com.vin.covidmapservice.domain.Center
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

const val DB_TABLE_NAME = "tbl_center"

/*
    Room DB for API request caching
 */
// Room Database
@Database(entities = [Center::class], version = 1, exportSchema = false)
@TypeConverters(CenterDataConverters::class)
abstract class CenterCacheDBAbstract : RoomDatabase() {
    abstract fun centerCacheDAO(): CenterCacheDAO
}

// For this project we'll be storing the LatLng objects are following string-encoded format:
// "lat;lng" -- Therefore we must define a converter to convert between the string and the LatLng
class CenterDataConverters {
    @TypeConverter
    fun fromStringPos (value: String): LatLng {
        val splitLatLng = value.split(";")
        return LatLng(splitLatLng[0].toDouble(), splitLatLng[1].toDouble())
    }
    @TypeConverter
    fun latLngToStringPos (value: LatLng): String {
        return (value.latitude.toString() + ";" + value.longitude.toString())
    }
}
