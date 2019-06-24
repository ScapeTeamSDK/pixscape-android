/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.model

import android.os.Parcel
import android.os.Parcelable
import com.google.android.libraries.maps.model.Marker
import com.scape.scapekit.LatLng

/**
 * Google Maps Marker for GPS Raw sensors or Scape VPS measurements.
 */
 class MarkerPoint() : Parcelable {

    lateinit var marker: Marker
    var mapMode: MapMode.MapMode = MapMode.MapMode.Standard
    lateinit var coordinates: LatLng

    var isFlat = false

    constructor(parcel: Parcel) : this() {
        coordinates = parcel.readParcelable(LatLng::class.java.classLoader)
        isFlat = parcel.readByte() != 0.toByte()
        mapMode = MapMode.MapMode.valueOf(parcel.readString())
    }

    fun init(coordinates: LatLng, mode: MapMode.MapMode, isFlat: Boolean) {
        this.coordinates = coordinates
        this.mapMode = mode
        this.isFlat = isFlat
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(coordinates, flags)
        parcel.writeByte(if (isFlat) 1 else 0)
        parcel.writeString(mapMode.mode)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MarkerPoint> {
        override fun createFromParcel(parcel: Parcel): MarkerPoint {
            return MarkerPoint(parcel)
        }

        override fun newArray(size: Int): Array<MarkerPoint?> {
            return arrayOfNulls(size)
        }
    }


}