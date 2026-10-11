package com.tuempresa.inventariovial.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "delivery_batches", indices = [Index("routeCode")])
data class DeliveryBatchEntity(@PrimaryKey val id: String, val routeCode: String,
    val fingerprint: String, val manifestPath: String, val namingPolicy: String,
    val status: String, val createdAt: Long, val error: String? = null)
