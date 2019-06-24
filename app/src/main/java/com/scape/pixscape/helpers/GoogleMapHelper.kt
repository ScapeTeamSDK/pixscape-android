/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.helpers

import com.google.android.libraries.maps.model.BitmapDescriptor
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.MarkerOptions

object GoogleMapHelper {

    /**
     * Overflow method with title
     *
     * @param markerTarget         self explanatory
     * @param isFlat               self explanatory
     * @param iconBitmapDescriptor icon of the marker
     * @return markerOptions object with given given specs
     */
    fun createMarker(markerTarget: LatLng,
                     isFlat: Boolean,
                     title: String,
                     iconBitmapDescriptor: BitmapDescriptor): MarkerOptions {

        return MarkerOptions().position(markerTarget)
                .flat(isFlat)
                .title(title)
                .icon(iconBitmapDescriptor)
    }

}
