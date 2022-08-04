package com.vin.covidmapservice

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.*
import com.naver.maps.geometry.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query

// Vaccination center API request properties
const val API_PAGE_PERPAGE = 10
const val DB_TABLE_NAME = "tbl_center"

/*
    Room DB
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
//    constructor(): this(0, LatLng(42.0, 42.0), "", "", "", "", "", "???", Color.Magenta)
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
    @androidx.room.Query("SELECT * FROM $DB_TABLE_NAME")
    fun getAll(): Flow<List<Center>>
    @androidx.room.Query("DELETE FROM $DB_TABLE_NAME")
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
    fun insert(vararg centers: Center) = dao?.insert(*centers) // *centers = center[0], center[1], center[2]...
    fun delete(center: Center) = dao?.delete(center) // not really used but added per traditional CRUD operation & in case of later use
    fun getCount(): Int = dao?.getCount() ?: 0
}

/*
    Vaccination center API: Retrofit2 -> Client
 */
// Vaccination center model, from API.
// See: https://www.data.go.kr/tcs/dss/selectApiDataDetailView.do?publicDataPk=15077586#/
// This is later converted into Center data class above, which is the real data class we display and use within the other parts of the app
data class CenterAPIModel(
    val id: Int,
    val centerName: String,
    val sido: String,
    val sigungu: String,
    val facilityName: String,
    val zipCode: String,
    val address: String,
    val lat: String,
    val lng: String,
    val createdAt: String,
    val updatedAt: String,
    val centerType: String,
    val org: String,
    val phoneNumber: String,
)

// Vaccination center API call response holder
data class CenterAPIResponse (
    val page: Int,
    val perPage: Int,
    val totalCount: Int,
    val currentCount: Int,
    val matchCount: Int,

    val data: List<CenterAPIModel> // size = currentCount
)

// Linked by Retrofit2 builder below:
interface CenterAPIInterface {
    @GET("v1/centers") // final request is something like this: https://api.odcloud.kr/api/15077586/v1/centers?page=<PAGE>&perPage=<PERPAGE>&serviceKey=<API_KEY>
    fun getCenters(
        @Query("serviceKey") key: String = BuildConfig.CENTERS_API_KEY,
        @Query("page") page: Int = 0,
        @Query("perPage") perPage: Int = API_PAGE_PERPAGE
    ): Call<CenterAPIResponse>
}

// Center API handler to access APIs. use CenterAPI.inst to call the API
object CenterAPI {
    private val BASE_URL = "https://api.odcloud.kr/api/15077586/"
    val inst: CenterAPIInterface = Retrofit.Builder()
                                        .baseUrl(BASE_URL)
                                        .addConverterFactory(GsonConverterFactory.create())
                                        .build()
                                        .create(CenterAPIInterface::class.java)
}