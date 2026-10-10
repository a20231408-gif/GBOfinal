package com.tuempresa.inventariovial.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tuempresa.inventariovial.road.RoadPr

@Entity(tableName = "field_prs", indices = [Index(value = ["routeCode", "roadbedCode", "prCode"], unique = true)])
data class FieldPrEntity(
    @PrimaryKey val id: String,
    val routeCode: String, val roadbedCode: String, val prCode: String,
    val type: String, val description: String?,
    val latitude: Double, val longitude: Double, val altitudeM: Double?, val accuracyM: Double,
    val gpsTimestamp: Long, val prAxisMeasureM: Double, val segmentId: String,
    val source: String = "FIELD_CAPTURED", val createdAt: Long
) {
    fun asRoadPr() = RoadPr(routeCode, roadbedCode, prCode, latitude, longitude, prAxisMeasureM)
}
