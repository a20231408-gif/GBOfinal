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
