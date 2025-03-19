package com.example.of1.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.of1.data.local.dao.SessionDao
import com.example.of1.data.local.entity.SessionEntity

@Database(entities = [SessionEntity::class], version = 1, exportSchema = false)
abstract class Of1Database : RoomDatabase() {

    abstract fun sessionDao(): SessionDao

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
                    .fallbackToDestructiveMigration() // Handle migrations (in a real app, you'd implement proper migrations)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}