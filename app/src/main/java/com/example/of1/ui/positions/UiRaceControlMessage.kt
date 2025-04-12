package com.example.of1.ui.positions

// UI Model
data class UiRaceControlMessage(
    val id: String, // Use API 'date' as unique ID
    val message: String,
    val category: String?,
    val flag: String?,
    val scope: String?,
    val isPersistent: Boolean,
    val displayUntilMillis: Long?, // Null means persistent until cleared
    val creationTimeMillis: Long // When this object was added to the list
)