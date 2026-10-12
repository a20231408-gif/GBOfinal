package com.tuempresa.inventariovial.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Append-only link to originals; intentionally survives removal of an active photograph. */
@Entity(tableName="photo_history", indices=[Index("recordId"),Index("photoId")])
data class PhotoHistoryEntity(@PrimaryKey val id:String, val recordId:String, val photoId:String,
    val revision:Long, val action:String, val snapshotJson:String)
