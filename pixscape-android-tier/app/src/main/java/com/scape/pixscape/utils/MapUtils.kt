package com.scape.pixscape.utils

import com.google.android.libraries.maps.model.LatLng

fun interpolate(from: LatLng, to: LatLng, fraction: Double): LatLng {
    val fromLat = Math.toRadians(from.latitude)
    val fromLng = Math.toRadians(from.longitude)
    val toLat = Math.toRadians(to.latitude)
    val toLng = Math.toRadians(to.longitude)
    val cosFromLat = Math.cos(fromLat)
    val cosToLat = Math.cos(toLat)
    val angle = computeAngleBetween(from, to)
    val sinAngle = Math.sin(angle)
    if (sinAngle < 1.0E-6) {
        return from
    } else {
        val a = Math.sin((1.0 - fraction) * angle) / sinAngle
        val b = Math.sin(fraction * angle) / sinAngle
        val x = a * cosFromLat * Math.cos(fromLng) + b * cosToLat * Math.cos(toLng)
        val y = a * cosFromLat * Math.sin(fromLng) + b * cosToLat * Math.sin(toLng)
        val z = a * Math.sin(fromLat) + b * Math.sin(toLat)
        val lat = Math.atan2(z, Math.sqrt(x * x + y * y))
        val lng = Math.atan2(y, x)
        return LatLng(Math.toDegrees(lat), Math.toDegrees(lng))
    }
}

private fun distanceRadians(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    return arcHav(havDistance(lat1, lat2, lng1 - lng2))
}

internal fun computeAngleBetween(from: LatLng, to: LatLng): Double {
    return distanceRadians(Math.toRadians(from.latitude),
                           Math.toRadians(from.longitude),
                           Math.toRadians(to.latitude),
                           Math.toRadians(to.longitude))
}

fun arcHav(x: Double): Double {
    return 2.0 * Math.asin(Math.sqrt(x))
}

fun havDistance(lat1: Double, lat2: Double, dLng: Double): Double {
    return hav(lat1 - lat2) + hav(dLng) * Math.cos(lat1) * Math.cos(lat2)
}

fun hav(x: Double): Double {
    val sinHalf = Math.sin(x * 0.5)
    return sinHalf * sinHalf
}