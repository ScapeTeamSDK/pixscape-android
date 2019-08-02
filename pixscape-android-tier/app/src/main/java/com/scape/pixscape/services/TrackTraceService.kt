package com.scape.pixscape.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.scape.pixscape.PixscapeApplication
import com.scape.pixscape.PixscapeApplication.Companion.CHANNEL_ID
import com.scape.pixscape.R
import com.scape.pixscape.activities.MainActivity
import com.scape.pixscape.fragments.CameraFragment.Companion.BROADCAST_ACTION_GPS_LOCATION
import com.scape.pixscape.fragments.CameraFragment.Companion.BROADCAST_ACTION_SCAPE_LOCATION
import com.scape.pixscape.fragments.CameraFragment.Companion.BROADCAST_ACTION_STOP_TIMER
import com.scape.pixscape.fragments.CameraFragment.Companion.BROADCAST_ACTION_TIME
import com.scape.pixscape.fragments.CameraFragment.Companion.CONTINUOUS_MODE
import com.scape.pixscape.models.dto.RouteSection
import com.scape.scapekit.*
import java.util.*
import java.util.Timer
import java.util.concurrent.TimeUnit

class TrackTraceService : Service(), ScapeSessionObserver {

    private var isDebug = false

    private var isContinuousModeEnabled: Boolean = false
    private var sharedPref: SharedPreferences? = null
    private lateinit var context: Context

    private var scapeClient: ScapeClient? = null
    private var gpsLocations: MutableList<RouteSection> = ArrayList()
    private var scapeLocations: MutableList<RouteSection> = ArrayList()
    private var lastGpsLocation: LatLng? = null
    private var lastScapeLocation: LatLng? = null

    private var currentTimeInMillis = 0L

    companion object {
        private var timer = Timer()
        var paused = false
        const val NOTIFICATION_ID = 5
        const val SCAPE_ERROR_STATE_KEY = "com.scape.pixscape.trackactivityservice.scapeerrorstatekey"
        const val MILLIS_DATA_KEY = "com.scape.pixscape.trackactivityservice.millisdatakey"
        const val ROUTE_GPS_SECTIONS_DATA_KEY = "com.scape.pixscape.trackactivityservice.routegpssectionsdatakey"
        const val ROUTE_SCAPE_SECTIONS_DATA_KEY = "com.scape.pixscape.trackactivityservice.routegpssectionsdatakey"
    }

