package com.tuempresa.inventariovial.repository

import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.validation.ContractualLength
import com.tuempresa.inventariovial.road.RoadPr
import org.json.JSONObject

/** Reuses the existing extension table. Missing mode means historical measured length. */
object TechnicalLengthStore {
    const val REFERENCE="_linearReference"
    const val KILOMETRES="EXPLICIT_KILOMETRES"
    suspend fun rememberReference(db:InventoryDatabase,record:InventoryRecordEntity,reference:String?,strict:Boolean=false) {
        if(reference==null)return
        require(reference in setOf(KILOMETRES,"CONTRACTUAL_PR"))
        val old=db.inventoryDao().sicExtension(record.id)
        val values=old?.values().orEmpty().toMutableMap()
        val result=if(reference==KILOMETRES)ContractualLength.kilometres(
            com.tuempresa.inventariovial.validation.LengthEndpoint(record.routeCode,record.roadbedCode,record.startPrCode,record.startDistanceM.toString()),
            com.tuempresa.inventariovial.validation.LengthEndpoint(record.routeCode,record.roadbedCode,record.endPrCode.orEmpty(),record.endDistanceM?.toString().orEmpty()),record.surveyDirection ?: "INCREASING") else null
        require(!strict || reference!=KILOMETRES || result?.metres!=null){result?.warning ?: "Completa los extremos kilométricos."}
        values[REFERENCE]=reference;values["_linearMetres"]=result?.metres?.toString().orEmpty()
        db.inventoryDao().saveSicExtension(SicExtensionEntity(record.id,old?.formatCode ?: record.sicCode,
            JSONObject(values as Map<*,*>).toString(),old?.sourceRecordId,old?.ownerUserId.orEmpty(),old?.status ?: record.status))
    }
    suspend fun remember(db:InventoryDatabase,record:InventoryRecordEntity,mode:String) {
        require(mode in setOf("AUTO","MEASURED","HISTORICAL"))
        val old=db.inventoryDao().sicExtension(record.id)
        require(old==null || old.formatCode=="SIC-20") {"Extensión incompatible con longitud del muro."}
        db.inventoryDao().saveSicExtension(SicExtensionEntity(record.id,"SIC-20",
            JSONObject(old?.values().orEmpty()+("_lengthMode" to mode)).toString(),null,old?.ownerUserId.orEmpty(),"COMPLETE"))
    }
    suspend fun refresh(db:InventoryDatabase,record:InventoryRecordEntity,prs:List<RoadPr>) {
        val dao=db.inventoryDao()
        rememberReference(db,record,dao.sicExtension(record.id)?.values()?.get(REFERENCE))
        if(dao.sicExtension(record.id)?.values()?.get("_lengthMode")!="AUTO") return
        val detail=dao.snapshot(record.id)?.sic20 ?: return
        if(detail.classCode=="14") dao.updateSic20(detail.copy(wallLengthMeters=ContractualLength.calculate(record,prs).metres))
    }
}
