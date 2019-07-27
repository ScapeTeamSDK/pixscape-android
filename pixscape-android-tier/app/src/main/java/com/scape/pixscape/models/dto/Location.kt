package com.scape.pixscape.models.dto

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Location(val latitude: Double, val longitude: Double) : Parcelable{
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}