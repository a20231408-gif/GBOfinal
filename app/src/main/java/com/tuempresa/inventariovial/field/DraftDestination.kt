package com.tuempresa.inventariovial.field

import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.model.RoadAssetType
import com.tuempresa.inventariovial.model.SignalizationType

internal data class DraftDestination(val asset:RoadAssetType,val signal:SignalizationType?=null)

/** No default form: an unrecognized identity must never be interpreted as a ford. */
internal fun draftDestination(record:InventoryRecordEntity,classCode:String?=null):DraftDestination = draftDestination(record,classCode,null)
internal fun draftDestination(record:InventoryRecordEntity,classCode:String?,rawSic20Class:String?):DraftDestination = when(record.sicCode) {
    "SIC-22"->DraftDestination(RoadAssetType.SIGNALIZATION,SignalizationType.VERTICAL)
    "SIC-21"->DraftDestination(RoadAssetType.SIGNALIZATION,
        SignalizationType.entries.firstOrNull {it.name==record.assetType && it.sicCode=="SIC-21"}
            ?: when(classCode) {"18"->SignalizationType.HORIZONTAL_MARKS;"19"->SignalizationType.SAFETY;"20"->SignalizationType.HORIZONTAL_STUDS
                else->error("No se reconoce el subtipo SIC-21; conserva la ficha y solicita revisión.")})
    "SIC-20"->DraftDestination(when(com.tuempresa.inventariovial.model.form.Sic20Identity.resolve(record.assetType,classCode,rawSic20Class)) {
        "12"->RoadAssetType.FORD;"13"->RoadAssetType.TUNNEL;"14"->RoadAssetType.WALL
        else->error("Subtipo SIC-20 no reconocido; conserva el borrador.")
    })
    "SIC-17"->DraftDestination(RoadAssetType.BRIDGE)
    "SIC-18"->DraftDestination(RoadAssetType.CULVERT)
    "SIC-19"->DraftDestination(RoadAssetType.DITCH)
    "SIC-23"->DraftDestination(RoadAssetType.RIGHT_OF_WAY)
    else->error("No se puede continuar este tipo de borrador (${record.sicCode}). La ficha se conserva; solicita revisión.")
}
