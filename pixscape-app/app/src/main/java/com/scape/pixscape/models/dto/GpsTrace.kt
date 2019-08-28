package com.scape.pixscape.models.dto

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "gps_trace_data")
data class GpsTrace(
        @PrimaryKey val date: Date,
        val timeInMillis: Long,
        val routeSections: List<RouteSection>)