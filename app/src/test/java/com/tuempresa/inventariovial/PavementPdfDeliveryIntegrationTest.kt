package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.delivery.sha256
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.pavement.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import org.robolectric.shadows.ShadowStatFs
import java.io.File

/** Real local preparers, fictitious Room/files only. No HTTP client, Google or tablet. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,shadows=[SyntheticPdf146::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementPdfDeliveryIntegrationTest {
    @Test fun wrongPavementAckPathKeepsLocalEvidenceAndReplaysFrozenRevision()=runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        val c=ApplicationProvider.getApplicationContext<Context>()
        ShadowStatFs.registerStats(c.filesDir.path,1_000_000,900_000,900_000)
        val (raw,originalDamages)=pdfReadableFixture("pocos")
        val sample=raw.copy(id="FICTITIOUS_ACK_SAMPLE",status="CLOSED",updatedAt=456)
        val damages=originalDamages.map { it.copy(sampleId=sample.id) }
        Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use { db->
            db.pavementDao().saveSample(sample)
            damages.forEach { db.pavementDao().saveDamage(it) }
            val savedDamages=db.pavementDao().damages(sample.id)
            val bitmap=Bitmap.createBitmap(24,20,Bitmap.Config.ARGB_8888)
            val original=File(c.filesDir,"FICTITIOUS_ACK_PHOTO.jpg").apply {
                outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG,95,it) }
            }
            bitmap.recycle()
            db.pavementDao().savePhoto(PavementDamagePhotoEntity("FICTITIOUS_ACK_PHOTO",damages.first().id,original.path,456))
            val savedPhotos=db.pavementDao().photos(damages.first().id)
            val originalBytes=original.readBytes()
            val intent=DriveSyncIntent("PAVEMENT/${sample.id}","PAVEMENT",sample.id,456,0,"QUEUED")
            db.automaticSyncDao().put(intent)
            var posts=0
            var frozen:String?=null
            val transport=object:AutomaticDriveTransport {
                override fun supportsV3()=true
                override fun supportsFamilyPaths()=true
                override fun supportsReadablePhotoNames()=true
                override fun reservePhotoNames(payload:JSONObject)=ReleasePhotoV2Fixture.reservation(payload,"FICTITIOUS_SAMPLE_FOLDER",7)
                override fun post(payload:JSONObject):JSONObject {
                    if(frozen==null)frozen=payload.toString() else assertEquals(frozen,payload.toString())
                    posts++
                    val root="${payload.getString("routeCode")}/EVALUACION_SUPERFICIAL/${payload.getString("tramo")}/${payload.getString("surface")}/MUESTRA 7"
                    val prepared=payload.getJSONArray("files")
                    val files=JSONArray((0 until prepared.length()).map { n->
                        val file=prepared.getJSONObject(n)
                        val key=file.getString("key")
                        val path=file.getString("path")
                        val finalPath=when {
                            key=="field"->"HOJAS_CAMPO/HC_007.pdf"
                            key=="sample_excel"->"DATOS_APP/M_007.xlsx"
                            key=="sample_json"->"DATOS_APP/M_007.json"
                            key.startsWith("photo/")->"PANEL_FOTOGRAFICO/"+
                                (if(payload.optInt("photoNamingVersion")==2)"" else "M007_")+path.substringAfterLast('/')
                            else->path
                        }
                        JSONObject().put("key",key).put("sha256",file.getString("sha256"))
                            .put("fileId","FICTITIOUS_FILE_$n").put("folderId","FICTITIOUS_FOLDER_$n")
                            .put("requestedPath",path)
                            .put("logicalPath","$root/"+(if(posts==1 && key=="field")"DATOS_APP/HC_007.pdf" else finalPath))
                    })
                    return JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",4)
                        .put("key",payload.getString("key")).put("revision",payload.getLong("revision"))
                        .put("contentHash",payload.getString("contentHash")).put("folderId","FICTITIOUS_SAMPLE_FOLDER")
                        .put("logicalPath",root).put("files",files).put("sicFiles",JSONArray())
                }
            }
            assertTrue(AutomaticSyncDrain.run(c,db,{transport},null))
            val rejected=db.automaticSyncDao().get(intent.key)!!
            assertEquals(sample.id,rejected.recordId)
            assertEquals(intent.revision,rejected.revision)
            assertEquals("ERROR",rejected.state)
            assertEquals(0L,rejected.syncedRevision)
            assertNull(rejected.receiptJson)
            assertEquals(frozen,rejected.preparedJson)
            assertEquals(sample,db.pavementDao().sample(sample.id))
            assertEquals(savedDamages,db.pavementDao().damages(sample.id))
            assertEquals(savedPhotos,db.pavementDao().photos(damages.first().id))
            assertArrayEquals(originalBytes,original.readBytes())
            assertFalse(AutomaticSyncDrain.run(c,db,{transport},null))
            val confirmed=db.automaticSyncDao().get(intent.key)!!
            assertEquals(2,posts)
            assertEquals(sample.id,confirmed.recordId)
            assertEquals("SYNCED",confirmed.state)
            assertEquals(intent.revision,confirmed.syncedRevision)
            assertEquals(frozen,confirmed.preparedJson)
            assertEquals(sample,db.pavementDao().sample(sample.id))
            assertEquals(savedDamages,db.pavementDao().damages(sample.id))
            assertEquals(savedPhotos,db.pavementDao().photos(damages.first().id))
            assertArrayEquals(originalBytes,original.readBytes())
        }
    }

    @Test fun newPdfAndPhotoPrepareForDriveAndDownloadsWithStableHashesAndCachedRevision()=runBlocking {
        val c=ApplicationProvider.getApplicationContext<Context>()
        ShadowStatFs.registerStats(c.filesDir.path,1_000_000,900_000,900_000)
        val(raw,ds)=pdfReadableFixture("pocos");val s=raw.copy(status="CLOSED",updatedAt=123)
        Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use{db->
            db.pavementDao().saveSample(s);ds.forEach{db.pavementDao().saveDamage(it)}
            val bitmap=Bitmap.createBitmap(24,20,Bitmap.Config.ARGB_8888)
            val original=File(c.filesDir,"TEST_PDF_DELIVERY.jpg").apply{outputStream().use{bitmap.compress(Bitmap.CompressFormat.JPEG,95,it)}};bitmap.recycle()
            val hash=fileDigest(original)
            val photo=PavementDamagePhotoEntity("TEST_PDF_PHOTO",ds.first().id,original.path,123)
            db.pavementDao().savePhoto(photo)
            val intent=DriveSyncIntent("PAVEMENT/${s.id}","PAVEMENT",s.id,s.updatedAt,0,"QUEUED")
            db.automaticSyncDao().put(intent)
            val before=db.pavementDao().damages(s.id)
            val prepared=AutomaticDelivery.prepare(c,db,intent,readable=false)
            assertEquals(s.id,prepared.getString("recordId"));assertEquals(123,prepared.getLong("revision"));assertEquals(3,prepared.getInt("syncProtocol"));assertEquals(4,prepared.getInt("pathVersion"))
            val entries=prepared.getJSONArray("files");val files=(0 until entries.length()).map{entries.getJSONObject(it)}
            for(f in files)assertEquals(f.getString("sha256"),sha256(File(f.getString("localFile")).readBytes()))
            val pdf=files.single{it.getString("key")=="field"};assertEquals("application/pdf",pdf.getString("mimeType"));assertTrue(File(pdf.getString("localFile")).readText(Charsets.ISO_8859_1).startsWith("%PDF-"))
            assertTrue(files.any{it.getString("key")=="sample_excel"});assertTrue(files.any{it.getString("key")=="sample_json"});assertTrue(files.any{it.getString("key")=="photo/${photo.id}"})
            val replay=AutomaticDelivery.prepare(c,db,intent,readable=false)
            assertEquals(prepared.getString("contentHash"),replay.getString("contentHash"))
            val local=LocalExportBuilder.build(c,db,LocalExportJob("TEST_LOCAL_PDF","PAVEMENT/${s.id}","PAVEMENT",s.id,123))
            assertTrue(local.problems.joinToString(),local.problems.isEmpty())
            assertEquals(pdf.getString("sha256"),local.files.single{it.mimeType=="application/pdf"}.sha256)
            assertTrue(local.files.any{it.sha256==hash.second});assertEquals(hash,fileDigest(original))
            assertEquals(s,db.pavementDao().sample(s.id));assertEquals(before,db.pavementDao().damages(s.id));assertEquals(intent,db.automaticSyncDao().get(intent.key))
            assertEquals(0,db.automaticSyncDao().get(intent.key)!!.syncedRevision)
        }
    }
}
