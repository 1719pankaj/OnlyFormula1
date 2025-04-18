package com.example.of1.di

import android.content.Context
import com.example.of1.data.local.Of1Database
import com.example.of1.data.local.dao.CarDataDao
import com.example.of1.data.local.dao.DriverDao
import com.example.of1.data.local.dao.IntervalDao
import com.example.of1.data.local.dao.LapDao
import com.example.of1.data.local.dao.MeetingDao
import com.example.of1.data.local.dao.PitStopDao
import com.example.of1.data.local.dao.PositionDao
import com.example.of1.data.local.dao.RaceControlDao
import com.example.of1.data.local.dao.RaceDao
import com.example.of1.data.local.dao.ResultDao
import com.example.of1.data.local.dao.SeasonDao
import com.example.of1.data.local.dao.SessionDao
import com.example.of1.data.local.dao.TeamRadioDao
import com.example.of1.data.remote.JolpicaApiService
import com.example.of1.data.remote.OpenF1ApiService
import com.example.of1.data.repository.CarDataRepository
import com.example.of1.data.repository.DriverRepository
import com.example.of1.data.repository.IntervalRepository
import com.example.of1.data.repository.LapRepository
import com.example.of1.data.repository.MeetingRepository
import com.example.of1.data.repository.PitStopRepository
import com.example.of1.data.repository.PositionRepository
import com.example.of1.data.repository.RaceControlRepository
import com.example.of1.data.repository.RaceRepository
import com.example.of1.data.repository.ResultRepository
import com.example.of1.data.repository.SessionRepository
import com.example.of1.data.repository.TeamRadioRepository
import com.example.of1.utils.LimitedBodyLoggingInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL_OPENF1 = "https://api.openf1.org/v1/"
    private  val BASE_URL_JOLPICA = "https://api.jolpi.ca/ergast/f1/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
//        val loggingInterceptor = HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BODY // Log everything: headers, body, etc.
//        }
        return OkHttpClient.Builder()
            .addInterceptor(LimitedBodyLoggingInterceptor()) // Add the interceptor
            .build()
    }

    @Provides
    @Singleton
    @OpenF1Retrofit // Custom qualifier
    fun provideRetrofitOpenF1(okHttpClient: OkHttpClient): Retrofit { // Inject OkHttpClient
        return Retrofit.Builder()
            .baseUrl(BASE_URL_OPENF1)
            .client(okHttpClient) // Set the OkHttpClient
            .addConverterFactory(GsonConverterFactory.create()) // Use GsonConverterFactory
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenF1ApiService(@OpenF1Retrofit retrofit: Retrofit): OpenF1ApiService {
        return retrofit.create(OpenF1ApiService::class.java)
    }
    @Provides
    @Singleton
    @JolpicaRetrofit // Custom qualifier
    fun provideRetrofitJolpica(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL_JOLPICA)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideJolpicaApiService(@JolpicaRetrofit retrofit: Retrofit): JolpicaApiService {
        return retrofit.create(JolpicaApiService::class.java)
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

    @Provides
    @Singleton
    fun provideMeetingDao(database: Of1Database): MeetingDao { // Add provideMeetingDao
        return database.meetingDao()
    }

    @Provides
    @Singleton
    fun provideMeetingRepository(apiService: OpenF1ApiService, meetingDao: MeetingDao): MeetingRepository { // Add provideMeetingRepository
        return MeetingRepository(apiService, meetingDao)
    }

    @Provides
    @Singleton
    fun provideRaceDao(database: Of1Database): RaceDao{
        return database.raceDao()
    }

    @Provides
    @Singleton
    fun provideRaceRepository(apiService: JolpicaApiService, raceDao: RaceDao) : RaceRepository{
        return RaceRepository(apiService, raceDao)
    }

    @Provides
    @Singleton
    fun provideResultDao(database: Of1Database): ResultDao {
        return database.resultDao()
    }

    @Provides
    @Singleton
    fun provideResultRepository(apiService: JolpicaApiService, resultDao: ResultDao): ResultRepository {
        return ResultRepository(apiService, resultDao)
    }

    @Provides
    @Singleton
    fun providePositionDao(database: Of1Database): PositionDao {
        return database.positionDao()
    }

    @Provides
    @Singleton
    fun providePositionRepository(apiService: OpenF1ApiService, positionDao: PositionDao): PositionRepository {
        return PositionRepository(apiService, positionDao)
    }

    @Provides
    @Singleton
    fun provideDriverDao(database: Of1Database): DriverDao {
        return database.driverDao()
    }

    @Provides
    @Singleton
    fun provideDriverRepository(apiService: OpenF1ApiService, driverDao: DriverDao): DriverRepository {
        return DriverRepository(apiService, driverDao)
    }

    @Provides
    @Singleton
    fun provideLapDao(database: Of1Database): LapDao {
        return database.lapDao()
    }

    @Provides
    @Singleton
    fun provideLapRepository(apiService: OpenF1ApiService, lapDao: LapDao): LapRepository {
        return LapRepository(apiService, lapDao)
    }

    @Provides
    @Singleton
    fun provideCarDataDao(database: Of1Database): CarDataDao {
        return database.carDataDao()
    }

    @Provides
    @Singleton
    fun provideCarDataRepository(apiService: OpenF1ApiService, carDataDao: CarDataDao): CarDataRepository {
        return CarDataRepository(apiService, carDataDao)
    }

    @Provides
    @Singleton
    fun providePitStopDao(database: Of1Database): PitStopDao {
        return database.pitStopDao()
    }

    @Provides
    @Singleton
    fun providePitStopRepository(apiService: OpenF1ApiService, pitStopDao: PitStopDao): PitStopRepository {
        return PitStopRepository(apiService, pitStopDao)
    }

    @Provides
    @Singleton
    fun provideTeamRadioDao(database: Of1Database): TeamRadioDao {
        return database.teamRadioDao()
    }

    @Provides
    @Singleton
    fun provideTeamRadioRepository(apiService: OpenF1ApiService, teamRadioDao: TeamRadioDao): TeamRadioRepository {
        return TeamRadioRepository(apiService, teamRadioDao)
    }

    @Provides
    @Singleton
    fun provideIntervalDao(database: Of1Database): IntervalDao {
        return database.intervalDao()
    }

    @Provides
    @Singleton
    fun provideIntervalRepository(apiService: OpenF1ApiService, intervalDao: IntervalDao): IntervalRepository {
        return IntervalRepository(apiService, intervalDao) // Add dao
    }

    @Provides
    @Singleton
    fun provideRaceControlDao(database: Of1Database): RaceControlDao {
        return database.raceControlDao()
    }

    @Provides
    @Singleton
    fun provideRaceControlRepository(apiService: OpenF1ApiService, raceControlDao: RaceControlDao): RaceControlRepository {
        return RaceControlRepository(apiService, raceControlDao) // Add dao
    }
}

// Custom qualifiers to distinguish between Retrofit instances
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpenF1Retrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class JolpicaRetrofit

