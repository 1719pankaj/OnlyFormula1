package com.example.of1.di

import android.content.Context
import com.example.of1.data.local.Of1Database
import com.example.of1.data.local.dao.SessionDao
import com.example.of1.data.remote.OpenF1ApiService
import com.example.of1.data.repository.SessionRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = "https://api.openf1.org/v1/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Log everything: headers, body, etc.
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) // Add the interceptor
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit { // Inject OkHttpClient
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Set the OkHttpClient
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenF1ApiService(retrofit: Retrofit): OpenF1ApiService {
        return retrofit.create(OpenF1ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): Of1Database {
        return Of1Database.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideSessionDao(database: Of1Database): SessionDao {
        return database.sessionDao()
    }

    @Provides
    @Singleton
    fun provideSessionRepository(apiService: OpenF1ApiService, sessionDao: SessionDao): SessionRepository {
        return SessionRepository(apiService, sessionDao)
    }
}