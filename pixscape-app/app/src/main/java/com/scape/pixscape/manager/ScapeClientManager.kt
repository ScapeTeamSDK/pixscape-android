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

    internal object InstanceHolder {
        internal var INSTANCE:  ScapeClientManager? = null
    }

    internal companion object {
        fun sharedInstance(context: Context): ScapeClientManager? {
            if (InstanceHolder.INSTANCE == null) {
                InstanceHolder.INSTANCE = ScapeClientManager(context)
            }

            return InstanceHolder.INSTANCE
        }
    }

    var scapeClient: CoreScapeClient? = null
    var context: Context? = null

    private var gpsLocations: MutableList<RouteSection> = ArrayList()
    private var scapeLocations: MutableList<RouteSection> = ArrayList()

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
                val scapeMeasurements = measurements ?: return

                if (scapeMeasurements.measurementsStatus != ScapeMeasurementsStatus.RESULTS_FOUND) {
                    return
                }

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

            override fun onDeviceLocationMeasurementsUpdated(session: ScapeSession?, measurements: LocationMeasurements?) {
                if (measurements == null) return

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

            override fun onScapeSessionError(session: ScapeSession?, state: ScapeSessionState, error: String) {
                val intent = Intent().apply {
                    action = CameraFragment.BROADCAST_ACTION_STOP_TIMER
                    putExtra(TrackTraceService.SCAPE_ERROR_STATE_KEY, state.ordinal)
                }

                context?.sendBroadcast(intent)
            }

            override fun onScapeMeasurementsRequested(session: ScapeSession?, timestamp: Double) {
            }

        })
    }

}