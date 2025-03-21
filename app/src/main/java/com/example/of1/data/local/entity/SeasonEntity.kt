package com.example.of1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "seasons")
data class SeasonEntity(
    @PrimaryKey
    val year: String,
    val wikipediaUrl: String
)