package com.scape.pixscape.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.scape.pixscape.models.dto.GpsTrace
import com.scape.pixscape.converters.DataConverter
import com.scape.pixscape.dao.TraceDao
import com.scape.pixscape.models.dto.ScapeTrace

@Database(entities = [GpsTrace::class, ScapeTrace::class], version = 1, exportSchema = false)
@TypeConverters(DataConverter::class)
abstract class TraceRoomDatabase : RoomDatabase() {

    abstract fun traceDataDao(): TraceDao

    companion object {
        @Volatile
        private var INSTANCE: TraceRoomDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = INSTANCE ?: synchronized(LOCK) {
            INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, TraceRoomDatabase::class.java, "trace.db")
                .build()

    }
}