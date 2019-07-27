package com.scape.pixscape.repository

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.scape.pixscape.models.dto.GpsTrace
import com.scape.pixscape.dao.TraceDao
import com.scape.pixscape.models.dto.ScapeTrace

fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { setValue(initialValue) }

internal class TraceRepository(private val traceDao: TraceDao) {
    var allTraces = MutableLiveData<Pair<LiveData<List<GpsTrace>>, LiveData<List<ScapeTrace>>>>().default(Pair(traceDao.getAllGpsTraces(), traceDao.getAllScapeTraces()))

    @WorkerThread
    suspend fun insert(gpsTrace: GpsTrace) {
        traceDao.insertGpsTrace(gpsTrace)
    }

    @WorkerThread
    suspend fun delete(gpsTrace: GpsTrace){
        traceDao.deleteGpsTrace(gpsTrace)
    }

    suspend fun getLastGpsTraces(startDate: Long): List<GpsTrace> {
        return traceDao.getLastGpsTraces(startDate)
    }

    @WorkerThread
    suspend fun insert(scapeTrace: ScapeTrace) {
        traceDao.insertScapeTrace(scapeTrace)
    }

    @WorkerThread
    suspend fun delete(scapeTrace: ScapeTrace){
        traceDao.deleteScapeTrace(scapeTrace)
    }

    suspend fun getLastScapeTraces(startDate: Long): List<ScapeTrace> {
        return traceDao.getLastScapeTraces(startDate)
    }
}