/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.manager

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import android.widget.Toast
import com.scape.pixscape.BuildConfig
import com.scape.pixscape.events.LocationMeasurementEvent
import com.scape.pixscape.events.ScapeMeasurementEvent
import com.scape.pixscape.events.ScapeSessionStateEvent
import com.scape.pixscape.events.TimerEvent
import com.scape.pixscape.fragments.CameraFragment.Companion.BROADCAST_CONNECTIVITY_OFF
import com.scape.pixscape.fragments.CameraFragment.Companion.BROADCAST_CONNECTIVITY_ON
import com.scape.pixscape.models.dto.RouteSection
import com.scape.pixscape.utils.MAX_SCAPE_CONFIDENCE_SCORE
import com.scape.pixscape.utils.MIN_SCAPE_CONFIDENCE_SCORE
import com.scape.scapekit.*
import org.greenrobot.eventbus.EventBus
import java.util.*
import java.util.Timer


class TrackTraceManager private constructor(context: Context): ScapeSessionObserver {

    companion object: SingletonHolder<TrackTraceManager, Context>(::TrackTraceManager)

    var scapeClient: CoreScapeClient? = null
    var context: Context? = null

    private var scapeMeasurementsStatus: ScapeMeasurementsStatus = ScapeMeasurementsStatus.RESULTS_FOUND
    private var scapeConfidenceScore = MAX_SCAPE_CONFIDENCE_SCORE

    private var isDebug = false

    private var isContinuousModeEnabled: Boolean = false

    private var gpsLocations: MutableList<RouteSection> = ArrayList()
    private var scapeLocations: MutableList<RouteSection> = ArrayList()
    private var lastGpsLocation: LatLng? = null
    private var lastScapeLocation: LatLng? = null

    private var currentTimeInMillis = 0L
    var paused = false
    var timer = Timer()

