package com.scape.pixscape.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.OnMapReadyCallback
import com.google.android.libraries.maps.SupportMapFragment
import com.google.android.libraries.maps.model.MapStyleOptions
import com.google.maps.android.data.kml.KmlLayer
import com.scape.pixscape.R
import com.scape.pixscape.fragments.CameraFragment
import com.scape.pixscape.models.dto.GpsTrace
import com.scape.pixscape.models.dto.RouteSection
import com.scape.pixscape.models.dto.ScapeTrace
import com.scape.pixscape.utils.downloadKmlFileAsync
import com.scape.pixscape.utils.placeMarker
import com.scape.pixscape.viewmodels.TraceViewModel
import com.scape.pixscape.viewmodels.TraceViewModelFactory
import kotlinx.android.synthetic.main.activity_trace_details.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

internal class TraceDetailsActivity : AppCompatActivity(), OnMapReadyCallback,
                                      GoogleMap.OnMapLoadedCallback {

    private var googleMap: GoogleMap? = null

    private lateinit var currentDate: Date
    private var measuredTimeInMillis = 0L
    private var gpsRouteSections: List<RouteSection> = arrayListOf()
    private var scapeRouteSections: List<RouteSection> = arrayListOf()
    private var launchedFromHistory = true

    private fun initMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.summary_map) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    private fun fillMap() {
        try {
            googleMap?.clear()

            val mapStyleOptions = MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json)
            googleMap?.setMapStyle(mapStyleOptions)

            GlobalScope.launch(Dispatchers.Main) {
                try {
                    val layer = KmlLayer(googleMap, downloadKmlFileAsync().await(), applicationContext)
                    layer.addLayerToMap()
                } catch (ex: Exception) {
                    Log.e("downloadKmlFileAsync", "cannot download $ex")
                }

            }
        } catch (ex: UninitializedPropertyAccessException) {
            Log.w("Google map", "fillMap() invoked with uninitialized googleMap")
            return
        }

        for (i in 0 until gpsRouteSections.size) {
            googleMap?.placeMarker(gpsRouteSections[i].beginning.toLatLng(),
                                    resources,
                                    R.drawable.circle_marker,
                                    R.color.color_primary_dark)
            googleMap?.placeMarker(gpsRouteSections[i].end.toLatLng(),
                                    resources,
                                    R.drawable.circle_marker,
                                    R.color.color_primary_dark)
        }

        for (i in 0 until scapeRouteSections.size) {
            googleMap?.placeMarker(scapeRouteSections[i].beginning.toLatLng(),
                                    resources,
                                    R.drawable.circle_marker,
                                    R.color.scape_color)
            googleMap?.placeMarker(scapeRouteSections[i].end.toLatLng(),
                                   resources,
                                   R.drawable.circle_marker,
                                   R.color.scape_color)
        }

        if (gpsRouteSections.isNotEmpty()) {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsRouteSections.last().end.toLatLng(), 18f))
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

        initMap()
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

    override fun onMapLoaded() {
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap?.setOnMapLoadedCallback(this)

        googleMap?.isTrafficEnabled = false
        googleMap?.isBuildingsEnabled = false

        map_mode_switch?.setOnToggledListener { toggleableView, isOn ->
            if (isOn) {
                googleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
            } else {
                googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
            }
        }

        fillMap()
    }

    // endregion Google Maps
}
