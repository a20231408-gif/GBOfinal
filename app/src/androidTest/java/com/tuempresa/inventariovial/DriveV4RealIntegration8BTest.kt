package com.tuempresa.inventariovial

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.model.form.SicFormDetail
import com.tuempresa.inventariovial.repository.EngineeringEdits
import com.tuempresa.inventariovial.repository.InventoryRepository
import com.tuempresa.inventariovial.sic.*
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Explicit opt-in real transport; separate databases and fresh UUIDs, never field records.
 * Credentials come only from the installed app's local build configuration.
 * Evidence deliberately excludes tokens, endpoint URLs and serialized request bodies.
 */
class DriveV4RealIntegration8BTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val evidence=JSONObject().put("runtime","Android instrumentation").put("mockBackend",false)
    private val events=JSONArray()
    private val evidenceFile get()=File(context.getExternalFilesDir(null),
        if(evidence.has("runId"))"8b-evidence.json" else "8b-capabilities-auth.json")
    private fun digest(bytes:ByteArray)=java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString(""){"%02x".format(it)}
    private fun persist() {evidence.put("events",events);evidenceFile.writeText(evidence.toString(2))}
    private fun event(name:String,result:String,detail:JSONObject=JSONObject()) {
        events.put(JSONObject().put("case",name).put("result",result).put("detail",detail));persist()
    }
    private fun optIn() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("gboDrive8B")=="true")
        require(DriveUploadPolicy.configurationError()==null) {"Private Drive configuration required"}
    }
    private fun capabilities() {
        val client=OkHttpClient()
        val caps=client.newCall(Request.Builder().url(DriveConfig.WEB_APP_URL).build()).execute().use {
            require(it.isSuccessful) {"Capability HTTP failure"};JSONObject(it.body!!.string())
        }
        require(caps.optString("rootFolderId")=="1FQBVRD3BK1rOBfC8NdRzPQt3fvU40V23") {"Unexpected Drive root; no test writes allowed"}
        require(caps.optBoolean("ok") && caps.optInt("syncProtocol")==3 && caps.optInt("pathVersion")==4 &&
            caps.optBoolean("stableFamilyNumbering") && caps.optInt("photoCategoryVersion")==1 &&
            caps.optString("photoPolicy")=="ORIGINAL_CLEAN" && caps.optBoolean("strictElementFoldering") &&
            caps.optBoolean("technicalDeliveryWithoutGps")) {"Incompatible live capabilities"}
        event("capabilities","APROBADO REAL",caps.apply {remove("message")})
    }
    private class ObservedTransport(private val delegate:AutomaticDriveTransport=AutomaticDriveHttp()):AutomaticDriveTransport {
        var loseNextAck=false
        val receipts=mutableListOf<JSONObject>()
        override fun supportsV3()=delegate.supportsV3()
        override fun supportsFamilyPaths()=delegate.supportsFamilyPaths()
        override fun supportsTechnicalDeliveryWithoutGps()=delegate.supportsTechnicalDeliveryWithoutGps()
        override fun post(payload:JSONObject):JSONObject {
            val ack=delegate.post(payload)
            receipts+=JSONObject(ack.toString())
            if(loseNextAck && ack.optBoolean("ok")) {loseNextAck=false;throw IOException("Controlled loss after real server ACK")}
            return ack
        }
    }
    private fun freshDb(name:String)=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
    private suspend fun saveCulvert(db:InventoryDatabase,id:String,operator:String,offset:Double) {
        val time=System.currentTimeMillis()
        val r=InventoryRecordEntity(id,"SIC-18","ALCANTARILLA","PE-22A","CD","0010",offset,
            null,null,"S",null,null,null,null,LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            "PRUEBA 8B $operator $id · NO INVENTARIO DE CAMPO","ACTIVE",
            "PENDING","PENDING",time,time,surveyDirection="INCREASING")
        val photos=listOf("CULVERT_INLET","CULVERT_OUTLET").mapIndexed {index,category->
            val photoId=UUID.randomUUID().toString();val image=File(context.filesDir,"8b-$photoId.jpg")
            val bitmap=Bitmap.createBitmap(320,240,Bitmap.Config.ARGB_8888)
            val canvas=Canvas(bitmap);canvas.drawColor(if(index==0)Color.CYAN else Color.YELLOW)
            canvas.drawText("PRUEBA 8B $operator",10f,80f,Paint().apply {color=Color.BLACK;textSize=22f})
            canvas.drawText(category,10f,120f,Paint().apply {color=Color.BLACK;textSize=20f})
            image.outputStream().use {check(bitmap.compress(Bitmap.CompressFormat.JPEG,95,it))};bitmap.recycle()
            PhotoEntity(photoId,id,index+1,image.path,index==0,null,null,null,"PENDING",time,photoCategory=category)
        }
        InventoryRepository(db).saveSic18(r,Sic18Entity(id,"06","1",1,"1",1.0,1.0,"1","1"),photos)
    }
    private suspend fun drain(db:InventoryDatabase,transport:AutomaticDriveTransport):Boolean=
        AutomaticSyncDrain.run(context,db,{transport},null)
    private suspend fun receipt(db:InventoryDatabase,id:String):JSONObject {
        val intent=db.automaticSyncDao().get("SIC/$id")!!
        val detail=JSONObject().put("recordId",id).put("revision",intent.revision)
            .put("syncedRevision",intent.syncedRevision).put("state",intent.state)
            .put("error",intent.error ?: JSONObject.NULL)
        intent.receiptJson?.let {detail.put("ack",JSONObject(it))}
        // Save the failed state before assertion so a rejection cannot be reported as success.
        event("delivery-$id",if(SyncIntentPresentation.confirmed(intent))"APROBADO REAL" else "FALLIDO",detail)
        assertTrue("Real delivery not confirmed; inspect sanitized 8b-evidence.json",SyncIntentPresentation.confirmed(intent))
        val p=JSONObject(intent.preparedJson!!)
        val photos=JSONArray()
        for(photo in db.inventoryDao().snapshot(id)!!.photos) {
            photos.put(JSONObject().put("photoId",photo.id).put("category",photo.photoCategory)
                .put("order",photo.photoIndex).put("fileId",photo.driveFileId)
                .put("originalSha256",digest(File(photo.originalPath).readBytes()))
                .put("originalBytes",File(photo.originalPath).length()))
        }
        detail.put("photos",photos).put("baseRevision",p.getLong("baseRevision"))
        persist()
        return JSONObject(intent.receiptJson!!)
    }
    private suspend fun edit(db:InventoryDatabase,id:String,condition:String) {
        val s=db.inventoryDao().snapshot(id)!!
        val form=EngineeringEdits.form(s) as SicFormDetail.Sic18
        EngineeringEdits.save(db,id,form.copy(state=form.state.copy(structuralConditionCode=condition)),
            s.photos.map {it.localPath},s.photos.associate {it.localPath to it.photoCategory!!},expectedUpdatedAt=s.record.updatedAt)
    }

    @Test fun realSicPhotosEditTwoStoresRetryConflictAndOfflinePersistence()=runBlocking {
        optIn();capabilities()
        val runId=UUID.randomUUID().toString();evidence.put("runId",runId);persist()
        val nameA="8b-A-$runId.db";val nameB="8b-B-$runId.db"
        var dbA=freshDb(nameA);val dbB=freshDb(nameB)
        val idA=UUID.randomUUID().toString();val idB=UUID.randomUUID().toString();val idOther=UUID.randomUUID().toString()
        evidence.put("records",JSONObject().put("operatorA",idA).put("operatorB",idB).put("otherFormat",idOther));persist()
        val transport=ObservedTransport()
        val other=SicFieldState(SicFieldFormat.SIC08,mapOf("routeCode" to "PE-22A","roadbedCode" to "CD","faja" to "2",
            "startPrCode" to "0010","startDistanceM" to "34","endPrCode" to "0011","endDistanceM" to "20",
            "surveyDate" to LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),"typeCode" to "1","directionCode" to "C",
            "observations" to "PRUEBA 8B OTRO FORMATO $idOther"))
        try {
            SicFieldRepository(dbA).save(idOther,other.set("endPrCode",""),"PRUEBA OPERADOR A",false)
            assertNull(dbA.automaticSyncDao().get("SIC/$idOther"))
            event("draft","APROBADO LOCAL",JSONObject().put("recordId",idOther).put("posts",0))
            saveCulvert(dbA,idA,"OPERADOR A",12.0)
            assertEquals("QUEUED",dbA.automaticSyncDao().get("SIC/$idA")!!.state)
            event("automatic-outbox","APROBADO LOCAL",JSONObject().put("recordId",idA))
            drain(dbA,transport);val ackA=receipt(dbA,idA)
            saveCulvert(dbB,idB,"OPERADOR B",23.0);drain(dbB,transport);receipt(dbB,idB)
            edit(dbA,idA,"2");assertEquals("UPDATE_PENDING",dbA.automaticSyncDao().get("SIC/$idA")!!.state)
            drain(dbA,transport);val editedAck=receipt(dbA,idA)
            assertEquals(ackA.getString("folderId"),editedAck.getString("folderId"))
            event("edit-same-identity","APROBADO REAL",JSONObject().put("recordId",idA).put("priorRevision",ackA.getLong("revision")).put("revision",editedAck.getLong("revision")))
            // Replay an actual confirmed snapshot after losing its returned ACK in this client.
            edit(dbB,idB,"2");transport.loseNextAck=true
            assertTrue(drain(dbB,transport));assertEquals("ERROR",dbB.automaticSyncDao().get("SIC/$idB")!!.state)
            val lost=transport.receipts.last();drain(dbB,transport);val replay=receipt(dbB,idB)
            assertEquals(lost.getString("folderId"),replay.getString("folderId"))
            assertEquals(lost.getLong("revision"),replay.getLong("revision"))
            event("lost-real-ack-retry","APROBADO REAL",JSONObject().put("controlledClientLoss",true).put("ack",replay))
            // A second, independent local store has the actual previously confirmed base.
            val original=dbA.inventoryDao().snapshot(idA)!!
            dbB.inventoryDao().insertRecord(original.record);dbB.inventoryDao().insertSic18(original.sic18!!)
            dbB.inventoryDao().insertPhotos(original.photos)
            dbB.automaticSyncDao().put(dbA.automaticSyncDao().get("SIC/$idA")!!)
            edit(dbA,idA,"1");drain(dbA,transport);receipt(dbA,idA)
            edit(dbB,idA,"3");drain(dbB,transport)
            val conflict=dbB.automaticSyncDao().get("SIC/$idA")!!
            event("concurrent-real-conflict",if(conflict.state=="CONFLICT")"APROBADO REAL" else "FALLIDO",
                JSONObject().put("recordId",idA).put("state",conflict.state).put("error",conflict.error).put("serverReply",transport.receipts.last()))
            assertEquals("CONFLICT",conflict.state);assertEquals("3",dbB.inventoryDao().snapshot(idA)!!.sic18!!.structuralConditionCode)
            // Another format remains queued locally across a real SQLite close/reopen.
            SicFieldRepository(dbA).save(idOther,other,"PRUEBA OPERADOR A",true)
            val offline=dbA.automaticSyncDao().get("SIC/$idOther")!!
            dbA.close();dbA=freshDb(nameA)
            assertEquals(offline,dbA.automaticSyncDao().get(offline.key))
            event("offline-room-reopen","APROBADO LOCAL",JSONObject().put("recordId",idOther).put("networkDisabled",false).put("note","No drain before reopen; physical airplane mode pending"))
            drain(dbA,transport);receipt(dbA,idOther)
            evidence.put("completed",true);persist()
        }catch(failure:Throwable) {
            evidence.put("completed",false).put("failureClass",failure.javaClass.simpleName);persist()
            throw AssertionError("Real 8B case did not complete; inspect sanitized evidence. No automatic repeated run.")
        }finally{dbA.close();dbB.close()}
    }

    @Test fun missingAndIncorrectTokenAreRejectedByRealEndpoint() {
        optIn();capabilities()
        val client=OkHttpClient()
        for(label in listOf("missing","incorrect")) {
            val p=JSONObject().put("action","stageRecordV3").put("syncProtocol",3)
            if(label=="incorrect")p.put("token",UUID.randomUUID().toString())
            val response=client.newCall(Request.Builder().url(DriveConfig.WEB_APP_URL)
                .post(p.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()).execute().use {
                JSONObject(it.body!!.string())
            }
            val rejected=!response.optBoolean("ok") && response.optString("error")=="No autorizado"
            event("token-$label",if(rejected)"APROBADO REAL" else "FALLIDO",JSONObject().put("accepted",response.optBoolean("ok")))
            assertTrue("Expected live authorization rejection",rejected)
        }
    }
}
