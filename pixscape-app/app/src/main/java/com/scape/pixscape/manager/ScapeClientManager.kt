/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.manager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import com.scape.pixscape.BuildConfig
import com.scape.pixscape.fragments.CameraFragment
import com.scape.pixscape.models.dto.RouteSection
import com.scape.pixscape.services.TrackTraceService
import com.scape.scapekit.*
import java.util.*

class ScapeClientManager private constructor(context: Context) {

    companion object: SingletonHolder<ScapeClientManager, Context>(::ScapeClientManager)

    var scapeClient: CoreScapeClient? = null
    var context: Context? = null

    private var gpsLocations: MutableList<RouteSection> = ArrayList()
    private var scapeLocations: MutableList<RouteSection> = ArrayList()
    private var scapeMeasurementsStatus: ScapeMeasurementsStatus = ScapeMeasurementsStatus.RESULTS_FOUND

    init {
        this.context = context
        scapeClient = CoreScape.coreScapeClientBuilder
            .withApiKey(BuildConfig.SCAPEKIT_API_KEY)
            .withContext(context)
            .withDebugSupport(true)
            .build()

        scapeClient?.debugSession?.setLogConfig(LogLevel.LOG_DEBUG, EnumSet.of(LogOutput.CONSOLE))
    }

    fun getMeasurements() {
        scapeClient?.scapeSession?.getMeasurements(object: ScapeSessionObserver {
            override fun onScapeCoverageUpdated(session: ScapeSession?, p1: Boolean) {
            }

            override fun onDeviceMotionMeasurementsUpdated(session: ScapeSession?, measurements: MotionMeasurements?) {
            }

            override fun onScapeMeasurementsUpdated(scapeSession: ScapeSession?, measurements: ScapeMeasurements?) {
                println(" onScapeMeasurementsUpdated $measurements")
                val scapeMeasurements = measurements ?: return

                when(scapeMeasurements.measurementsStatus) {
                    ScapeMeasurementsStatus.NO_RESULTS -> {}
                    ScapeMeasurementsStatus.UNAVAILABLE_AREA ->{
                        scapeMeasurementsStatus = ScapeMeasurementsStatus.UNAVAILABLE_AREA
                    }
                    ScapeMeasurementsStatus.RESULTS_FOUND -> {
                        broadcastScapeMeasurements(scapeMeasurements)
                    }
                    ScapeMeasurementsStatus.INTERNAL_ERROR -> {}
                }
            }

            override fun onDeviceLocationMeasurementsUpdated(session: ScapeSession?, measurements: LocationMeasurements?) {
                println(" onScapeMeasurementsUpdated  $measurements")
                if (measurements == null) return

                broadcastLocationMeasurements(measurements)
            }

            override fun onScapeSessionError(session: ScapeSession?, state: ScapeSessionState, error: String) {

                broadcastScapeSessionState(state)
            }

            override fun onScapeMeasurementsRequested(session: ScapeSession?, timestamp: Double) {
            }

        })
    }

    private fun broadcastScapeSessionState(state: ScapeSessionState) {
        val intent = Intent().apply {
            action = CameraFragment.BROADCAST_ACTION_STOP_TIMER
            putExtra(TrackTraceService.SCAPE_ERROR_STATE_KEY, state.ordinal)

            if (scapeMeasurementsStatus == ScapeMeasurementsStatus.UNAVAILABLE_AREA) {
                putExtra(TrackTraceService.SCAPE_MEASUREMENTS_STATUS_KEY, scapeMeasurementsStatus.ordinal)
            }
        }

        context?.sendBroadcast(intent)
    }

    private fun broadcastLocationMeasurements(measurements: LocationMeasurements) {
        val newLatLng = measurements.latLng
        if(newLatLng != null) {
            // single mode is enabled so the RouteSection's beginning and end will be the same
            // basically a single location
            gpsLocations.add(RouteSection(com.scape.pixscape.models.dto.Location(newLatLng.latitude,
                newLatLng.longitude),
                com.scape.pixscape.models.dto.Location(newLatLng.latitude,
                    newLatLng.longitude)))
        }

        val bundle = Bundle().apply {
            putParcelableArrayList(TrackTraceService.ROUTE_GPS_SECTIONS_DATA_KEY, gpsLocations as ArrayList<out Parcelable>)
        }

        val intent = Intent().apply {
            action = CameraFragment.BROADCAST_ACTION_GPS_LOCATION
            putExtras(bundle)
        }

        context?.sendBroadcast(intent)
    }

    private fun broadcastScapeMeasurements(scapeMeasurements: ScapeMeasurements) {
        println(" Confidence score ${scapeMeasurements.confidenceScore}")

        val score = scapeMeasurements.confidenceScore ?: 0.0
        if(score < 3.0) {
            return
        }

        val newLatLng = scapeMeasurements.latLng
        if(newLatLng != null) {
            // single mode is enabled so the RouteSection's beginning and end will be the same
            // basically a single location
            scapeLocations.add(RouteSection(com.scape.pixscape.models.dto.Location(newLatLng.latitude,
                newLatLng.longitude),
                com.scape.pixscape.models.dto.Location(newLatLng.latitude,
                    newLatLng.longitude)))
        }

        val bundle = Bundle().apply {
            putParcelableArrayList(TrackTraceService.ROUTE_SCAPE_SECTIONS_DATA_KEY, scapeLocations as ArrayList<out Parcelable>)
        }
        val intent = Intent().apply {
            action = CameraFragment.BROADCAST_ACTION_SCAPE_LOCATION
            putExtras(bundle)
        }

        context?.sendBroadcast(intent)
    }
}

open class SingletonHolder<out T: Any, in A>(creator: (A) -> T) {
    private var creator: ((A) -> T)? = creator
    @Volatile private var instance: T? = null

    fun sharedInstance(arg: A): T {
        val i = instance
        if (i != null) {
            return i
        }

        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2
            } else {
                val created = creator!!(arg)
                instance = created
                creator = null
                created
            }
        }
    }
}