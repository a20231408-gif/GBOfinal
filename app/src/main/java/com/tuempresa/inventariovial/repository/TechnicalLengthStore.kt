package com.tuempresa.inventariovial.repository

import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.validation.ContractualLength
import com.tuempresa.inventariovial.road.RoadPr
import org.json.JSONObject

/** Reuses the existing extension table. Missing mode means historical measured length. */
object TechnicalLengthStore {
    suspend fun remember(db:InventoryDatabase,record:InventoryRecordEntity,mode:String) {
        require(mode in setOf("AUTO","MEASURED","HISTORICAL"))
        val old=db.inventoryDao().sicExtension(record.id)
        require(old==null || old.formatCode=="SIC-20") {"Extensión incompatible con longitud del muro."}
        db.inventoryDao().saveSicExtension(SicExtensionEntity(record.id,"SIC-20",
            JSONObject(old?.values().orEmpty()+("_lengthMode" to mode)).toString(),null,old?.ownerUserId.orEmpty(),"COMPLETE"))
    }
    suspend fun refresh(db:InventoryDatabase,record:InventoryRecordEntity,prs:List<RoadPr>) {
        val dao=db.inventoryDao()
        if(dao.sicExtension(record.id)?.values()?.get("_lengthMode")!="AUTO") return
        val detail=dao.snapshot(record.id)?.sic20 ?: return
        if(detail.classCode=="14") dao.updateSic20(detail.copy(wallLengthMeters=ContractualLength.calculate(record,prs).metres))
    }
}
