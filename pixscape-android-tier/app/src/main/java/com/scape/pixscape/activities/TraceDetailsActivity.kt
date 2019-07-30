package com.scape.pixscape.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.scape.pixscape.R
import com.scape.pixscape.viewmodels.TraceViewModel
import com.scape.pixscape.viewmodels.TraceViewModelFactory
import com.scape.pixscape.models.dto.RouteSection
import com.scape.pixscape.models.dto.GpsTrace
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.scape.pixscape.fragments.CameraFragment
import com.scape.pixscape.models.dto.ScapeTrace
import kotlinx.android.synthetic.main.activity_trace_details.*
import java.util.*
import java.util.concurrent.TimeUnit

internal class TraceDetailsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var sharedPref: SharedPreferences? = null

    private lateinit var currentDate: Date
    private var measuredTimeInMillis = 0L
    private var gpsRouteSections: List<RouteSection> = arrayListOf()
    private var scapeRouteSections: List<RouteSection> = arrayListOf()
    private var launchedFromHistory = true

    private fun fillMap() {
        try {
            googleMap.clear()

            val mapStyleOptions = MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json)
            googleMap.setMapStyle(mapStyleOptions)
        } catch (ex: UninitializedPropertyAccessException) {
            Log.w("Google map", "fillMap() invoked with uninitialized googleMap")
            return
        }

        for (i in 0 until gpsRouteSections.size) {
            googleMap.addPolyline(PolylineOptions().add(gpsRouteSections[i].beginning.toLatLng(),
                                                        gpsRouteSections[i].end.toLatLng())
                                          .color(ContextCompat.getColor(baseContext!!, R.color.text_color_black))
                                          .width(10f))
        }

        for (i in 0 until scapeRouteSections.size) {
            googleMap.addPolyline(PolylineOptions().add(scapeRouteSections[i].beginning.toLatLng(),
                                                        scapeRouteSections[i].end.toLatLng())
                                          .color(ContextCompat.getColor(baseContext!!, R.color.scape_color))
                                          .width(10f))
        }

        if (gpsRouteSections.isNotEmpty()) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsRouteSections.last().end.toLatLng(), 18f))
        }
    }

    // region Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trace_details)

        gpsRouteSections = intent.getParcelableArrayListExtra<RouteSection>(CameraFragment.ROUTE_GPS_SECTIONS_DATA_KEY) as List<RouteSection>

        val scapeList = intent.getParcelableArrayListExtra<RouteSection>(CameraFragment.ROUTE_SCAPE_SECTIONS_DATA_KEY)
        if(!scapeList.isNullOrEmpty()) {
            scapeRouteSections = scapeList as List<RouteSection>
        }

        currentDate = Calendar.getInstance().time
        measuredTimeInMillis = intent.getLongExtra(CameraFragment.TIME_DATA_KEY, 0L)
        launchedFromHistory = intent.getBooleanExtra(CameraFragment.MODE_DATA_KEY, true)

        sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        summary_measured_time.text =
            if (measuredTimeInMillis >= 3600000)
                String.format(
                    "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(measuredTimeInMillis),
                    TimeUnit.MILLISECONDS.toMinutes(measuredTimeInMillis) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(measuredTimeInMillis) % TimeUnit.MINUTES.toSeconds(1)
                )
            else
                String.format(
                    "%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(measuredTimeInMillis) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(measuredTimeInMillis) % TimeUnit.MINUTES.toSeconds(1)
                )

        val distance = gpsRouteSections.sumByDouble { section -> section.distance.toDouble() }
        val distanceInKm = Math.round(distance / 10).toDouble() / 100
        val pace = measuredTimeInMillis / distance / 60
        summary_measured_distance.text = distanceInKm.toString().plus(" km")
        summary_measured_speed.text = (Math.round((distance * 360000.0) / measuredTimeInMillis.toDouble()) / 100.0).toString().plus(" km/h")
        summary_measured_pace.text = String.format(
            "%01d:%02d",
            Math.floor(pace).toInt(),
            Math.floor((pace - Math.floor(pace)) * 60).toInt()
        )

        summary_map.onCreate(savedInstanceState)
        summary_map.onResume()
        summary_map.getMapAsync(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (launchedFromHistory) return

        val gpsViewModel = TraceViewModelFactory(application).create(TraceViewModel::class.java)
        val scapeViewModel = TraceViewModelFactory(application).create(TraceViewModel::class.java)

        val gpsTrace = GpsTrace(currentDate, measuredTimeInMillis, gpsRouteSections)
        val scapeTrace = ScapeTrace(currentDate, measuredTimeInMillis, scapeRouteSections)

        gpsViewModel.insert(gpsTrace)
        scapeViewModel.insert(scapeTrace)
    }

    // endregion Activity

    // region Google Maps

    override fun onMapReady(_googleMap: GoogleMap) {
        googleMap = _googleMap
        fillMap()
    }

    // endregion Google Maps
}
