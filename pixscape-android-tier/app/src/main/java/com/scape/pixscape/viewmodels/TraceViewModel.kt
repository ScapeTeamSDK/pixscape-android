package com.scape.pixscape.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.scape.pixscape.database.TraceRoomDatabase
import com.scape.pixscape.models.dto.GpsTrace
import com.scape.pixscape.models.dto.ScapeTrace
import com.scape.pixscape.repository.TraceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal class TraceViewModel(application: Application) : AndroidViewModel(application) {
    private var parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.Main
    private val scope = CoroutineScope(coroutineContext)

    private val mRepository: TraceRepository
    internal val tracesList: LiveData<Pair<LiveData<List<GpsTrace>>, LiveData<List<ScapeTrace>>>>

    init {
        val traceDao = TraceRoomDatabase.invoke(application).traceDataDao()
        mRepository = TraceRepository(traceDao)
        tracesList = mRepository.allTraces
    }

    fun insert(gpsTrace: GpsTrace) = scope.launch(Dispatchers.IO){
        mRepository.insert(gpsTrace)
    }

    fun delete(gpsTrace: GpsTrace) = scope.launch(Dispatchers.IO) {
        mRepository.delete(gpsTrace)
    }

    suspend fun getLastGpsTraces(startDate: Long): List<GpsTrace>{
        return mRepository.getLastGpsTraces(startDate)
    }

    fun insert(scapeTrace: ScapeTrace) = scope.launch(Dispatchers.IO){
        mRepository.insert(scapeTrace)
    }

    fun delete(scapeTrace: ScapeTrace) = scope.launch(Dispatchers.IO) {
        mRepository.delete(scapeTrace)
    }

    suspend fun getLastScapeTraces(startDate: Long): List<ScapeTrace>{
        return mRepository.getLastScapeTraces(startDate)
    }

    override fun onCleared() {
        super.onCleared()
        parentJob.cancel()
    }
}