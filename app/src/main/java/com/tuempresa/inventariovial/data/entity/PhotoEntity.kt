package com.tuempresa.inventariovial.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "photos",

    foreignKeys = [
        ForeignKey(
            entity = InventoryRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],

    indices = [
        Index("recordId"),

        Index(
            value = [
                "recordId",
                "photoIndex"
            ],
            unique = true
        )
    ]
)
data class PhotoEntity(

    @PrimaryKey
    val id: String,

    val recordId: String,

    val photoIndex: Int,

    val localPath: String,

    val isPrimary: Boolean,

    val generatedFileName: String?,

    val driveFolderId: String?,

    val driveFileId: String?,

    val syncStatus: String,

    val createdAt: Long,
    @androidx.room.ColumnInfo(defaultValue = "''") val originalPath: String = localPath,
    val stampedPath: String? = null,
    val scapInspectionId: String? = null,
    val scapElementCode: String? = null,
    val photoCategory: String? = null,
    val description: String? = null,
    val photoLatitude: Double? = null,
    val photoLongitude: Double? = null,
    val photoAltitudeM: Double? = null,
    val photoGpsAccuracyM: Double? = null,
    val photoGpsTimestamp: Long? = null,
    val photoGnssProvider: String? = null,
    val photoSatellites: Int? = null,
    val photoHdop: Double? = null,
    val photoIsRtkFixed: Boolean? = null,
    val capturedAt: Long? = null,
    @androidx.room.ColumnInfo(defaultValue = "'UNAVAILABLE_LEGACY'") val photoLocationSource: String = "UNAVAILABLE_LEGACY",
    val sha256: String? = null
)
