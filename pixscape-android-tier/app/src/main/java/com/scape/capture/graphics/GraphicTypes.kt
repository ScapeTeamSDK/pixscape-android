
package com.scape.capture.graphics

/**
 * Data classes for type-safe handling of graphic related data.
 */

data class Dimens(val width: Int, val height: Int) {
    val ratio = height.toFloat() / width.toFloat()
}

data class XY(val x: Float, val y: Float)