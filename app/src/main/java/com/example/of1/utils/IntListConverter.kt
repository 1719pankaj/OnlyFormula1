package com.example.of1.utils

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class IntListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromIntList(intList: List<Int>?): String? {
        return intList?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toIntList(intListString: String?): List<Int>? {
        return intListString?.let {
            val listType = object : TypeToken<List<Int>>() {}.type
            gson.fromJson(it, listType)
        }
    }
}