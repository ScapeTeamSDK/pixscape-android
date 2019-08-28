package com.scape.pixscape.models.dto

import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RouteSection(val beginning: Location, val end: Location) : Parcelable {
    var distance = 0F
        get(){
            val result = FloatArray(1)
            android.location.Location.distanceBetween(beginning.latitude, beginning.longitude, end.latitude, end.longitude, result)
            return result[0]
        }
}