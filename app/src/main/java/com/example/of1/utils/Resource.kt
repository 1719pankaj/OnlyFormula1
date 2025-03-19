package com.example.of1.utils

sealed class Resource<out T>(val data: T? = null, val message: String? = null, val isLoading: Boolean = false) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T>(isLoading: Boolean = true) : Resource<T>(isLoading = isLoading) //Added is loading parameter

}