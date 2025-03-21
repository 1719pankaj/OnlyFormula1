package com.example.of1.data.model.jolpica

import com.google.gson.annotations.SerializedName

// Outer wrapper
data class RaceResponse(
    @SerializedName("MRData") val mrData: RaceMRData
)

data class RaceMRData(
    val xmlns: String,
    val series: String,
    val url: String,
    val limit: String,
    val offset: String,
    val total: String,
    @SerializedName("RaceTable") val raceTable: RaceTable
)

data class RaceTable(
    val season: String,
    @SerializedName("Races") val races: List<JolpicaRace>
)
data class JolpicaRace(
    val season: String,
    val round: String,
    val url: String,
    val raceName: String,
    @SerializedName("Circuit") val circuit: JolpicaCircuit,
    val date: String,
    val time: String?, // Nullable
    @SerializedName("FirstPractice") val firstPractice: JolpicaPractice?, // Nullable
    @SerializedName("SecondPractice") val secondPractice: JolpicaPractice?, // Nullable
    @SerializedName("ThirdPractice") val thirdPractice: JolpicaPractice?, // Nullable
    @SerializedName("Qualifying") val qualifying: JolpicaPractice?,// Nullable
    @SerializedName("Sprint") val sprint: JolpicaPractice?, // Nullable
)

data class JolpicaCircuit(
    val circuitId: String,
    val url: String,
    val circuitName: String,
    @SerializedName("Location") val location: JolpicaLocation
)

data class JolpicaLocation(
    val lat: String,
    val long: String,
    val locality: String,
    val country: String
)
data class JolpicaPractice(
    val date: String,
    val time: String?
)