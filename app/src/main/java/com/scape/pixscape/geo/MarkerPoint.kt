/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.geo

import android.graphics.drawable.Icon
import com.google.android.libraries.maps.model.Marker
import com.scape.scapekit.LatLng

/**
 * Google Maps Marker for GPS Raw sensors or Scape VPS measurements.
 */
class MarkerPoint {

    lateinit var marker: Marker
    var isScapeMeasurements: Boolean = false
    lateinit var icon: Icon
    lateinit var coordinates: LatLng

    fun init(measurements: LatLng, type: Boolean) {
        this.coordinates = measurements
        this.isScapeMeasurements = type
    }




}