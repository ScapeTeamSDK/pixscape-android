package com.scape.pixscape.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.scape.pixscape.models.dto.GpsTrace
import com.scape.pixscape.models.dto.ScapeTrace

@Dao
interface TraceDao {
    @Insert
    fun insertGpsTrace(gpsTraceData: GpsTrace)

    @Query("SELECT * FROM gps_trace_data ORDER BY date DESC")
    fun getAllGpsTraces(): LiveData<List<GpsTrace>>

    @Query("SELECT * FROM gps_trace_data WHERE date > :startDate")
    fun getLastGpsTraces(startDate: Long): List<GpsTrace>

    @Delete
    fun deleteGpsTrace(gpsTrace: GpsTrace)

    @Insert
    fun insertScapeTrace(scapeTraceData: ScapeTrace)

    @Query("SELECT * FROM scape_trace_data ORDER BY date DESC")
    fun getAllScapeTraces(): LiveData<List<ScapeTrace>>

    @Query("SELECT * FROM scape_trace_data WHERE date > :startDate")
    fun getLastScapeTraces(startDate: Long): List<ScapeTrace>

    @Delete
    fun deleteScapeTrace(scapeTrace: ScapeTrace)
}