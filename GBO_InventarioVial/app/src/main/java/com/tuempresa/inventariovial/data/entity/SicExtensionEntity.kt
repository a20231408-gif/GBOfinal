package com.tuempresa.inventariovial.data.entity

import androidx.room.*
import org.json.JSONObject

/** Raw drafts and contractual detail; source identity is a real foreign key, never a label. */
@Entity(tableName = "sic_extension_details", foreignKeys = [
    ForeignKey(entity=InventoryRecordEntity::class, parentColumns=["id"], childColumns=["recordId"], onDelete=ForeignKey.CASCADE),
    ForeignKey(entity=InventoryRecordEntity::class, parentColumns=["id"], childColumns=["sourceRecordId"], onDelete=ForeignKey.RESTRICT)
], indices=[Index("sourceRecordId")])
data class SicExtensionEntity(
    @PrimaryKey val recordId: String,
    val formatCode: String,
    val valuesJson: String,
    val sourceRecordId: String?,
    val ownerUserId: String,
    val status: String
) {
    fun values(): Map<String,String> = JSONObject(valuesJson).let { j -> j.keys().asSequence().associateWith { j.getString(it) } }
}
