package com.scape.pixscape.models.dto

import android.os.Parcel
import android.os.Parcelable

data class RouteSection(val beginning: Location, val end: Location) : Parcelable {
    var distance = 0F
        get(){
            val result = FloatArray(1)
            android.location.Location.distanceBetween(beginning.latitude, beginning.longitude, end.latitude, end.longitude, result)
            return result[0]
        }

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(Location::class.java.classLoader),
        parcel.readParcelable(Location::class.java.classLoader)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(beginning, flags)
        parcel.writeParcelable(end, flags)
    }

    override fun describeContents() = 0

    companion object {
        @Suppress("unused")
        @JvmField val CREATOR = object : Parcelable.Creator<RouteSection> {
            override fun createFromParcel(parcel: Parcel) = RouteSection(parcel)

            override fun newArray(size: Int) = arrayOfNulls<RouteSection>(size)
        }
    }
}