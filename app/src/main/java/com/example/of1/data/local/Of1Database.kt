package com.example.of1.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
import com.example.of1.data.local.entity.CarDataEntity
import com.example.of1.data.local.entity.DriverEntity
import com.example.of1.data.local.entity.LapEntity
import com.example.of1.data.local.entity.MeetingEntity
import com.example.of1.data.local.entity.PitStopEntity
import com.example.of1.data.local.entity.PositionEntity
import com.example.of1.data.local.entity.RaceEntity
import com.example.of1.data.local.entity.ResultEntity
import com.example.of1.data.local.entity.SeasonEntity
import com.example.of1.data.local.entity.SessionEntity
import com.example.of1.data.local.dao.TeamRadioDao
import com.example.of1.data.local.entity.IntervalEntity
import com.example.of1.data.local.entity.RaceControlEntity
import com.example.of1.data.local.entity.TeamRadioEntity

@Database(entities = [
    SessionEntity::class,
    MeetingEntity::class,
    SeasonEntity::class,
    RaceEntity::class,
    ResultEntity::class,
    DriverEntity::class,
    PositionEntity::class,
    LapEntity::class,
    CarDataEntity::class,
    PitStopEntity::class,
    TeamRadioEntity::class,
    IntervalEntity::class,
    RaceControlEntity::class], version = 7, exportSchema = false) // Add entities, update version

abstract class Of1Database : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun meetingDao(): MeetingDao // Keep this for now, even though it's unused
    abstract fun seasonDao(): SeasonDao
    abstract fun raceDao(): RaceDao
    abstract fun resultDao(): ResultDao // Add resultDao
    abstract fun driverDao(): DriverDao
    abstract fun positionDao(): PositionDao
    abstract fun lapDao(): LapDao
    abstract fun carDataDao(): CarDataDao
    abstract fun pitStopDao(): PitStopDao
    abstract fun teamRadioDao(): TeamRadioDao
    abstract fun intervalDao(): IntervalDao
    abstract fun raceControlDao(): RaceControlDao


    companion object {
        @Volatile
        private var INSTANCE: Of1Database? = null

        fun getDatabase(context: Context): Of1Database {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Of1Database::class.java,
                    "of1_database"
                )
                    .fallbackToDestructiveMigration() // Use migrations in production!
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}