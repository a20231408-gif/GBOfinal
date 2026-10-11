package com.tuempresa.inventariovial.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "inventory_records",

    indices = [
        Index("routeCode"),
        Index("sicCode"),
        Index("surveyDate")
    ]
)
data class InventoryRecordEntity(

    @PrimaryKey
    val id: String,

    // SIC-17, SIC-18, ...
    val sicCode: String,

    // PUENTE, ALCANTARILLA, CUNETA,
    // BADEN, TUNEL, MURO, etc.
    val assetType: String,


    // =====================================================
    // UBICACIÓN VIAL
    // =====================================================

    val routeCode: String,

    val roadbedCode: String,

    val startPrCode: String,

    val startDistanceM: Double,

    val endPrCode: String?,

    val endDistanceM: Double?,

    val sideCode: String?,


    // =====================================================
    // GPS
    // =====================================================

    val latitude: Double?,

    val longitude: Double?,

    val altitudeM: Double?,

    val gpsAccuracyM: Double?,


    // =====================================================
    // REGISTRO
    // =====================================================

    val surveyDate: String,

    val observations: String?,

    val status: String,

    val photoSyncStatus: String,

    val excelSyncStatus: String,


    // =====================================================
    // AUDITORÍA
    // =====================================================

    val createdAt: Long,

    val updatedAt: Long,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    val endGpsAccuracyM: Double? = null,
    @androidx.room.ColumnInfo(defaultValue = "'MANUAL'") val locationSource: String = "MANUAL",
    @androidx.room.ColumnInfo(defaultValue = "'MANUAL'") val sideSource: String = "MANUAL",
    val sessionId: String? = null,
    val gpsTimestamp: Long? = null,
    val endGpsTimestamp: Long? = null,
    @androidx.room.ColumnInfo(defaultValue = "'TABLET'") val gnssProvider: String = "TABLET",
    val gnssFixType: String? = null,
    val verticalAccuracyM: Double? = null,
    val satellites: Int? = null,
    val hdop: Double? = null,
    val correctionAge: Double? = null,
    @androidx.room.ColumnInfo(defaultValue = "0") val isRtkFixed: Boolean = false,
    @androidx.room.ColumnInfo(defaultValue = "'PENDING'") val serverSyncStatus: String = "PENDING",
    val serverSyncError: String? = null,
    val segment: String? = null,
    val surveyDirection: String? = null,
    // Geometric distance from the reference start, independent of contractual PR.
    val axisMeasureM: Double? = null,
    val distanceToRoadAxisM: Double? = null,
    val roadMatchConfidence: Double? = null,
    val matchedSegmentId: String? = null,
    val projectedLatitude: Double? = null,
    val projectedLongitude: Double? = null,
    @androidx.room.ColumnInfo(defaultValue = "'MANUAL'") val endLocationSource: String = "MANUAL",
    @androidx.room.ColumnInfo(defaultValue = "'MANUAL'") val routeSource: String = "MANUAL",
    val suggestedRouteCode: String? = null,
    val suggestedPrCode: String? = null,
    val suggestedDistanceFromPrM: Double? = null,
    val routeMatchDistanceM: Double? = null,
    val routeMatchConfidence: Double? = null,
    @androidx.room.ColumnInfo(defaultValue = "0") val routeMatchAmbiguous: Boolean = false,
    val endSuggestedPrCode: String? = null,
    val endSuggestedDistanceFromPrM: Double? = null,
    val endAxisMeasureM: Double? = null,
    val endDistanceToRoadAxisM: Double? = null,
    val endMatchedSegmentId: String? = null,
    val endProjectedLatitude: Double? = null,
    val endProjectedLongitude: Double? = null
)
