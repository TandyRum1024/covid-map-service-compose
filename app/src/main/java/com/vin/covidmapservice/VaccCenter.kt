package com.vin.covidmapservice

import androidx.compose.ui.graphics.Color
import com.naver.maps.geometry.LatLng
import kotlinx.coroutines.flow.Flow
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query

// API request properties
val API_PAGE_PERPAGE = 10

/*
    Vaccination center API: Retrofit2
 */
// Vaccination center model
// See: https://www.data.go.kr/tcs/dss/selectApiDataDetailView.do?publicDataPk=15077586#/
data class Center(
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
data class CenterResponse (
    val page: Int,
    val perPage: Int,
    val totalCount: Int,
    val currentCount: Int,
    val matchCount: Int,

    val data: List<Center> // size = currentCount
)

interface CenterAPIInterface {
    @GET("v1/centers")
    fun getCenters(
        @Query("serviceKey") key: String = BuildConfig.CENTERS_API_KEY,
        @Query("page") page: Int = 0,
        @Query("perPage") perPage: Int = API_PAGE_PERPAGE
    ): Call<CenterResponse>
}

object CenterAPI {
    private val BASE_URL = "https://api.odcloud.kr/api/15077586/"
    val inst: CenterAPIInterface = Retrofit.Builder()
                                        .baseUrl(BASE_URL)
                                        .addConverterFactory(GsonConverterFactory.create())
                                        .build()
                                        .create(CenterAPIInterface::class.java)

    fun getCenterTypeColour(center: Center): Color {
        when (center.centerType) {
            "중앙/권역" -> return Color.Transparent
            "지역" -> return Color.Blue
            else -> return Color.Magenta
        }
    }
    fun getCenterLatLng(center: Center): LatLng {
        return LatLng(center.lat.toDouble(), center.lng.toDouble())
    }
}