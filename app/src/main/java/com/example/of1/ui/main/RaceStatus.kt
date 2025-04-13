package com.example.of1.ui.main // Or a more common package

enum class RaceStatus {
    PAST,
    LIVE,
    UPCOMING, // Within a defined window (e.g., next 7 days)
    FUTURE    // Beyond the upcoming window
}