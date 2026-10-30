package com.tuempresa.inventariovial

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.repository.TechnicalLengthStore
import com.tuempresa.inventariovial.export.TechnicalLengthExport
import com.tuempresa.inventariovial.scap.export.TemplateWorkbook
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class LinearLengthDeliveryTest {
 @Test fun kilometreReferenceReachesPreparedDriveJsonAndExcelWithoutChangingIdentity()=runBlocking {
  val c=ApplicationProvider.getApplicationContext<Application>()
  Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use {db->
   val r=testRecord("FICTITIOUS_KM_DRIVE",sic="SIC-20",route="PE-28H",pr="0042",distance=995.0)
       .copy(assetType="BADEN",roadbedCode="CD",endPrCode="0043",endDistanceM=5.0,sideCode="D",surveyDirection="INCREASING",latitude=null,longitude=null)
   com.tuempresa.inventariovial.repository.InventoryRepository(db).saveSic20(r,Sic20Entity(r.id,"12","2",2.0,3.0,"1","1"),emptyList())
   TechnicalLengthStore.rememberReference(db,r,TechnicalLengthStore.KILOMETRES,true)
   val beforePreparation=db.inventoryDao().recordById(r.id)!!
   val intent=db.automaticSyncDao().get("SIC/${r.id}")!!
   val preview=com.tuempresa.inventariovial.core.AutomaticDelivery.namingPreview(c,db,intent)
   com.tuempresa.inventariovial.core.ReadablePhotoNames.remember(c,preview,ReleasePhotoV2Fixture.reservation(preview,"FICTITIOUS_KM_FOLDER"))
   val prepared=com.tuempresa.inventariovial.core.AutomaticDelivery.prepare(c,db,intent)
   assertEquals(intent.key,prepared.getString("key"));assertEquals(intent.revision,prepared.getLong("revision"))
   val files=prepared.getJSONArray("files");val entries=(0 until files.length()).map{files.getJSONObject(it)}
   val metadata=org.json.JSONObject(java.io.File(entries.single{it.getString("key")=="metadata"}.getString("localFile")).readText())
   assertEquals(10.0,metadata.getJSONObject("technicalLength").getDouble("calculatedM"),0.0)
   assertEquals("EXPLICIT_KILOMETRES",metadata.getJSONObject("technicalLength").getString("calculationSource"))
   val workbook=java.io.File(entries.single{it.getString("key")=="technical-length"}.getString("localFile"))
   assertTrue(TemplateWorkbook(workbook.inputStream()).sheet(1).documentElement.textContent.contains("EXPLICIT_KILOMETRES"))
   assertEquals(beforePreparation,db.inventoryDao().recordById(r.id));assertEquals(0L,db.automaticSyncDao().get(intent.key)!!.syncedRevision)
  }
 }
 @Test fun cleanupProtectsPendingAndEditedRevisionsAndChronologyUsesSurveyDate() {
  val ack=deliveryFixture()
  val row=com.tuempresa.inventariovial.localfiles.LocalInventoryItem("SIC",ack.recordId,"SIC","FICTICIO","PE-28H","PR 42+995",null,"CD","INCREASING","ACTIVE",2,"30/10/2026")
  val copy=com.tuempresa.inventariovial.localfiles.LocalExportJob("FICTITIOUS_COPY",row.key,"SIC",row.id,2,state="READY")
  val index=com.tuempresa.inventariovial.localfiles.LocalInventoryIndex
  assertTrue(index.cleanupCandidate(row,ack,copy))
  assertFalse(index.cleanupCandidate(row.copy(revision=3),ack,copy))
  assertFalse(index.cleanupCandidate(row,ack.copy(state="ERROR"),copy))
  assertFalse(index.cleanupCandidate(row,ack,copy.copy(state="PARTIAL")))
  assertEquals(listOf("30/10/2026","2026-10-29",""),index.chronological(listOf(row.copy(id="older",surveyDate="2026-10-29"),row.copy(id="unknown",surveyDate=""),row)).map{it.surveyDate})
 }
 @Test fun explicitKilometresPersistReopenRecalculateAndExportWithoutReplacingMeasuredLength()=runBlocking {
  val c=ApplicationProvider.getApplicationContext<Application>()
  val name="linear-delivery.db"
  fun open()=Room.databaseBuilder(c,InventoryDatabase::class.java,name).build()
  val r=testRecord("FICTITIOUS_LINEAR",sic="SIC-19",pr="0042",distance=995.0).copy(endPrCode="0043",endDistanceM=5.0,surveyDirection="INCREASING")
  open().use {db->db.inventoryDao().insertRecord(r);TechnicalLengthStore.rememberReference(db,r,TechnicalLengthStore.KILOMETRES,true)}
  open().use {db->
   val saved=db.inventoryDao().recordById(r.id)!!;val ext=db.inventoryDao().sicExtension(r.id)!!
   assertEquals(r,saved);assertEquals("10.0",ext.values()["_linearMetres"])
   val item=InventoryRecordForExport(saved,sic19=Sic19Entity(r.id,"01","1","1","1","1"),sicExtension=ext)
   val data=TechnicalLengthExport.metadata(item,emptyList())
   assertEquals(10.0,data.getDouble("calculatedM"),0.0);assertEquals("EXPLICIT_KILOMETRES",data.getString("calculationSource"))
   val bytes=ByteArrayOutputStream();TechnicalLengthExport.write(bytes,item,emptyList())
   assertTrue(TemplateWorkbook(bytes.toByteArray().inputStream()).sheet(1).documentElement.textContent.contains("EXPLICIT_KILOMETRES"))
   val edited=saved.copy(endDistanceM=15.0,updatedAt=saved.updatedAt+1)
   db.inventoryDao().updateRecord(edited);TechnicalLengthStore.refresh(db,edited,emptyList())
   assertEquals("20.0",db.inventoryDao().sicExtension(r.id)!!.values()["_linearMetres"])
   assertEquals(20.0,TechnicalLengthExport.metadata(item.copy(record=edited,sicExtension=db.inventoryDao().sicExtension(r.id)),emptyList()).getDouble("calculatedM"),0.0)
   assertNull(TechnicalLengthExport.metadata(item.copy(sicExtension=null),emptyList()).opt("calculatedM").takeUnless{it==org.json.JSONObject.NULL})
  }
 }
}
