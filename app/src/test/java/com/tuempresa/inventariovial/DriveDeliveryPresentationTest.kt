package com.tuempresa.inventariovial

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapRepository
import com.tuempresa.inventariovial.scap.domain.ScapBridgeType
import kotlinx.coroutines.runBlocking
import org.json.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

internal fun deliveryFixture():DriveSyncIntent {
    val key="SIC/FICTICIO_DRIVE";val path="PE-28H/PUENTES/PUENTE 1"
    val files=JSONArray().put(JSONObject().put("key","photo/ficticia").put("path","GENERAL/F01.jpg").put("mimeType","image/jpeg").put("sha256","a".repeat(64)))
    val p=JSONObject().put("kind","SIC").put("key",key).put("revision",2L).put("routeCode","PE-28H").put("familyPath","PUENTES").put("family","PUENTE")
        .put("contentHash","b".repeat(64)).put("files",files).put("sicRows",JSONArray())
    val ack=JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",4).put("key",key).put("revision",2L).put("contentHash","b".repeat(64))
        .put("folderId","FICTICIO_FOLDER").put("logicalPath",path).put("sicFiles",JSONArray())
        .put("files",JSONArray().put(JSONObject().put("key","photo/ficticia").put("sha256","a".repeat(64)).put("fileId","FICTICIO_FILE").put("folderId","FICTICIO_FOLDER").put("requestedPath","GENERAL/F01.jpg").put("logicalPath","$path/GENERAL/F01.jpg")))
    return DriveSyncIntent(key,"SIC","FICTICIO_DRIVE",2,2,"SYNCED",lastSyncedAt=1,receiptJson=ack.toString(),preparedJson=p.toString())
}

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class DriveDeliveryPresentationTest {
    private val identity=DriveDeliveryIdentity("Puente 001 - Correviento","PE-28H",2,true)
    private fun view(i:DriveSyncIntent,running:Boolean=false,connected:Boolean=true,retrying:Boolean=false,p:SyncProgress?=null)=
        DriveDeliveryPresentation.view(i,identity,running,connected,true,retrying,p)
    @Test fun validCurrentReceiptIsTheOnlyCompleteDelivery() {
        val i=deliveryFixture();val v=view(i)
        assertTrue(v.confirmed);assertEquals(DriveDeliveryStatus.COMPLETE,v.status);assertEquals("Archivos confirmados: 1 de 1",v.files)
        assertFalse(view(i.copy(state="UPLOADING",syncedRevision=0)).confirmed)
        assertFalse(view(i.copy(preparedJson=null)).confirmed)
    }
    @Test fun malformedOrIncompleteReceiptsNeverTurnGreen() {
        val i=deliveryFixture()
        val mutations=listOf<(JSONObject)->Unit>(
            {it.put("key","SIC/OTHER")},{it.put("revision",1)},{it.put("contentHash","c".repeat(64))},
            {it.put("folderId","")},{it.put("files",JSONArray())},
            {it.getJSONArray("files").getJSONObject(0).remove("fileId")},
            {it.getJSONArray("files").getJSONObject(0).put("sha256","d".repeat(64))},
            {it.put("logicalPath","OTRA/RUTA")})
        for(change in mutations){val ack=JSONObject(i.receiptJson!!);change(ack);assertEquals(DriveDeliveryStatus.VERIFYING,view(i.copy(receiptJson=ack.toString())).status)}
        assertEquals(DriveDeliveryStatus.VERIFYING,view(i.copy(receiptJson="invalid")).status)
    }
    @Test fun editedRecordNeverReusesEarlierReceiptOrFileCounts() {
        val i=deliveryFixture();val latest=identity.copy(currentRevision=3)
        assertFalse(DriveDeliveryPresentation.view(i,latest,false,true,true).confirmed)
        assertEquals(DriveDeliveryStatus.PENDING,DriveDeliveryPresentation.view(i,latest,false,true,true).status)
        assertFalse(view(i.copy(revision=3)).confirmed)
    }
    @Test fun offlineQueueAndBusyArePendingRatherThanPermanentErrors() {
        val i=deliveryFixture().copy(state="QUEUED",syncedRevision=0)
        assertTrue(view(i,connected=false).message.contains("Internet"))
        for(state in listOf("ERROR","BLOCKED"))assertEquals(DriveDeliveryStatus.PENDING,view(i.copy(state=state,error="Servidor ocupado (BUSY). Esperando para reintentar.")).status)
        assertEquals(DriveDeliveryStatus.PENDING,view(i.copy(state="ERROR"),retrying=true).status)
    }
    @Test fun activeWorkAndConfirmationAreDifferentAndNeverInventPartialAck() {
        val i=deliveryFixture().copy(state="UPLOADING",syncedRevision=0)
        assertEquals(DriveDeliveryStatus.VERIFYING,view(i).status)
        assertEquals(DriveDeliveryStatus.UPLOADING,view(i,running=true,p=SyncProgress(2,"PHOTOS",3,6,1)).status)
        assertEquals(DriveDeliveryStatus.VERIFYING,view(i,running=true,p=SyncProgress(2,"CONFIRMING",6,6,1)).status)
        assertFalse(view(i,running=true,p=SyncProgress(2,"PHOTOS",3,6,1)).files.contains("3"))
        assertEquals(DriveDeliveryStatus.UPLOADING,view(i,running=true,p=SyncProgress(1,"CONFIRMING",6,6,1)).status)
    }
    @Test fun blockedConflictValidationAndConfigurationRequireAttention() {
        for(state in listOf("BLOCKED","CONFLICT","PENDING_VALIDATION","UNCONFIGURED","DRAFT"))
            assertEquals(DriveDeliveryStatus.ATTENTION,view(deliveryFixture().copy(state=state,syncedRevision=0)).status)
        assertEquals(DriveDeliveryStatus.ATTENTION,DriveDeliveryPresentation.view(deliveryFixture(),identity.copy(validated=false),false,true,true).status)
    }
    @Test fun fileListWithoutConsolidatedExcelReceiptCannotConfirm() {
        val i=deliveryFixture();val p=JSONObject(i.preparedJson!!)
        p.getJSONArray("sicRows").put(JSONObject().put("format","SIC-17").put("recordId",i.recordId))
        assertFalse(view(i.copy(preparedJson=p.toString())).confirmed)
    }
    @Test fun namesUseRealFieldsAndReadingDoesNotReserveFoldersOrWriteData()=runBlocking {
        val c=ApplicationProvider.getApplicationContext<Application>()
        Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use{db->
            val repo=ScapRepository(db,ScapCatalog.load(c))
            val bridges=(1..2).map{repo.create("FICTICIO","FICTICIO",null,ScapBridgeType.MODULAR).also{id->
                repo.setField(id,"inspection","singularityName","Correviento");repo.setField(id,"inspection","bridgeCode","001");repo.setField(id,"inspection","route","PE-28H")}}
            val records=listOf("MURO" to "Muro","BADEN" to "Badén","TUNEL" to "Túnel").map{(type,title)->
                val r=testRecord("FICTICIO_$type",sic="SIC-20",pr="0010",distance=60.0,route="PE-28C").copy(assetType=type)
                db.inventoryDao().insertRecord(r);r to title}
            val sample=visualSample().copy(tramo="Tramo 3",muestra=2);db.pavementDao().saveSample(sample)
            fun count(table:String)=db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use{it.moveToFirst();it.getLong(0)}
            val tables=listOf("device_delivery_choices","local_export_jobs","local_shared_files","drive_sync_outbox")
            val before=tables.associateWith{count(it)}
            for(id in bridges){val s=db.scapDao().snapshot(id)!!
                val display=DriveDeliveryPresentation.identity(db,DriveSyncIntent("SCAP/$id","SCAP",id,s.inspection.updatedAt))
                assertEquals("Puente 001 - Correviento",display.title);assertEquals("PE-28H",display.location);assertEquals(s,db.scapDao().snapshot(id))}
            for((r,title)in records){val display=DriveDeliveryPresentation.identity(db,DriveSyncIntent("SIC/${r.id}","SIC",r.id,r.updatedAt));assertEquals("$title - PE-28C - PR 10+060",display.title);assertEquals(r,db.inventoryDao().recordById(r.id))}
            val display=DriveDeliveryPresentation.identity(db,DriveSyncIntent("PAVEMENT/${sample.id}","PAVEMENT",sample.id,sample.updatedAt))
            assertEquals("Pavimento rígido - Tramo 3 - Muestra 2",display.title)
            repo.setField(bridges[0],"inspection","singularityName","");repo.setField(bridges[0],"inspection","bridgeCode","")
            assertTrue(DriveDeliveryPresentation.identity(db,DriveSyncIntent("SCAP/${bridges[0]}","SCAP",bridges[0],1)).title.startsWith("Puente - PE-28H"))
            assertEquals(before,tables.associateWith{count(it)})
        }
    }
}
