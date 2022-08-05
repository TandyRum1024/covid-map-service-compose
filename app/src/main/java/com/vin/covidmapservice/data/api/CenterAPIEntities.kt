package com.vin.covidmapservice.data.api

// Vaccination center model, from API.
// See: https://www.data.go.kr/tcs/dss/selectApiDataDetailView.do?publicDataPk=15077586#/
// This is later converted into Center data class (see: CenterCacheDBAbstract.kt), which is the real data class we display and use within the other parts of the app
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