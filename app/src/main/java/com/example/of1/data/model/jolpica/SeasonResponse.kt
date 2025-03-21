package com.example.of1.data.model.jolpica

import com.google.gson.annotations.SerializedName

// Outer wrapper
data class SeasonResponse(
    @SerializedName("MRData") val mrData: SeasonMRData
)

data class SeasonMRData(
    val xmlns: String,
    val series: String,
    val url: String,
    val limit: String,
    val offset: String,
    val total: String,
    @SerializedName("SeasonTable") val seasonTable: SeasonTable
)

data class SeasonTable(
    @SerializedName("Seasons") val seasons: List<JolpicaSeason>
)

// The actual season data from Jolpica
data class JolpicaSeason(
    val season: String,
    val url: String
)