    init {
        this.context = context
        scapeClient = CoreScape.coreScapeClientBuilder
            .withApiKey(BuildConfig.SCAPEKIT_API_KEY)
            .withContext(context)
            .withDebugSupport(true)
            .build()

        scapeClient?.debugSession?.setLogConfig(LogLevel.LOG_DEBUG, EnumSet.of(LogOutput.CONSOLE))

        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                //take action when network connection is gained
                Log.w("Connectivity","Network is available")
                broadcastNetworkConnectivityChange(true)
            }
            override fun onLost(network: Network?) {
                //take action when network connection is lost
                Log.w("Connectivity", "Connection lost")
                broadcastNetworkConnectivityChange(false)
            }
        })
    }

    fun startUpdatingLocation(isContinuousModeEnabled: Boolean) {
        this.isContinuousModeEnabled = isContinuousModeEnabled

        if(isDebug) {
            Toast.makeText(context,
                "Tracking started: $isContinuousModeEnabled",
                Toast.LENGTH_LONG).show()
        }

        timer.scheduleAtFixedRate(UpdateTimeTask(), 10, 10)
        timer.scheduleAtFixedRate(NotifyTimer(), 1000, 500)

        if (scapeClient != null && scapeClient?.scapeSession != null) {
            if(isContinuousModeEnabled) {
                scapeClient?.scapeSession?.startFetch(this)
            } else {
                // we use startFetch until we get one measurement/error, until we fix getMeasurements on SDK side
                scapeClient?.scapeSession?.startFetch(this)
            }
        }
    }

    fun pauseUpdatingContinuousLocation() {
        paused = true

        scapeClient?.scapeSession?.stopFetch()
    }

    fun resumeUpdatingContinuousLocation() {
        paused = false

        scapeClient?.scapeSession?.startFetch(this)

    }
    fun stopUpdatingLocation(isContinuousModeEnabled: Boolean) {
        this.isContinuousModeEnabled = isContinuousModeEnabled

        paused = false
        currentTimeInMillis = 0
        timer.cancel()
        timer = Timer()

        scapeClient?.scapeSession?.stopFetch()

        if(isDebug) {
            Toast.makeText(context,
                "Tracking stopped. GPS Locations: ${gpsLocations.size} Scape Locations: ${scapeLocations.size}",
                Toast.LENGTH_LONG).show()
        }
    }

    private fun broadcastNetworkConnectivityChange(isNetworkAvailable: Boolean) {
        val intent = Intent().apply {
            action = if (isNetworkAvailable) BROADCAST_CONNECTIVITY_ON else BROADCAST_CONNECTIVITY_OFF
        }

        context?.sendBroadcast(intent)
    }

    private fun broadcastScapeSessionStateEvent(state: ScapeSessionState) {

        EventBus.getDefault().post(ScapeSessionStateEvent(state, scapeMeasurementsStatus, scapeConfidenceScore))
    }

    private fun broadcastLocationMeasurementsEvent() {

        EventBus.getDefault().post(LocationMeasurementEvent(gpsLocations))
    }

    private fun broadcastScapeMeasurementsEvent() {

        EventBus.getDefault().post(ScapeMeasurementEvent(scapeLocations))
    }

    open class SingletonHolder<out T: Any, in A>(creator: (A) -> T) {
        private var creator: ((A) -> T)? = creator
        @Volatile
        private var instance: T? = null

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

    // region Timer

    private inner class UpdateTimeTask : TimerTask() {
        override fun run() {
            if (!paused) currentTimeInMillis += 10
        }
    }

    private inner class NotifyTimer : TimerTask() {
        override fun run() {

            if(isContinuousModeEnabled) {
                EventBus.getDefault().post(TimerEvent(currentTimeInMillis))
            }
        }
    }

    // endregion Timer

    // region ScapeSessionObserver
    override fun onScapeCoverageUpdated(p0: ScapeSession?, p1: Boolean) {

    }

    override fun onDeviceLocationMeasurementsUpdated(session: ScapeSession?, measurements: LocationMeasurements?) {
        if(isContinuousModeEnabled) {
            onDeviceLocationUpdatedContinuousMode(measurements)
        }
        else {
            onDeviceLocationUpdatedSingleMode(measurements)
        }
    }

    override fun onScapeMeasurementsUpdated(session: ScapeSession?, measurements: ScapeMeasurements?) {
        if(isContinuousModeEnabled) {
            onScapeLocationUpdatedContinuousMode(measurements)
        }
        else {
            onScapeLocationUpdatedSingleMode(measurements)
        }
    }

    override fun onDeviceMotionMeasurementsUpdated(session: ScapeSession?, measurements: MotionMeasurements?) {

    }

    override fun onScapeSessionError(session: ScapeSession?, state: ScapeSessionState, error: String) {
        if (!paused) {
            broadcastScapeSessionStateEvent(state)
        }
    }

    override fun onScapeMeasurementsRequested(session: ScapeSession?, timestamp: Double) {

    }

    // endregion ScapeSessionObserver

    // region Private

    private fun onDeviceLocationUpdatedSingleMode(measurements: LocationMeasurements?) {
        Log.d("Manager"," onDeviceLocationMeasurementsUpdated  $measurements")
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

        broadcastLocationMeasurementsEvent()
    }

    private fun onDeviceLocationUpdatedContinuousMode(measurements: LocationMeasurements?) {
        Log.d("Manager", "onDeviceLocationUpdatedContinuousMode")

        if (measurements == null) return

        if (paused) {
            lastGpsLocation = null
            return
        }

        val lastLocation = lastGpsLocation

        // unless it is the first location after start or resume, save section to list
        if (lastLocation != null) {
            val newLatLng = measurements.latLng
            if(newLatLng != null) {
                gpsLocations.add(RouteSection(com.scape.pixscape.models.dto.Location(lastLocation.latitude,
                    lastLocation.longitude),
                    com.scape.pixscape.models.dto.Location(newLatLng.latitude,
                        newLatLng.longitude)))
            }
        }
        lastGpsLocation = measurements.latLng

        broadcastLocationMeasurementsEvent()
    }

    private fun onScapeLocationUpdatedSingleMode(measurements: ScapeMeasurements?) {
        Log.d("Manager"," onScapeMeasurementsUpdated $measurements ")

        scapeClient?.scapeSession?.stopFetch()

        val scapeMeasurements = measurements ?: return

        when(scapeMeasurements.measurementsStatus) {
            ScapeMeasurementsStatus.NO_RESULTS -> {
                scapeMeasurementsStatus = ScapeMeasurementsStatus.NO_RESULTS
            }
            ScapeMeasurementsStatus.UNAVAILABLE_AREA -> {
                scapeMeasurementsStatus = ScapeMeasurementsStatus.UNAVAILABLE_AREA
            }
            ScapeMeasurementsStatus.RESULTS_FOUND -> {
                scapeConfidenceScore = scapeMeasurements.confidenceScore ?: MAX_SCAPE_CONFIDENCE_SCORE

                if(scapeConfidenceScore < MIN_SCAPE_CONFIDENCE_SCORE) {

                    broadcastScapeSessionStateEvent(ScapeSessionState.NO_ERROR)
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

                broadcastScapeMeasurementsEvent()
            }
            ScapeMeasurementsStatus.INTERNAL_ERROR -> {
                broadcastScapeSessionStateEvent(ScapeSessionState.UNEXPECTED_ERROR)
            }
        }
    }

    private fun onScapeLocationUpdatedContinuousMode(measurements: ScapeMeasurements?) {
        if (paused) {
            lastScapeLocation = null
            return
        }

        val scapeMeasurements = measurements ?: return

        when (scapeMeasurements.measurementsStatus) {
            ScapeMeasurementsStatus.RESULTS_FOUND -> {
                val score = measurements.confidenceScore ?: 0.0
                if (score < 3.0) return

                val lastLocation = lastScapeLocation

                // unless it is the first location after start or resume, save section to list
                if (lastLocation != null) {
                    val newLatLng = measurements.latLng
                    if(newLatLng != null) {
                        scapeLocations.add(RouteSection(com.scape.pixscape.models.dto.Location(lastLocation.latitude,
                            lastLocation.longitude),
                            com.scape.pixscape.models.dto.Location(newLatLng.latitude,
                                newLatLng.longitude)))
                    }
                }
                lastScapeLocation = measurements.latLng

                broadcastScapeMeasurementsEvent()
            }
            ScapeMeasurementsStatus.NO_RESULTS -> {
                scapeMeasurementsStatus = ScapeMeasurementsStatus.NO_RESULTS
            }
            ScapeMeasurementsStatus.UNAVAILABLE_AREA -> {
                scapeMeasurementsStatus = ScapeMeasurementsStatus.UNAVAILABLE_AREA
            }
            ScapeMeasurementsStatus.INTERNAL_ERROR -> {
                scapeMeasurementsStatus = ScapeMeasurementsStatus.INTERNAL_ERROR
            }
        }
    }

    // endregion Private
}