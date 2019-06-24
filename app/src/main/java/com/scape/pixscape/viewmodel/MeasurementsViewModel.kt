///**
// *
// * Copyright © 2019 Scape Technologies Limited. All rights reserved.
// */
//package com.scape.pixscape.viewmodel
//
//import android.app.Application
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import com.scape.pixscape.model.LatestMeasurements
//
//class MeasurementsViewModel(application: Application) : AndroidViewModel(application) {
//
//    private val scapeMeasurements: MutableLiveData<LatestMeasurements> by lazy {
//        MutableLiveData<LatestMeasurements>().also {
//            getScapeMeasurements()
//        }
//    }
//
//    fun getMeasurements(): LiveData<LatestMeasurements> {
//        return scapeMeasurements
//    }
//
//    private fun getScapeMeasurements() {
//        // async get measurements
//    }
//}