package com.example.of1.utils

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class LimitedBodyLoggingInterceptor : Interceptor {
    private val maxBodyLogSize = 1000 // Characters to log
    private val logger = HttpLoggingInterceptor.Logger { message ->
        android.util.Log.d("OkHttp", message)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Log request
        logger.log("--> ${request.method} ${request.url}")

        val response = chain.proceed(request)

        // Log response code and headers
        logger.log("<-- ${response.code} ${response.message} for ${request.url}")

        // Log limited response body
        response.body?.let { body ->
            val contentType = body.contentType()
            val charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

            val source = body.source()
            source.request(Long.MAX_VALUE)
            val buffer = source.buffer.clone()

            val bodyString = buffer.readString(charset)
            val truncatedBody = if (bodyString.length > maxBodyLogSize) {
                bodyString.take(maxBodyLogSize) + "... [${bodyString.length} total characters]"
            } else {
                bodyString
            }

            logger.log("Response body: $truncatedBody")
        }

        return response
    }
}