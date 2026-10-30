package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.pavement.PavementSampleEntity
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class FieldCleanupAndConsolidationTest {
 private val c get()=ApplicationProvider.getApplicationContext<Context>()
 private fun db()=Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build()
 private class Store:SharedDocumentStore {
  val files=mutableMapOf<String,ByteArray>();var deletes=0
  override fun reserve(file:LocalSharedFile,knownUri:Uri?)=error("unused")
  override fun write(uri:Uri,source:File)=error("unused")
  override fun publish(slot:SharedDocumentStore.Slot,file:LocalSharedFile)=error("unused")
  override fun digest(uri:Uri)=files.getValue(uri.toString()).let{it.size.toLong() to com.tuempresa.inventariovial.delivery.sha256(it)}
  override fun removeOwned(file:LocalSharedFile):Boolean{val uri=Uri.parse(file.uri);if(!files.containsKey(uri.toString()))return false;require(digest(uri)==(file.size to file.sha256));files.remove(uri.toString());deletes++;return true}
 }
 private suspend fun seed(d:InventoryDatabase,s:Store):ConfirmedLocalCleanup.Candidate {
  val original=File(c.filesDir,"cleanup-${UUID.randomUUID()}.json").apply{writeText("{\"fictitious\":true}")};val hash=fileDigest(original)
  val old=deliveryFixture();val p=JSONObject(old.preparedJson!!);val ack=JSONObject(old.receiptJson!!)
  p.getJSONArray("files").getJSONObject(0).put("key","metadata").put("path","FICHA.json").put("sha256",hash.second).put("mimeType","application/json")
  ack.getJSONArray("files").getJSONObject(0).put("key","metadata").put("requestedPath","FICHA.json").put("logicalPath",ack.getString("logicalPath")+"/FICHA.json").put("sha256",hash.second)
  d.inventoryDao().insertRecord(testRecord(old.recordId,sic="SIC-17").copy(updatedAt=2,assetType="PUENTE"))
  d.automaticSyncDao().put(old.copy(preparedJson=p.toString(),receiptJson=ack.toString()))
  val f=LocalSharedFile("FICTITIOUS_FILE","FICTITIOUS_JOB",old.key,"Download/GBO Inventario Vial/PE-28H/PUENTES/PUENTE 1/","FICHA.json","application/json",hash.first,hash.second,original.path,"content://fictitious/local",state="READY")
  d.localFilesDao().saveJob(LocalExportJob(f.jobId,old.key,"SIC",old.recordId,2,state="READY",manifestJson=LocalPlan(listOf(f),emptyList(),4).json(),completed=1,total=1))
  d.localFilesDao().saveFile(f);s.files[f.uri!!]=original.readBytes();return ConfirmedLocalCleanup.Candidate(old.key,2)
 }
 private fun destination(backup:ConfirmedLocalCleanup.Backup):Uri=Uri.parse("content://fictitious/backup-${UUID.randomUUID()}").also{org.robolectric.Shadows.shadowOf(c.contentResolver).registerInputStreamSupplier(it){ByteArrayInputStream(backup.zip.readBytes())}}
 @Test fun cleanupArchivesAndPreservesIdentity()=runBlocking{db().use{d->
  val s=Store();val item=seed(d,s);val before=d.inventoryDao().recordById(item.key.substringAfter('/'));val intent=d.automaticSyncDao().get(item.key)
  val source=File(d.localFilesDao().files("FICTITIOUS_JOB").single().source)
  val backup=ConfirmedLocalCleanup.prepare(c,d,listOf(item),s);assertEquals(1,ConfirmedLocalCleanup.clean(c,d,backup,destination(backup),"FICTITIOUS",s))
  assertTrue(s.files.isEmpty());assertTrue(source.isFile);assertEquals(before,d.inventoryDao().recordById(item.key.substringAfter('/')));assertEquals(intent,d.automaticSyncDao().get(item.key))
  assertTrue(ConfirmedLocalCleanup.archived(listOf(d.localFilesDao().choice(ConfirmedLocalCleanup.PREFIX+item.key)!!),item.key,2))
  ConfirmedLocalCleanup.restore(d,item.key);assertFalse(ConfirmedLocalCleanup.archived(listOf(d.localFilesDao().choice(ConfirmedLocalCleanup.PREFIX+item.key)!!),item.key,2))
 }}
 @Test fun changedRevisionProtectsSelection()=runBlocking{db().use{d->
  val s=Store();val item=seed(d,s);val backup=ConfirmedLocalCleanup.prepare(c,d,listOf(item),s)
  val r=d.inventoryDao().recordById(item.key.substringAfter('/'))!!;d.inventoryDao().updateRecord(r.copy(updatedAt=3))
  assertTrue(runCatching{ConfirmedLocalCleanup.clean(c,d,backup,destination(backup),"FICTITIOUS",s)}.isFailure);assertEquals(0,s.deletes)
  d.inventoryDao().updateRecord(r);val i=d.automaticSyncDao().get(item.key)!!;d.automaticSyncDao().put(i.copy(receiptJson=JSONObject(i.receiptJson!!).put("files",org.json.JSONArray()).toString()))
  assertTrue(runCatching{ConfirmedLocalCleanup.prepare(c,d,listOf(item),s)}.isFailure);assertEquals(0,s.deletes)
 }}
 @Test fun corruptBackupNeverCleans()=runBlocking{db().use{d->
  val s=Store();val item=seed(d,s);val backup=ConfirmedLocalCleanup.prepare(c,d,listOf(item),s);val uri=Uri.parse("content://fictitious/bad")
  org.robolectric.Shadows.shadowOf(c.contentResolver).registerInputStreamSupplier(uri){ByteArrayInputStream(byteArrayOf(1,2))}
  assertTrue(runCatching{ConfirmedLocalCleanup.clean(c,d,backup,uri,"FICTITIOUS",s)}.isFailure);assertEquals(0,s.deletes)
  s.files["content://fictitious/local"]=byteArrayOf(9);assertTrue(runCatching{ConfirmedLocalCleanup.prepare(c,d,listOf(item),s)}.isFailure);assertEquals(0,s.deletes)
 }}
 @Test fun pavementDraftsPublishOnlyAtClosure()=runBlocking{db().use{d->
  AutomaticLocalDocuments.install(d.openHelper.writableDatabase,true)
  val s=PavementSampleEntity("FICTITIOUS_UNIT","T","PE-22A",1,"Derecho","CD","00+000","00+020",10.0,20.0,"RIGID","2026-10-30","FICTITIOUS","FICTITIOUS",slabRows=4,slabColumns=2)
  for(n in 1L..10L)d.pavementDao().saveSample(s.copy(updatedAt=n))
  assertEquals(10L,d.pavementDao().sample(s.id)!!.updatedAt);assertTrue(d.localFilesDao().jobs().isEmpty())
  d.pavementDao().saveSample(s.copy(status="CLOSED",updatedAt=11));assertEquals(1,d.localFilesDao().jobs().size);assertEquals(11L,d.localFilesDao().jobs().single().revision)
  d.pavementDao().saveSample(s.copy(updatedAt=12));d.localFilesDao().saveJob(LocalExportJob("OLD_DRAFT","PAVEMENT/${s.id}","PAVEMENT",s.id,12,automatic=true,createdAt=0))
  LocalCopyDrain.run(c,d,Store());assertEquals("SUPERSEDED",d.localFilesDao().job("OLD_DRAFT")!!.state);assertEquals(12L,d.pavementDao().sample(s.id)!!.updatedAt)
 }}
}
