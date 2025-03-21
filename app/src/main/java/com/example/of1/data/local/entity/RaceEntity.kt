package com.example.of1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "races")
data class RaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null, // Auto-generated ID, good practice for Room
    val season: String,
    val round: String,
    val url: String,
    val raceName: String,
    val circuitId: String,
    val circuitUrl: String,
    val circuitName: String,
    val circuitLat: String,
    val circuitLong: String,
    val circuitLocality: String,
    val circuitCountry: String,
    val date: String,
    val time: String?, // Nullable
    val firstPracticeDate: String?, // Nullable
    val firstPracticeTime: String?, // Nullable
    val secondPracticeDate: String?, // Nullable
    val secondPracticeTime: String?, // Nullable
    val thirdPracticeDate: String?, // Nullable
    val thirdPracticeTime: String?, // Nullable
    val qualifyingDate: String?, // Nullable
    val qualifyingTime: String?, // Nullable
    val sprintDate: String?, // Nullable
    val sprintTime: String?  // Nullable
)