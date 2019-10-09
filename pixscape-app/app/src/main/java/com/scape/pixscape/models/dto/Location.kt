package com.scape.pixscape.models.dto

import android.os.Parcel
import android.os.Parcelable
import com.google.android.libraries.maps.model.LatLng


data class Location(val latitude: Double, val longitude: Double) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readDouble()
    )

    override fun writeToParcel(dest: Parcel?, flags: Int) {
       dest?.writeDouble(latitude)
       dest?.writeDouble(longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun toLatLng(): LatLng = LatLng(latitude, longitude)

    companion object {
        @Suppress("unused")
        @JvmField val CREATOR = object : Parcelable.Creator<Location> {
            override fun createFromParcel(parcel: Parcel) = Location(parcel)

            override fun newArray(size: Int) = arrayOfNulls<Location>(size)
        }
    }
}