package com.vin.covidmapservice.data.api

import com.vin.covidmapservice.BuildConfig
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Vaccination center API request properties
const val API_PAGE_PERPAGE = 10

/*
    Vaccination center API: Retrofit2 -> Client
 */
// Linked by Retrofit2 builder below:
interface CenterAPIInterface {
    @GET("v1/centers") // final request is something like this: https://api.odcloud.kr/api/15077586/v1/centers?page=<PAGE>&perPage=<PERPAGE>&serviceKey=<API_KEY>
    fun getCenters(
        @Query("serviceKey") key: String = BuildConfig.CENTERS_API_KEY,
        @Query("page") page: Int = 0,
        @Query("perPage") perPage: Int = API_PAGE_PERPAGE
    ): Call<CenterAPIResponse>
}

// Center API handler to access APIs.
// use CenterAPI.inst to call the API
object CenterAPI {
    private val BASE_URL = "https://api.odcloud.kr/api/15077586/"
    val inst: CenterAPIInterface = Retrofit.Builder()
                                        .baseUrl(BASE_URL)
                                        .addConverterFactory(GsonConverterFactory.create())
                                        .build()
                                        .create(CenterAPIInterface::class.java)
    // Calls the vaccination center API, invokes corresponding callbacks
    suspend fun callAPI (
        pages: Int = 10,
        emulateSlowAPICall: Boolean = false,
        onAPIPageDone: (Int, List<CenterAPIModel>) -> Unit, // (page idx, list)
        onAPIFailed: (Int, Int) -> Unit, // (page idx, response code (-1 if no response))
    ) {
        for (i in 1..pages) {
            inst.getCenters(page = i).enqueue(object: Callback<CenterAPIResponse> {
                override fun onResponse(
                    call: Call<CenterAPIResponse>,
                    response: Response<CenterAPIResponse>
                ) {
                    if (response.isSuccessful && response.code() == 200) {
                        onAPIPageDone(i, response.body()!!.data)
                    }
                    else {
                        onAPIFailed(i, response.code())
                    }
                }
                override fun onFailure(call: Call<CenterAPIResponse>, t: Throwable) {
                    onAPIFailed(i, -1)
                }
            })

            // Emulate slow network / API response by... simply waiting longer. (wow!)
            if (emulateSlowAPICall)
                delay(500)
        }
    }
}