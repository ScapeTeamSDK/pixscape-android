package com.scape.pixscape.fragments

import android.annotation.SuppressLint
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.OnMapReadyCallback
import com.google.android.libraries.maps.model.CameraPosition
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.MapStyleOptions
import com.google.android.libraries.maps.model.Marker
import com.google.maps.android.data.kml.KmlLayer
import com.scape.pixscape.R
import com.scape.pixscape.events.LocationMeasurementEvent
import com.scape.pixscape.events.ScapeMeasurementEvent
import com.scape.pixscape.models.dto.RouteSection
import com.scape.pixscape.utils.downloadKmlFileAsync
import com.scape.pixscape.utils.placeMarker
import kotlinx.android.synthetic.main.fragment_track_trace.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

internal class TrackTraceFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var fullMap: GoogleMap

    companion object {
        private const val TAG = "TrackTraceFrag"
        private var gpsRouteSections: List<RouteSection> = ArrayList()
        private var scapeRouteSections: List<RouteSection> = ArrayList()
    }

    // region EventBus

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: LocationMeasurementEvent) {
        Log.d(TAG, "LocationMeasurementEvent")

        if (activity == null) return
        if(context == null) return

        gpsRouteSections = event.gpsLocations

        fillMap()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ScapeMeasurementEvent) {
        Log.d(TAG, "ScapeMeasurementEvent")

        if (activity == null) return
        if(context == null) return

        scapeRouteSections = event.scapeLocations

        fillMap()
    }

    // endregion EventBus

    // region Private

    @SuppressLint("MissingPermission")
    private fun setUpFullMap() {
        if (context == null) return

        fullMap.setOnMarkerClickListener(this)
        fullMap.apply {
            isBuildingsEnabled = false
            isTrafficEnabled = false
            isMyLocationEnabled = false
        }
        fullMap.uiSettings.apply {
            isZoomControlsEnabled = false
            isCompassEnabled = false
            isMapToolbarEnabled = false
            isMyLocationButtonEnabled = false
        }

        val mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.style_json)
        fullMap.setMapStyle(mapStyleOptions)


        GlobalScope.launch(Dispatchers.Main) {
            try {
                val layer = KmlLayer(fullMap, downloadKmlFileAsync().await(), context)
                layer.addLayerToMap()
            } catch (ex: Exception) {
                Log.e(TAG, "downloadKmlFileAsync failed, reason: $ex")
            }
        }

        map_mode_switch?.setOnToggledListener { toggleableView, isOn ->
            if (isOn) {
                fullMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            } else {
                fullMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            }
        }

        LocationServices.getFusedLocationProviderClient(activity!!)
                .lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val position = CameraPosition.Builder()
                                .target(LatLng(it.latitude, it.longitude))
                                .zoom(15.0f)
                                .build()
                        fullMap.animateCamera(CameraUpdateFactory.newCameraPosition(position))
                    }
                }
    }

    private fun fillMap() {
        try {
            fullMap.clear()
        } catch (ex: UninitializedPropertyAccessException) {
            Log.w("Google fullMap", "fillMap() invoked with uninitialized fullMap")
            return
        }

        for (i in 0 until gpsRouteSections.size) {
            fullMap.placeMarker(gpsRouteSections[i].beginning.toLatLng(),
                                resources,
                                R.drawable.circle_marker,
                                R.color.color_primary_dark)
            fullMap.placeMarker(gpsRouteSections[i].end.toLatLng(),
                                resources,
                                R.drawable.circle_marker,
                                R.color.color_primary_dark)
        }

        if (gpsRouteSections.isNotEmpty()) {
            fullMap.placeMarker(gpsRouteSections.last().end.toLatLng(),
                                resources,
                                R.drawable.gps_user_location,
                                R.color.color_primary_dark)

            fullMap.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsRouteSections.last().end.toLatLng(),
                                                                    18f))
        }

        for (i in 0 until scapeRouteSections.size) {
            fullMap.placeMarker(scapeRouteSections[i].beginning.toLatLng(),
                                resources,
                                R.drawable.circle_marker,
                                R.color.scape_color)
            fullMap.placeMarker(scapeRouteSections[i].end.toLatLng(),
                                resources,
                                R.drawable.circle_marker,
                                R.color.scape_color)
        }

        if (scapeRouteSections.isNotEmpty()) {
            fullMap.placeMarker(scapeRouteSections.last().end.toLatLng(),
                                resources,
                                R.drawable.gps_user_location,
                                R.color.scape_color)

            fullMap.animateCamera(CameraUpdateFactory.newLatLngZoom(scapeRouteSections.last().end.toLatLng(),
                                                                    18f))
        }
    }

    // endregion

    // region Fragment

    override fun onResume() {
        super.onResume()

        full_map_view?.onResume()
    }

    override fun onPause() {
        super.onPause()

        full_map_view?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()

        full_map_view?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()

        full_map_view?.onLowMemory()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_track_trace, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        EventBus.getDefault().register(this)


        try {
            full_map_view?.onCreate(savedInstanceState)

            full_map_view?.getMapAsync(this)
        } catch (ex: Resources.NotFoundException) {
            Log.e("Google Map", "Resources\$NotFoundException")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        EventBus.getDefault().unregister(this)
    }

    // endregion Fragment

    // region Google Maps

    override fun onMarkerClick(marker: Marker?) = false

    override fun onMapReady(googleMap: GoogleMap) {
        fullMap = googleMap

        setUpFullMap()
    }

    // endregion Google Maps
}
