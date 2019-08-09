package com.scape.pixscape.fragments

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.OnMapReadyCallback
import com.google.android.libraries.maps.model.*
import com.scape.pixscape.R
import com.scape.pixscape.models.dto.RouteSection
import com.scape.pixscape.services.TrackTraceService
import kotlinx.android.synthetic.main.fragment_track_trace.*

internal class TrackTraceFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var fullMap: GoogleMap

    companion object {
        private var gpsRouteSections: List<RouteSection> = ArrayList()
        private var scapeRouteSections: List<RouteSection> = ArrayList()
    }

    private val trackTraceBroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context?, intent: Intent?) {
            val intent = intent ?: return

            when (intent.action) {
                CameraFragment.BROADCAST_ACTION_GPS_LOCATION   -> {
                    if (activity == null) return
                    if(context == null) return

                    gpsRouteSections = intent.getParcelableArrayListExtra(TrackTraceService.ROUTE_GPS_SECTIONS_DATA_KEY)

                    fillMap()
                }
                CameraFragment.BROADCAST_ACTION_SCAPE_LOCATION -> {
                    if (activity == null) return
                    if(context == null) return

                    scapeRouteSections = intent.getParcelableArrayListExtra(TrackTraceService.ROUTE_SCAPE_SECTIONS_DATA_KEY)

                    fillMap()
                }
            }
        }
    }

    // region Private

    private fun vectorToBitmap(id: Int, color: Int) : BitmapDescriptor {
        val vectorDrawable = ResourcesCompat.getDrawable(resources, id, null)
        val bitmap = Bitmap.createBitmap(vectorDrawable!!.intrinsicWidth,
                                         vectorDrawable.intrinsicHeight,
                                         Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        DrawableCompat.setTint(vectorDrawable, color)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun placeMarkerOnMap(location: LatLng, color: Int) {
        val markerOptions = MarkerOptions().position(location)
        markerOptions.icon(vectorToBitmap(R.drawable.gps_user_location, resources.getColor(color)))
        fullMap.addMarker(markerOptions)

        fullMap.addCircle(CircleOptions().center(location)
                                 .radius(2.0)
                                 .strokeColor(ContextCompat.getColor(context!!, color))
                                 .fillColor(ContextCompat.getColor(context!!, color))
                                 .strokeWidth(0.5f))
    }

    @SuppressLint("MissingPermission")
    private fun setUpFullMap() {
        if (context == null) return


        fullMap.isBuildingsEnabled = false
        fullMap.isTrafficEnabled = false
        fullMap.isMyLocationEnabled = false
        fullMap.uiSettings.isZoomControlsEnabled = false
        fullMap.uiSettings.isCompassEnabled = false
        fullMap.uiSettings.isMapToolbarEnabled = false
        fullMap.uiSettings.isMyLocationButtonEnabled = false
        fullMap.setOnMarkerClickListener(this)

        val mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.style_json)
        fullMap.setMapStyle(mapStyleOptions)

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
            fullMap.addCircle(CircleOptions().center(gpsRouteSections[i].beginning.toLatLng())
                                      .radius(2.0)
                                      .strokeColor(ContextCompat.getColor(context!!, R.color.text_color_black))
                                      .fillColor(ContextCompat.getColor(context!!, R.color.text_color_black))
                                      .strokeWidth(0.5f))

            fullMap.addCircle(CircleOptions().center(gpsRouteSections[i].end.toLatLng())
                                      .radius(2.0)
                                      .strokeColor(ContextCompat.getColor(context!!, R.color.text_color_black))
                                      .fillColor(ContextCompat.getColor(context!!, R.color.text_color_black))
                                      .strokeWidth(0.5f))
        }

        if (gpsRouteSections.isNotEmpty()) {
            placeMarkerOnMap(gpsRouteSections.last().end.toLatLng(), R.color.color_primary_dark)

            fullMap.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsRouteSections.last().end.toLatLng(),
                                                                    18f))
        }

        for (i in 0 until scapeRouteSections.size) {
            fullMap.addCircle(CircleOptions().center(scapeRouteSections[i].beginning.toLatLng())
                                         .radius(2.0)
                                         .strokeColor(ContextCompat.getColor(context!!, R.color.scape_color))
                                         .fillColor(ContextCompat.getColor(context!!, R.color.scape_color))
                                         .strokeWidth(0.5f))

            fullMap.addCircle(CircleOptions().center(scapeRouteSections[i].end.toLatLng())
                                         .radius(2.0)
                                         .strokeColor(ContextCompat.getColor(context!!, R.color.scape_color))
                                         .fillColor(ContextCompat.getColor(context!!, R.color.scape_color))
                                         .strokeWidth(0.5f))
        }

        if (scapeRouteSections.isNotEmpty()) {
            placeMarkerOnMap(scapeRouteSections.last().end.toLatLng(), R.color.scape_color)

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

        val intentFilter = IntentFilter()
        intentFilter.addAction(CameraFragment.BROADCAST_ACTION_GPS_LOCATION)
        intentFilter.addAction(CameraFragment.BROADCAST_ACTION_SCAPE_LOCATION)

        activity!!.registerReceiver(trackTraceBroadcastReceiver, intentFilter)

        try {
            full_map_view?.onCreate(savedInstanceState)

            full_map_view?.getMapAsync(this)
        } catch (ex: Resources.NotFoundException) {
            Log.e("Google Map", "Resources\$NotFoundException")
        }
    }

    @SuppressLint("ApplySharedPref")
    override fun onDestroyView() {
        super.onDestroyView()

        try {
            activity!!.unregisterReceiver(trackTraceBroadcastReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e("TrackTraceFrag", e.toString())
        }
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
