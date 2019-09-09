package com.scape.pixscape.utils

import android.content.res.Resources
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.MarkerOptions

fun GoogleMap.placeMarker(location: LatLng, resources: Resources, resourceId: Int, color: Int) {
    MarkerOptions().apply {
        position(location)
        icon(vectorToBitmap(resources, resourceId, resources.getColor(color)))
        addMarker(this)
    }
}