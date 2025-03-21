package com.example.of1.data.model

data class Race(
    val season: String,
    val round: String,
    val url: String,
    val raceName: String,
    val circuit: Circuit,
    val date: String,
    val time: String?,
    val firstPractice: Practice?,
    val secondPractice: Practice?,
    val thirdPractice: Practice?,
    val qualifying: Practice?,
    val sprint: Practice?
)
data class Circuit(
    val circuitId: String,
    val url: String,
    val circuitName: String,
    val location: Location
)
data class Location(
    val lat: String,
    val long: String,
    val locality: String,
    val country: String
)
data class Practice(
    val date: String,
    val time: String?
)