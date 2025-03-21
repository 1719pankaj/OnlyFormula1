package com.example.of1.data.model.jolpica

import com.google.gson.annotations.SerializedName

data class ResultResponse(
    @SerializedName("MRData") val mrData: ResultMRData
)

data class ResultMRData(
    val xmlns: String,
    val series: String,
    val url: String,
    val limit: String,
    val offset: String,
    val total: String,
    @SerializedName("RaceTable") val raceTable: ResultRaceTable
)
data class ResultRaceTable(
    val season: String,
    val round: String,
    @SerializedName("Races") val races: List<JolpicaResultRace>
)

data class JolpicaResultRace(
    val season: String,
    val round: String,
    val url: String,
    val raceName: String,
    @SerializedName("Circuit") val circuit: JolpicaCircuit, // Reuse from RaceResponse
    val date: String,
    val time: String?,
    @SerializedName("Results") val results: List<JolpicaRaceResult>
)

data class JolpicaRaceResult(
    val number: String,
    val position: String,
    val positionText: String,
    val points: String,
    @SerializedName("Driver") val driver: JolpicaDriver,
    @SerializedName("Constructor") val constructor: JolpicaConstructor,
    val grid: String?,
    val laps: String?,
    val status: String?,
    @SerializedName("Time") val time: JolpicaResultTime?, //Different structure.
    @SerializedName("FastestLap") val fastestLap: JolpicaFastestLap?
)
data class JolpicaDriver(
    val driverId: String,
    val permanentNumber: String?,
    val code: String?,
    val url: String,
    val givenName: String,
    val familyName: String,
    val dateOfBirth: String,
    val nationality: String
)
data class JolpicaConstructor(
    val constructorId: String,
    val url: String,
    val name: String,
    val nationality: String
)

data class JolpicaResultTime(
    val millis: String?,
    val time: String
)

data class JolpicaFastestLap(
    val rank: String?,
    val lap: String?,
    @SerializedName("Time") val time: JolpicaFastestLapTime?
)
data class JolpicaFastestLapTime(
    val time: String?
)