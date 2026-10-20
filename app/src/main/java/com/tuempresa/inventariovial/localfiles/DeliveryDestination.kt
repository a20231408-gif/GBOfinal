package com.tuempresa.inventariovial.localfiles

import androidx.room.withTransaction
import com.tuempresa.inventariovial.auth.AuthSession
import com.tuempresa.inventariovial.auth.UserRole
import com.tuempresa.inventariovial.core.AutomaticSync
import com.tuempresa.inventariovial.data.database.InventoryDatabase

/** Additive per-record choice. Absence means the historical Drive behaviour. */
object DeliveryDestination {
    suspend fun deviceOnly(db:InventoryDatabase,key:String)=db.localFilesDao().choice(key)?.destination=="DEVICE"
    suspend fun revision(db:InventoryDatabase,kind:String,id:String):Long?=when(kind){
        "SIC"->db.inventoryDao().recordById(id)?.updatedAt
        "SCAP"->db.scapDao().inspection(id)?.updatedAt
        "PAVEMENT"->db.pavementDao().sample(id)?.updatedAt
        else->null
    }
    suspend fun closed(db:InventoryDatabase,kind:String,id:String)=when(kind){
        "SIC"->db.inventoryDao().recordById(id)?.status=="ACTIVE"
        "SCAP"->db.scapDao().inspection(id)?.status=="COMPLETE"
        "PAVEMENT"->db.pavementDao().sample(id)?.status=="CLOSED"
        else->false
    }
    private suspend fun draft(db:InventoryDatabase,kind:String,id:String)=when(kind){
        "SIC"->db.inventoryDao().recordById(id)?.status.let {it==null||it=="DRAFT"}
        "SCAP"->db.scapDao().inspection(id)?.status.let {it==null||it=="DRAFT"||it=="IN_PROGRESS"}
        "PAVEMENT"->db.pavementDao().sample(id)?.status.let {it==null||it=="DRAFT"}
        else->false
    }
    suspend fun choose(db:InventoryDatabase,kind:String,id:String,onlyDevice:Boolean,user:AuthSession)=db.withTransaction {
        require(!user.locked&&!user.closing)
        require(kind in setOf("SIC","SCAP","PAVEMENT")&&id.matches(Regex("[A-Za-z0-9_-]{1,80}")))
        val key="$kind/$id";val prior=db.localFilesDao().choice(key)
        require(prior==null||prior.selectedBy==user.id||user.role==UserRole.ADMIN){"Conserva el destino elegido por el responsable de la ficha."}
        require(draft(db,kind,id)&&db.automaticSyncDao().get(key)==null){"El destino solo se elige antes del primer cierre/envío. Las entregas existentes se conservan."}
        db.localFilesDao().saveChoice(DeviceDeliveryChoice(key,if(onlyDevice)"DEVICE"else"DRIVE",user.id,System.currentTimeMillis()))
    }
    /** Explicit owner/administrator authorization; never used by an automatic retry. */
    suspend fun authorizeDrive(db:InventoryDatabase,kind:String,id:String,user:AuthSession)=db.withTransaction {
        require(user.role==UserRole.ADMIN&&!user.locked&&!user.closing){"El administrador debe autorizar el envío posterior a Drive."}
        val key="$kind/$id";require(deviceOnly(db,key));require(closed(db,kind,id)){"Finaliza primero la ficha."}
        require(db.automaticSyncDao().get(key)==null){"Hay una entrega existente; no se cambia ni cancela."}
        db.localFilesDao().saveChoice(DeviceDeliveryChoice(key,"DRIVE",user.id,System.currentTimeMillis()))
        AutomaticSync.request(db,kind,id,requireNotNull(revision(db,kind,id)),origin="MANUAL_APP")
    }
}