    private fun startTrackService(isContinuousModeEnabled: Boolean) {
        timer.scheduleAtFixedRate(UpdateTimeTask(), 10, 10)
        timer.scheduleAtFixedRate(UpdateNotificationTask(), 1000, 1000)
        timer.scheduleAtFixedRate(NotifyTimer(), 1000, 500)

        startUpdatingLocation(isContinuousModeEnabled)

        if(isDebug) {
            Toast.makeText(this,
                           "Tracking started: $isContinuousModeEnabled",
                           Toast.LENGTH_LONG).show()
        }

        val defaultNotification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(0L.toString())
                .setSmallIcon(R.drawable.ic_play_arrow)
                .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), 0))
                .setAutoCancel(true)
                .build()

        startService(NOTIFICATION_ID, defaultNotification)
    }

    @SuppressLint("MissingPermission")
    private fun startUpdatingLocation(isContinuousModeEnabled: Boolean) {
        if(isContinuousModeEnabled) {
            scapeClient?.scapeSession?.startFetch(this)
        } else {
            scapeClient!!.scapeSession!!.getMeasurements(this)
        }
    }

    private fun startService(id: Int, notification: Notification) {
        try {
            startForeground(id, notification)
        } catch(e: Throwable) {
            Log.e("", e.toString())
        }
    }

    private inner class UpdateTimeTask : TimerTask() {
        override fun run() {
            if (!paused) currentTimeInMillis += 10
        }
    }

    private inner class NotifyTimer : TimerTask() {
        override fun run() {
            val intent = Intent()
            intent.action = BROADCAST_ACTION_TIME
            if(isContinuousModeEnabled) {
                intent.putExtra(MILLIS_DATA_KEY, currentTimeInMillis)
            }
            sendBroadcast(intent)
        }
    }

    private inner class UpdateNotificationTask : TimerTask() {
        override fun run() {
            if (!paused) {

                val notificationIntent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0)

                var notification: Notification?
                var distance: Double

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

                    notification = NotificationCompat.Builder(context, CHANNEL_ID)
                            .setContentTitle(getString(R.string.notification_title))
                            .setContentText(timeFormatted.plus(" ").plus(distance.toString()))
                            .setSmallIcon(R.drawable.ic_play_arrow)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .build()
                } else {
                    // don't compute distance and time if single mode is enabled

                    notification = NotificationCompat.Builder(context, CHANNEL_ID)
                            .setContentTitle(getString(R.string.notification_title))
                            .setSmallIcon(R.drawable.ic_play_arrow)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .build()
                }

                val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                mNotificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    // region Service

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        // We create a default dummy notitifcation which allows us to call startForeground as early as possible and in a clean manner
        // so when stopSelf is called, Android does not crash on us with a fatal exception
        // TODO test on Android Nougat and Lollipop devices

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val defaultNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.notification_title))
                    .setContentText(0L.toString())
                    .setSmallIcon(R.drawable.ic_play_arrow)
                    .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), 0))
                    .setAutoCancel(true)
                    .build()
            startForeground(NOTIFICATION_ID, defaultNotification)
        } else {
            startForeground(NOTIFICATION_ID, Notification())
        }

        context = this
        scapeClient = PixscapeApplication.sharedInstance?.scapeClient
        sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
    }

    override fun onDestroy() {
        super.onDestroy()

        paused = false
        currentTimeInMillis = 0
        timer.cancel()
        timer = Timer()

        scapeClient?.scapeSession?.stopFetch()

        if(isDebug) {
            Toast.makeText(this,
                           "Tracking stopped. GPS Locations: ${gpsLocations.size} Scape Locations: ${scapeLocations.size}",
                           Toast.LENGTH_LONG).show()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val continuousModeEnabled = false
        isContinuousModeEnabled = intent?.getBooleanExtra(CONTINUOUS_MODE, continuousModeEnabled) ?: false
        startTrackService(isContinuousModeEnabled)

        return START_NOT_STICKY
    }

    // endregion Service

    // region ScapeSessionObserver

    private fun onDeviceLocationUpdatedSingleMode(measurements: LocationMeasurements?) {
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
            putParcelableArrayList(ROUTE_GPS_SECTIONS_DATA_KEY, gpsLocations as ArrayList<out Parcelable>)
        }

        val intent = Intent()
        intent.action = BROADCAST_ACTION_GPS_LOCATION
        intent.putExtras(bundle)

        sendBroadcast(intent)
    }

    private fun onDeviceLocationUpdatedContinuousMode(measurements: LocationMeasurements?) {
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

        val bundle = Bundle().apply {
            putParcelableArrayList(ROUTE_GPS_SECTIONS_DATA_KEY, gpsLocations as ArrayList<out Parcelable>)
        }

        val intent = Intent()
        intent.action = BROADCAST_ACTION_GPS_LOCATION
        intent.putExtras(bundle)

        sendBroadcast(intent)
    }

    private fun onScapeLocationUpdatedSingleMode(measurements: ScapeMeasurements?) {
        val scapeMeasurements = measurements ?: return

        if (scapeMeasurements?.measurementsStatus != ScapeMeasurementsStatus.RESULTS_FOUND) {
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
            putParcelableArrayList(ROUTE_SCAPE_SECTIONS_DATA_KEY, scapeLocations as ArrayList<out Parcelable>)
        }
        val intent = Intent()
        intent.action = BROADCAST_ACTION_SCAPE_LOCATION
        intent.putExtras(bundle)

        sendBroadcast(intent)
    }

    private fun onScapeLocationUpdatedContinuousMode(measurements: ScapeMeasurements?) {
        if (measurements == null || measurements.measurementsStatus != ScapeMeasurementsStatus.RESULTS_FOUND) return
        if (paused) {
            lastScapeLocation = null
            return
        }

        val score = measurements?.confidenceScore ?: 0.0
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

        val bundle = Bundle().apply {
            putParcelableArrayList(ROUTE_SCAPE_SECTIONS_DATA_KEY, scapeLocations as ArrayList<out Parcelable>)
        }
        val intent = Intent()
        intent.action = BROADCAST_ACTION_SCAPE_LOCATION
        intent.putExtras(bundle)

        sendBroadcast(intent)
    }

    override fun onDeviceLocationMeasurementsUpdated(session: ScapeSession?, measurements: LocationMeasurements?) {
        if(isContinuousModeEnabled) {
            onDeviceLocationUpdatedContinuousMode(measurements)
        } else {
            onDeviceLocationUpdatedSingleMode(measurements)
        }
    }

    override fun onScapeMeasurementsUpdated(session: ScapeSession?, measurements: ScapeMeasurements?) {
        if(isContinuousModeEnabled) {
            onScapeLocationUpdatedContinuousMode(measurements)
        } else {
            onScapeLocationUpdatedSingleMode(measurements)
        }
    }

    override fun onDeviceMotionMeasurementsUpdated(session: ScapeSession?, measurements: MotionMeasurements?) {

    }

    override fun onScapeSessionError(session: ScapeSession?, state: ScapeSessionState, error: String) {
        if(!isContinuousModeEnabled) {
            val intent = Intent()
            intent.action = BROADCAST_ACTION_STOP_TIMER
            intent.putExtra(SCAPE_ERROR_STATE_KEY, state.ordinal)

            sendBroadcast(intent)
        }
    }

    override fun onScapeMeasurementsRequested(session: ScapeSession?, timestamp: Double) {

    }

    // endregion ScapeSessionObserver
}
