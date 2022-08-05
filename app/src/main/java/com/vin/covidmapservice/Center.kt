package com.vin.covidmapservice

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.room.*
import com.naver.maps.geometry.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

const val DB_TABLE_NAME = "tbl_center"

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

/*
    Room DB for API request caching
 */
/*
    From the assignment's specs, only the following data from the API result are used:
        (id)
        (pos: latlng)
        address
        centerName
        facilityName
        phoneNumber
        updatedAt
    Additional data:
        Type (used for marker colour and caption)
 */
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

// Room DAO
@Dao
interface CenterCacheDAO {
    @Query("SELECT * FROM $DB_TABLE_NAME")
    fun getAll(): Flow<List<Center>>
    @Query("DELETE FROM $DB_TABLE_NAME")
    fun deleteAll()
    @Insert
    fun insert(vararg centers: Center)
    @Delete
    fun delete(center: Center)
    @androidx.room.Query("SELECT COUNT(id) FROM $DB_TABLE_NAME")
    fun getCount(): Int
}

// Room Database
@Database(entities = [Center::class], version = 1, exportSchema = false)
@TypeConverters(CenterDataConverters::class)
abstract class CenterCacheDBAbstract : RoomDatabase() {
    abstract fun centerCacheDAO(): CenterCacheDAO
}

// Helper / exposed API for Room DB
class CenterCacheDB {
    private var dao: CenterCacheDAO? = null
    constructor(context: Context?) { // Pass null as context to create the 'dummy' DB which does nothing thanks to the ?. operator (they're nice for testing stuffs)
        if (context != null) {
            val db = Room.databaseBuilder(context, CenterCacheDBAbstract::class.java, "CenterCacheDB").build()
            dao = db.centerCacheDAO()
        }
    }

    // getAll() returns the live(flow) list of center, so whenever the DB changes (i.e. getting cached),
    // the composables that accesses the list of center (collected within the viewmodel) will be automatically notified & re-composed
    fun getAll(): Flow<List<Center>> = dao?.getAll() ?: flow { emit(listOf<Center>()) }
    fun deleteAll() = dao?.deleteAll()
    fun insert(vararg centers: Center) = dao?.insert(*centers) // (*centers => center[0], center[1], center[2]...)
    fun delete(center: Center) = dao?.delete(center) // (not really used but added per traditional CRUD operation & in case of later use)
    fun getCount(): Int = dao?.getCount() ?: 0
}