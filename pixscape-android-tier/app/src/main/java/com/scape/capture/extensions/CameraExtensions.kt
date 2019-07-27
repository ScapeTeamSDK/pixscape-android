@file:Suppress("DEPRECATION")

package com.scape.capture.extensions

import android.hardware.Camera
import android.util.Log
import android.view.Surface
import com.scape.capture.graphics.Dimens
import com.scape.capture.utils.CameraUtils

/**
 * Extension methods for Camera class & static utility methods for opening Camera.
 */
object CameraExtensions {

    const val TAG = "CameraExtensions"

    /**
     * Returns Camera with Rotation in Degrees
     */
    fun openCamera(deviceRotation: Int): Pair<Camera, Int> {
        fun processCameraId(id: Int): Pair<Camera, Int> =
                Pair(Camera.open(id), calculateCameraRotation(id, deviceRotation))

        getCameraId()?.let { id ->
            return processCameraId(id)
        }

        throw RuntimeException("Unable to Open Camera")
    }

    private fun getCameraId(): Int? {
        val info = Camera.CameraInfo()
        val numCameras = Camera.getNumberOfCameras()
        for (i in 0 until numCameras) {
            Camera.getCameraInfo(i, info)
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i
            }
        }
        return null
    }

    private fun calculateCameraRotation(cameraId: Int, deviceRotation: Int): Int {
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        val degrees = when (deviceRotation) {
                    Surface.ROTATION_0 -> 0
                    Surface.ROTATION_90 -> 90
                    Surface.ROTATION_180 -> 180
                    Surface.ROTATION_270 -> 270
                    else -> {
                        Log.e(TAG, "Wrong deviceRotation value : " + deviceRotation)
                        0
                    }
                }

        val result: Int = if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                            val orient = (info.orientation + degrees) % 360
                            (360 - orient) % 360  // compensate the mirror
                        } else {  // back-facing
                            (info.orientation - degrees + 360) % 360
                        }

        Log.d(TAG, "Rotation : " + result)

        return result
    }
}

fun Camera.setCameraPreviewSize(windowDimens: Dimens, cameraRotation: Int): Dimens {

    val params = parameters
    CameraUtils.choosePreviewSize(params, cameraRotation, windowDimens)
    // TODO : Consider target FPS Setting
    params.setRecordingHint(true)
    parameters = params

    return parameters.previewSize.let {
        Dimens(it.width, it.height)
    }
}

fun Camera.setFocusMode(mode: String) {
    val params = parameters
    params.focusMode = mode
    parameters = params
}
