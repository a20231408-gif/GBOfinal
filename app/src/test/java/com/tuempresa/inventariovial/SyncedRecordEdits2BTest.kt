package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.repository.EngineeringEdits
import com.tuempresa.inventariovial.delivery.RouteDelivery
import com.tuempresa.inventariovial.road.*
import kotlinx.coroutines.*
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SyncedRecordEdits2BTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    private suspend fun save(d:InventoryDatabase,id:String) {
        val r=testRecord(id,pr=if(id.endsWith("-b"))"2" else "10",route="PE-22A",sic="SIC-18").copy(assetType="",latitude=null,longitude=null,updatedAt=1000,axisMeasureM=null,surveyDirection="INCREASING")
        d.inventoryDao().insertRecord(r)
        d.inventoryDao().insertSic18(Sic18Entity(id,"06","1",1,"1",1.0,1.0,"1","1"))
        val f=photo("$id-original")
        d.inventoryDao().insertPhoto(PhotoEntity("photo-$id",id,1,f.path,true,null,null,null,"PENDING",1,photoCategory="CULVERT_INLET"))
        AutomaticSync.request(d,"SIC",id,r.updatedAt)
    }
    private fun photo(id:String):File {
        val f=File(context.cacheDir,"$id.jpg");val b=Bitmap.createBitmap(32,24,Bitmap.Config.ARGB_8888)
        f.outputStream().use {assertTrue(b.compress(Bitmap.CompressFormat.JPEG,95,it))};b.recycle();return f
    }
    private suspend fun edit(d:InventoryDatabase,id:String,condition:String="2",paths:List<String>?=null,categories:Map<String,String>?=null) {
        val s=d.inventoryDao().snapshot(id)!!;val form=EngineeringEdits.form(s) as SicFormDetail.Sic18
        EngineeringEdits.save(d,id,form.copy(state=form.state.copy(structuralConditionCode=condition)),paths ?: s.photos.map {it.localPath},
            categories ?: s.photos.associate {it.localPath to it.photoCategory!!},expectedUpdatedAt=s.record.updatedAt)
    }
    private class Backend:AutomaticDriveTransport {
        val records=mutableMapOf<String,JSONObject>();val receipts=mutableMapOf<String,JSONObject>()
        var loseAck=false;var invalidAck=false;var corruptIdentity=false;var posts=0;var onCommit:((JSONObject)->Unit)?=null;var beforePost:(()->Unit)?=null
        override fun supportsV3()=true
        override fun supportsFamilyPaths()=true
        override fun supportsTechnicalDeliveryWithoutGps()=true
        override fun post(payload:JSONObject):JSONObject {
            posts++;beforePost?.let {beforePost=null;it()};val p=JSONObject(payload.toString());val key=p.getString("key");val old=records[key]
            if(old!=null && old.getLong("revision")==p.getLong("revision")) {
                if(old.getString("contentHash")!=p.getString("contentHash"))return rejection("REVISION_CONFLICT",old)
                return JSONObject(receipts.getValue(key).toString()).put("alreadyExists",true)
            }
            if(old!=null && p.getLong("revision")<old.getLong("revision"))return rejection("STALE_REVISION",old)
            if(p.getLong("baseRevision")!=(old?.getLong("revision") ?: 0L))return rejection("BASE_REVISION_CONFLICT",old)
            val path=receipts[key]?.getString("logicalPath") ?: "PE-22A/ALCANTARILLAS/ALCANTARILLA ${records.size+1}"
            val files=JSONArray();val src=p.getJSONArray("files")
            for(n in 0 until src.length()) {val f=src.getJSONObject(n);files.put(JSONObject().put("key",f.getString("key")).put("sha256",f.getString("sha256"))
                .put("fileId","file-$key-${f.getString("key")}").put("folderId","folder-$key").put("requestedPath",f.getString("path")).put("logicalPath",path+"/"+f.getString("path")))}
            val sic=JSONArray();val rows=p.getJSONArray("sicRows")
            for(n in 0 until rows.length()) {val row=rows.getJSONObject(n);sic.put(JSONObject().put("format",row.getString("format"))
                .put("fileId","excel-${row.getString("format")}").put("folderId","excel-folder").put("logicalPath","PE-22A/EXCEL_SIC/${row.getString("format")}.xlsx")
                .put("sha256",row.getString("sha256")).put("recordIds",JSONArray().put(row.getString("recordId"))))}
            val ack=JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",4).put("key",key).put("revision",p.getLong("revision"))
                .put("contentHash",p.getString("contentHash")).put("folderId","folder-$key").put("logicalPath",path).put("files",files).put("sicFiles",sic)
                .put("technicalDeliveryStatus","DELIVERED").put("georeferencingStatus","PENDING_EXTERNAL").put("externalGeoreferencingStatus","PENDING")
            records[key]=p;receipts[key]=ack
            onCommit?.let {onCommit=null;it(p)}
            if(loseAck){loseAck=false;throw IOException("synthetic lost response")}
            return if(invalidAck)JSONObject(ack.toString()).put("folderId","") else if(corruptIdentity)JSONObject(ack.toString()).put("folderId","another-folder") else ack
        }
        private fun rejection(code:String,old:JSONObject?)=JSONObject().put("ok",false).put("code",code).put("retryable",false).put("remoteRevision",old?.getLong("revision") ?: 0)
    }
    private suspend fun drain(d:InventoryDatabase,b:Backend)=AutomaticSyncDrain.run(context,d,{b},configurationError=null)
    private suspend fun fixture(d:InventoryDatabase,id:String,name:String) {
        val i=d.automaticSyncDao().get("SIC/$id")!!;val p=JSONObject(AutomaticDelivery.prepare(context,d,i).toString())
        val files=p.getJSONArray("files")
        for(n in 0 until files.length()) {val f=files.getJSONObject(n);f.put("base64",android.util.Base64.encodeToString(File(f.getString("localFile")).readBytes(),android.util.Base64.NO_WRAP));f.remove("localFile")}
        val out=File("build/outputs/cierre-1.4.5/edicion-sincronizados/examples/$name.json");out.parentFile!!.mkdirs();out.writeText(p.toString(2))
    }
    @Test fun conditionAndProgressiveEditsKeepIdentityConfirmedBaseAndGenerateActualPayloads()=runBlocking(Dispatchers.IO) {db().use {d->
        val id="edit-culvert-a";val b=Backend();save(d,id);fixture(d,id,"initial-a");drain(d,b)
        val ack=d.automaticSyncDao().get("SIC/$id")!!.receiptJson;val original=d.inventoryDao().snapshot(id)!!.photos.single();val bytes=File(original.localPath).readBytes()
        edit(d,id);val pending=d.automaticSyncDao().get("SIC/$id")!!
        assertEquals("UPDATE_PENDING",pending.state);assertEquals(1000L,pending.syncedRevision);assertEquals(ack,pending.receiptJson)
        assertEquals("Actualización pendiente · sin conexión",SyncIntentPresentation.label(pending,true,false,true))
        assertEquals("PENDING",d.inventoryDao().recordById(id)!!.excelSyncStatus);fixture(d,id,"condition-a");drain(d,b)
        assertEquals("SYNCED",d.automaticSyncDao().get("SIC/$id")!!.state);assertNull(d.inventoryDao().recordById(id)!!.latitude)
        val firstFolder=JSONObject(ack!!).getString("folderId");assertEquals(firstFolder,JSONObject(d.automaticSyncDao().get("SIC/$id")!!.receiptJson!!).getString("folderId"))
        save(d,"edit-culvert-b");fixture(d,"edit-culvert-b","initial-b");drain(d,b)
        val r=d.inventoryDao().recordById(id)!!;d.inventoryDao().updateCoreFields(id,r.routeCode,r.roadbedCode,"1",0.0,r.endPrCode,r.endDistanceM,r.sideCode,r.observations,r.updatedAt+1)
        AutomaticSync.request(d,"SIC",id,d.inventoryDao().recordById(id)!!.updatedAt);fixture(d,id,"progressive-a");drain(d,b)
        val ordered=RoadOrdering.orderAssetsForRoute(d.inventoryDao().activeSnapshots().map(RouteDelivery::row));assertEquals(id,ordered.first().recordId)
        assertEquals(firstFolder,JSONObject(d.automaticSyncDao().get("SIC/$id")!!.receiptJson!!).getString("folderId"));assertArrayEquals(bytes,File(original.localPath).readBytes())
    }}
    @Test fun editDuringUploadRetainsLateAckAsBaseAndThenDeliversNewerRevision()=runBlocking(Dispatchers.IO) {db().use {d->
        val id="late-edit";save(d,id);val b=Backend();b.onCommit={runBlocking {edit(d,id)}};drain(d,b)
        val i=d.automaticSyncDao().get("SIC/$id")!!;assertEquals("UPDATE_PENDING",i.state);assertEquals(1000L,i.syncedRevision);assertTrue(i.revision>i.syncedRevision)
        drain(d,b);assertEquals("SYNCED",d.automaticSyncDao().get("SIC/$id")!!.state);assertEquals(2,b.posts)
    }}
    @Test fun lostAckThenEditReplaysExactPredecessorWithoutConflictingWithOwnDelivery()=runBlocking(Dispatchers.IO) {db().use {d->
        val id="lost-edit";save(d,id);val b=Backend();b.loseAck=true;assertTrue(drain(d,b));edit(d,id)
        val i=d.automaticSyncDao().get("SIC/$id")!!;assertEquals(1000L,JSONObject(i.preparedJson!!).getLong("revision"))
        assertFalse(drain(d,b));assertEquals("SYNCED",d.automaticSyncDao().get("SIC/$id")!!.state);assertEquals(3,b.posts);assertEquals(1,b.records.size)
    }}
    @Test fun operatorConflictPersistsLocalEditAndCannotBeClearedByAnotherSave()=runBlocking(Dispatchers.IO) {db().use {d->
        val id="conflict-edit";save(d,id);val b=Backend();drain(d,b);edit(d,id)
        val remote=JSONObject(b.records.getValue("SIC/$id").toString()).put("revision",d.automaticSyncDao().get("SIC/$id")!!.revision+10)
        b.records["SIC/$id"]=remote;assertFalse(drain(d,b));val i=d.automaticSyncDao().get("SIC/$id")!!
        assertEquals("CONFLICT",i.state);assertTrue(i.error!!.contains(remote.getLong("revision").toString()));assertEquals("2",d.inventoryDao().snapshot(id)!!.sic18!!.structuralConditionCode)
        edit(d,id,condition="1");assertEquals("CONFLICT",d.automaticSyncDao().get("SIC/$id")!!.state);val count=b.posts;drain(d,b);assertEquals(count,b.posts)
        assertEquals("Conflicto · edición local conservada",SyncIntentPresentation.label(i,true,true,true))
    }}
    @Test fun photoCategoryAddAndRemovePreserveOriginalFilesAndExistingPhotoUuid()=runBlocking(Dispatchers.IO) {db().use {d->
        val id="photos-edit";save(d,id);val b=Backend();drain(d,b);val old=d.inventoryDao().snapshot(id)!!.photos.single();val bytes=File(old.localPath).readBytes();val added=photo("new-photo")
        edit(d,id,paths=listOf(old.localPath,added.path),categories=mapOf(old.localPath to "CULVERT_OUTLET",added.path to "CULVERT_INLET"))
        assertEquals("PENDING",d.inventoryDao().photoById(old.id)!!.syncStatus);fixture(d,id,"photos-added");drain(d,b)
        assertEquals(old.id,d.inventoryDao().snapshot(id)!!.photos.single {it.localPath==old.localPath}.id)
        edit(d,id,paths=listOf(added.path),categories=mapOf(added.path to "CULVERT_INLET"));fixture(d,id,"photos-removed");drain(d,b)
        assertArrayEquals(bytes,File(old.localPath).readBytes());assertTrue(added.exists());assertEquals(1,d.inventoryDao().snapshot(id)!!.photos.size)
    }}
    @Test fun staleLocalFormCannotOverwriteNewerConditionOrPhotos()=runBlocking(Dispatchers.IO) {db().use {d->
        val id="stale-form";save(d,id);val before=d.inventoryDao().snapshot(id)!!;edit(d,id)
        try {EngineeringEdits.save(d,id,EngineeringEdits.form(before),emptyList(),emptyMap(),expectedUpdatedAt=before.record.updatedAt);fail("Must reject stale form")}catch(_:IllegalArgumentException){}
        assertEquals("2",d.inventoryDao().snapshot(id)!!.sic18!!.structuralConditionCode);assertEquals(1,d.inventoryDao().snapshot(id)!!.photos.size)
    }}
    @Test fun missingBackendConfirmationCannotMarkAnEditSynced()=runBlocking(Dispatchers.IO) {db().use {d->
        val id="invalid-ack-edit";save(d,id);val b=Backend();drain(d,b);edit(d,id);b.invalidAck=true;assertTrue(drain(d,b))
        val i=d.automaticSyncDao().get("SIC/$id")!!;assertEquals("ERROR",i.state);assertEquals(1000L,i.syncedRevision);assertEquals("2",d.inventoryDao().snapshot(id)!!.sic18!!.structuralConditionCode)
    }}
    @Test fun pendingEditAndConfirmedIdentitySurviveDatabaseReopening()=runBlocking(Dispatchers.IO) {
        val name="edit-persistence-${System.nanoTime()}";val b=Backend();var revision=0L
        fun open()=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        try {open().use {d->save(d,"persist-edit");drain(d,b);edit(d,"persist-edit");revision=d.automaticSyncDao().get("SIC/persist-edit")!!.revision}
            open().use {d->val i=d.automaticSyncDao().get("SIC/persist-edit")!!;assertEquals("UPDATE_PENDING",i.state);assertEquals(revision,i.revision);assertTrue(i.receiptJson!!.contains("ALCANTARILLA 1"));drain(d,b);assertEquals("SYNCED",d.automaticSyncDao().get(i.key)!!.state)}
        }finally {context.deleteDatabase(name)}
    }
    @Test fun nonblankButChangedFolderIdentityCannotAcknowledgeAnUpdate()=runBlocking(Dispatchers.IO) {db().use {d->
        val id="wrong-identity";save(d,id);val b=Backend();drain(d,b);val before=d.automaticSyncDao().get("SIC/$id")!!.receiptJson
        edit(d,id);b.corruptIdentity=true;assertTrue(drain(d,b))
        val pending=d.automaticSyncDao().get("SIC/$id")!!;assertEquals("ERROR",pending.state);assertEquals(before,pending.receiptJson);assertEquals(1000L,pending.syncedRevision)
    }}

    @Test fun rejectionDuringAnotherSaveKeepsLatestLocalRevisionInConflict()=runBlocking(Dispatchers.IO) {db().use {d->
        val id="conflict-during-save";save(d,id);val b=Backend();drain(d,b);edit(d,id)
        val attempted=d.automaticSyncDao().get("SIC/$id")!!.revision
        b.records["SIC/$id"]=JSONObject(b.records.getValue("SIC/$id").toString()).put("revision",attempted+10)
        b.beforePost={runBlocking {edit(d,id,condition="1")}}
        drain(d,b);val latest=d.automaticSyncDao().get("SIC/$id")!!
        assertTrue(latest.revision>attempted);assertEquals("CONFLICT",latest.state);assertEquals("1",d.inventoryDao().snapshot(id)!!.sic18!!.structuralConditionCode)
    }}

}
