package com.example.of1.data.model

data class Result(
    val driverNumber: String,
    val position: String,
    val positionText: String,
    val points: String,
    val driver: Driver,
    val constructor: Constructor,
    val grid: String?,
    val laps: String?,
    val status: String?,
    val time: ResultTime?,
    val fastestLap: FastestLap?
)

data class Driver(
    val driverId: String,
    val permanentNumber: String?,
    val code: String?,
    val url: String,
    val givenName: String,
    val familyName: String,
    val dateOfBirth: String,
    val nationality: String
)

data class Constructor(
    val constructorId: String,
    val url: String,
    val name: String,
    val nationality: String
)
data class ResultTime(
    val millis: String?,
    val time: String
)
data class FastestLap(
    val rank: String?,
    val lap: String?,
    val time: FastestLapTime?
)
data class FastestLapTime(
    val time: String?
)