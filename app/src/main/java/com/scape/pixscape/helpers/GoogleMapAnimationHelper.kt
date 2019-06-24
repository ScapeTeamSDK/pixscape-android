/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.helpers

import com.google.android.libraries.maps.model.CameraPosition
import com.google.android.libraries.maps.model.LatLng


object GoogleMapAnimationHelper {

    /**
     * Creates a camera position with given @params
     *
     * @param target            of the position
     * @param cameraObliqueZoom self explanatory
     * @param cameraObliqueTilt self explanatory
     * @return the CameraPosition object (already .build())
     */
    fun createCameraPosition(target: Array<LatLng>,
                             cameraObliqueZoom: Float,
                             cameraObliqueTilt: Float): CameraPosition {
        return CameraPosition.Builder()
                .target(target[0])
                .zoom(cameraObliqueZoom)
                .tilt(cameraObliqueTilt)
                //.bearing(SphericalUtil.computeHeading(target[0], target[1]) as Float)
                .build()
    }

    /**
     * Creates a camera position with given @params
     *
     * @param target            of the position
     * @param cameraObliqueZoom self explanatory
     * @param cameraObliqueTilt self explanatory
     * @return the CameraPosition object (already .build())
     */
    fun createCameraPosition(target: LatLng,
                             cameraObliqueZoom: Float,
                             cameraObliqueTilt: Float): CameraPosition {
        return CameraPosition.Builder()
                .target(target)
                .zoom(cameraObliqueZoom)
                .tilt(cameraObliqueTilt)
                .bearing(10f)
                .build()
    }

}
