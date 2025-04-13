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
// REMOVED: import com.example.of1.data.local.dao.SeasonDao // Remove import
import com.example.of1.data.local.dao.SessionDao
import com.example.of1.data.local.entity.CarDataEntity
import com.example.of1.data.local.entity.DriverEntity
import com.example.of1.data.local.entity.LapEntity
import com.example.of1.data.local.entity.MeetingEntity
import com.example.of1.data.local.entity.PitStopEntity
import com.example.of1.data.local.entity.PositionEntity
import com.example.of1.data.local.entity.RaceEntity
import com.example.of1.data.local.entity.ResultEntity
// REMOVED: import com.example.of1.data.local.entity.SeasonEntity // Remove import
import com.example.of1.data.local.entity.SessionEntity
import com.example.of1.data.local.dao.TeamRadioDao
import com.example.of1.data.local.entity.IntervalEntity
import com.example.of1.data.local.entity.RaceControlEntity
import com.example.of1.data.local.entity.TeamRadioEntity

@Database(entities = [
    SessionEntity::class,
    MeetingEntity::class,
    // REMOVED: SeasonEntity::class, // Remove SeasonEntity
    RaceEntity::class,
    ResultEntity::class,
    DriverEntity::class,
    PositionEntity::class,
    LapEntity::class,
    CarDataEntity::class,
    PitStopEntity::class,
    TeamRadioEntity::class,
    IntervalEntity::class,
    RaceControlEntity::class], version = 8, exportSchema = false) // Increment version number
abstract class Of1Database : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun meetingDao(): MeetingDao
    // REMOVED: abstract fun seasonDao(): SeasonDao // Remove seasonDao function
    abstract fun raceDao(): RaceDao
    abstract fun resultDao(): ResultDao
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