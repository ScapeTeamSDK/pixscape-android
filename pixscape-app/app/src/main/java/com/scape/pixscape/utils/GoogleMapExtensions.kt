package com.scape.pixscape.utils

import android.content.res.Resources
import android.util.Log
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.Marker
import com.google.android.libraries.maps.model.MarkerOptions
import com.scape.pixscape.R
import com.scape.pixscape.models.dto.RouteSection

fun GoogleMap.placeMarker(location: LatLng, resources: Resources, resourceId: Int, color: Int): Marker {
    MarkerOptions().apply {
        position(location)
        icon(vectorToBitmap(resources, resourceId, resources.getColor(color)))
        return addMarker(this)
    }
}

fun GoogleMap.animate(latLng: LatLng, zoomLevel: Float = 18f) {
    animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
}

fun GoogleMap.fillMapAndPlaceUserLocation(resources: Resources, gpsRouteSections: List<RouteSection>, scapeRouteSections: List<RouteSection>) {

    fillMap(resources, gpsRouteSections, scapeRouteSections)

    // Place user POI for both gps and Scape
    if (gpsRouteSections.isNotEmpty()) {
        placeMarker(
            gpsRouteSections.last().end.toLatLng(),
            resources,
            R.drawable.gps_user_location,
            R.color.color_primary_dark)

        animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                gpsRouteSections.last().end.toLatLng(),
                18f))
    }

    if (scapeRouteSections.isNotEmpty()) {
        placeMarker(
            scapeRouteSections.last().end.toLatLng(),
            resources,
            R.drawable.gps_user_location,
            R.color.scape_color)

        animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                scapeRouteSections.last().end.toLatLng(),
                18f))
    }
}

fun GoogleMap.fillMap(resources: Resources, gpsRouteSections: List<RouteSection>, scapeRouteSections: List<RouteSection>) {
    try {
        clear()
    } catch (ex: UninitializedPropertyAccessException) {
        Log.w("Google fullMap", "fillMap() invoked with uninitialized fullMap")
        return
    }

    for (i in 0 until gpsRouteSections.size) {
        placeMarker(
            gpsRouteSections[i].beginning.toLatLng(),
            resources,
            R.drawable.circle_marker,
            R.color.color_primary_dark)
        placeMarker(
            gpsRouteSections[i].end.toLatLng(),
            resources,
            R.drawable.circle_marker,
            R.color.color_primary_dark)
    }

    for (i in 0 until scapeRouteSections.size) {
        placeMarker(
            scapeRouteSections[i].beginning.toLatLng(),
            resources,
            R.drawable.circle_marker,
            R.color.scape_color)
        placeMarker(
            scapeRouteSections[i].end.toLatLng(),
            resources,
            R.drawable.circle_marker,
            R.color.scape_color)
    }
}