/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.manager

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.scape.pixscape.BuildConfig
import com.scape.pixscape.PixscapeApplication
import com.scape.pixscape.R
import com.scape.pixscape.activities.MainActivity
import com.scape.pixscape.fragments.CameraFragment
import com.scape.pixscape.fragments.CameraFragment.Companion.BROADCAST_ACTION_SCAPE_LOCATION
import com.scape.pixscape.models.dto.RouteSection
import com.scape.pixscape.services.TrackTraceService
import com.scape.pixscape.services.TrackTraceService.Companion.ROUTE_SCAPE_SECTIONS_DATA_KEY
import com.scape.pixscape.utils.getNetworkStrengthInInt
import com.scape.pixscape.utils.getSignalStrength
import com.scape.scapekit.*
import java.util.*
import java.util.Timer
import java.util.concurrent.TimeUnit


class TrackTraceManager private constructor(context: Context): ScapeSessionObserver {

    companion object: SingletonHolder<TrackTraceManager, Context>(::TrackTraceManager)

    var scapeClient: CoreScapeClient? = null
    var context: Context? = null

    private var scapeMeasurementsStatus: ScapeMeasurementsStatus = ScapeMeasurementsStatus.RESULTS_FOUND

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

        scapeClient?.debugSession?.setLogConfig(LogLevel.LOG_VERBOSE, EnumSet.of(LogOutput.CONSOLE))
    }

    fun startUpdatingLocation(isContinuousModeEnabled: Boolean) {
        this.isContinuousModeEnabled = isContinuousModeEnabled

        timer.scheduleAtFixedRate(UpdateTimeTask(), 10, 10)
        timer.scheduleAtFixedRate(UpdateNotificationTask(), 1000, 1000)
        timer.scheduleAtFixedRate(NotifyTimer(), 1000, 500)

        if(isDebug) {
            Toast.makeText(context,
                "Tracking started: $isContinuousModeEnabled",
                Toast.LENGTH_LONG).show()
        }

        if (scapeClient != null && scapeClient?.scapeSession != null) {
            if(isContinuousModeEnabled) {
                scapeClient?.scapeSession?.startFetch(this)
            } else {
                // we use startFetch until we get one measurement/error, until we fix getMeasurements on SDK side

                // only 3G:::: -127, -84
                // only WIFI:: -42, -86
                // both:::
                // none::
                // wifi via hotspot::

//                val initialTime = System.currentTimeMillis()
//                val strength = getNetworkStrengthInInt(context!!)
//                val endTime = System.currentTimeMillis()
//
//                println("Duration: ${endTime-initialTime} strength: ${strength}")
//
//                val sss = getSignalStrength(context!!)
//                val endTime2 = System.currentTimeMillis()
//
//                println("Duration: ${endTime2-endTime} cell strength: $sss")

                scapeClient?.scapeSession?.startFetch(this)
            }
        }
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

    fun pauseTimer(on: Boolean) {
        paused = on
    }

    private fun broadcastScapeSessionState(state: ScapeSessionState) {
        if (!isContinuousModeEnabled) {
            val intent = Intent().apply {
                action = CameraFragment.BROADCAST_ACTION_STOP_TIMER
                putExtra(TrackTraceService.SCAPE_ERROR_STATE_KEY, state.ordinal)
                putExtra(TrackTraceService.SCAPE_MEASUREMENTS_STATUS_KEY, scapeMeasurementsStatus.ordinal)
            }

            context?.sendBroadcast(intent)

            scapeClient?.scapeSession?.stopFetch()
        }
    }

    private fun broadcastLocationMeasurements() {
        val bundle = Bundle().apply {
            putParcelableArrayList(TrackTraceService.ROUTE_GPS_SECTIONS_DATA_KEY, gpsLocations as ArrayList<out Parcelable>)
        }

        val intent = Intent().apply {
            action = CameraFragment.BROADCAST_ACTION_GPS_LOCATION
            putExtras(bundle)
        }

        context?.sendBroadcast(intent)
    }

    private fun broadcastScapeMeasurements() {
        val bundle = Bundle().apply {
            putParcelableArrayList(ROUTE_SCAPE_SECTIONS_DATA_KEY, scapeLocations as ArrayList<out Parcelable>)
        }
        val intent = Intent().apply {
            action = BROADCAST_ACTION_SCAPE_LOCATION
            putExtras(bundle)
        }

        context?.sendBroadcast(intent)

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
            val intent = Intent()
            intent.action = CameraFragment.BROADCAST_ACTION_TIME
            if(isContinuousModeEnabled) {
                intent.putExtra(TrackTraceService.MILLIS_DATA_KEY, currentTimeInMillis)

                context?.sendBroadcast(intent)
            }

        }
    }

    private inner class UpdateNotificationTask : TimerTask() {
        override fun run() {
            if (!paused) {

                val notificationIntent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0)

                val notification: Notification?
                val distance: Double

                if(isContinuousModeEnabled) {
                    val timeFormatted =
                        if (currentTimeInMillis >= 3600000)
                            String.format(
                                "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(currentTimeInMillis),
                                TimeUnit.MILLISECONDS.toMinutes(currentTimeInMillis) % TimeUnit.HOURS.toMinutes(1),
                                TimeUnit.MILLISECONDS.toSeconds(currentTimeInMillis) % TimeUnit.MINUTES.toSeconds(1)
                            )
                        else
                            String.format(
                                "%02d:%02d",
                                TimeUnit.MILLISECONDS.toMinutes(currentTimeInMillis) % TimeUnit.HOURS.toMinutes(1),
                                TimeUnit.MILLISECONDS.toSeconds(currentTimeInMillis) % TimeUnit.MINUTES.toSeconds(1)
                            )

                    // use gpsLocations instead of scapeLocations as we will probably have more of them
                    distance = Math.round(gpsLocations.sumByDouble { section -> section.distance.toDouble() } * 100.0) / 100.0

                    notification = NotificationCompat.Builder(context!!, PixscapeApplication.CHANNEL_ID)
                        .setContentTitle(context?.getString(R.string.notification_title))
                        .setContentText(timeFormatted.plus(" ").plus(distance.toString()))
                        .setSmallIcon(R.drawable.ic_play_arrow)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build()
                } else {
                    // don't compute distance and time if single mode is enabled

                    notification = NotificationCompat.Builder(context!!, PixscapeApplication.CHANNEL_ID)
                        .setContentTitle(context?.getString(R.string.notification_title))
                        .setSmallIcon(R.drawable.ic_play_arrow)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build()
                }

                val mNotificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                mNotificationManager.notify(TrackTraceService.NOTIFICATION_ID, notification)
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
        Log.d("observer"," onScapeSessionError  $state")

        if (!isContinuousModeEnabled) {
            broadcastScapeSessionState(state)
        }
    }

    override fun onScapeMeasurementsRequested(session: ScapeSession?, timestamp: Double) {

    }

    // endregion ScapeSessionObserver

    // region Private

    private fun onDeviceLocationUpdatedSingleMode(measurements: LocationMeasurements?) {
        Log.d("observer"," onDeviceLocationMeasurementsUpdated  $measurements")
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

        broadcastLocationMeasurements()
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

        broadcastLocationMeasurements()
    }

    private fun onScapeLocationUpdatedSingleMode(measurements: ScapeMeasurements?) {
        Log.d("observer"," onScapeMeasurementsUpdated $measurements ")
        val scapeMeasurements = measurements ?: return

        when(scapeMeasurements.measurementsStatus) {
            ScapeMeasurementsStatus.NO_RESULTS -> {
                scapeMeasurementsStatus = ScapeMeasurementsStatus.NO_RESULTS
            }
            ScapeMeasurementsStatus.UNAVAILABLE_AREA -> {
                scapeMeasurementsStatus = ScapeMeasurementsStatus.UNAVAILABLE_AREA
            }
            ScapeMeasurementsStatus.RESULTS_FOUND -> {
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

                broadcastScapeMeasurements()
            }
            ScapeMeasurementsStatus.INTERNAL_ERROR -> {}
        }

        scapeClient?.scapeSession?.stopFetch()
    }

    private fun onScapeLocationUpdatedContinuousMode(measurements: ScapeMeasurements?) {
        if (measurements == null || measurements.measurementsStatus != ScapeMeasurementsStatus.RESULTS_FOUND) return
        if (paused) {
            lastScapeLocation = null
            return
        }

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

        broadcastScapeMeasurements()
    }

    // endregion Private
}