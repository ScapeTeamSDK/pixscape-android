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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.scape.pixscape.R
import com.scape.pixscape.models.dto.RouteSection
import com.scape.pixscape.services.TrackTraceService
import kotlinx.android.synthetic.main.fragment_track_trace.*

internal class TrackTraceFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap

    companion object {
        private var gpsRouteSections: List<RouteSection> = ArrayList()
        private var scapeRouteSections: List<RouteSection> = ArrayList()
    }

    private val trackTraceBroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent!!.action) {
                CameraFragment.BROADCAST_ACTION_GPS_LOCATION   -> {
                    if (activity == null) return
                    gpsRouteSections = intent.getParcelableArrayListExtra(TrackTraceService.ROUTE_GPS_SECTIONS_DATA_KEY)
                    context?.let {
                        fillMap()
                    }
                }
                CameraFragment.BROADCAST_ACTION_SCAPE_LOCATION -> {
                    if (activity == null) return
                    scapeRouteSections = intent.getParcelableArrayListExtra(TrackTraceService.ROUTE_SCAPE_SECTIONS_DATA_KEY)
                    context?.let {
                        fillMap()
                    }
                }
            }
        }
    }

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
        markerOptions.icon(vectorToBitmap(R.drawable.gps_user_location, color))
        map.addMarker(markerOptions)
    }

    @SuppressLint("MissingPermission")
    private fun setUpMap() {
        if (context == null) return

        map.isMyLocationEnabled = false

        LocationServices.getFusedLocationProviderClient(activity!!)
                .lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val position = CameraPosition.Builder()
                                .target(LatLng(it.latitude, it.longitude))
                                .zoom(15.0f)
                                .build()
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(position))
            }
        }
    }

    private fun fillMap() {
        try {
            map.clear()
        } catch (ex: UninitializedPropertyAccessException) {
            Log.w("Google map", "fillMap() invoked with uninitialized map")
            return
        }

        for (i in 0 until gpsRouteSections.size) {
            map.addPolyline(PolylineOptions().add(gpsRouteSections[i].beginning.toLatLng(),
                                                  gpsRouteSections[i].end.toLatLng())
                                    .color(ContextCompat.getColor(context!!, R.color.text_color_black))
                                    .width(10f))
        }

        if (gpsRouteSections.isNotEmpty()) {
            placeMarkerOnMap(gpsRouteSections.last().end.toLatLng(), R.color.colorPrimaryDark)

            map.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsRouteSections.last().end.toLatLng(),
                                                                18f))
        }

        for (i in 0 until scapeRouteSections.size) {
            map.addPolyline(PolylineOptions().add(scapeRouteSections[i].beginning.toLatLng(),
                                                  scapeRouteSections[i].end.toLatLng())
                                    .color(ContextCompat.getColor(context!!, R.color.scapeColor))
                                    .width(10f))
        }

        if (scapeRouteSections.isNotEmpty()) {
            placeMarkerOnMap(scapeRouteSections.last().end.toLatLng(), R.color.scapeColor)

            map.animateCamera(CameraUpdateFactory.newLatLngZoom(scapeRouteSections.last().end.toLatLng(),
                                                                18f))
        }
    }

    // region Google Maps

    override fun onMarkerClick(marker: Marker?) = false

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.uiSettings.isZoomControlsEnabled = false
        map.setOnMarkerClickListener(this)

        setUpMap()
    }

    // endregion Google Maps

    // region Fragment

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
            track_map.onCreate(savedInstanceState)
            track_map.onResume()
            track_map.getMapAsync(this)
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
}
