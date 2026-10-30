package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.LocalExportJob
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class PublicDeliveryNamesTest {
 private val c get()=ApplicationProvider.getApplicationContext<Context>()
 @Test fun unicodeAndWindowsComponentsAreSafeAndPrefixStable(){
  assertEquals("abc",PublicDeliveryNames.component("abc😀z",4));assertEquals("Café",PublicDeliveryNames.component("Cafe\u0301"))
  assertEquals("Ficha - CON",PublicDeliveryNames.component("CON"));assertEquals("A B",PublicDeliveryNames.component("A/../B\u0000"))
  val name=PublicDeliveryNames.photoName("Ñ".repeat(100),"CULVERT_PANORAMIC",999,"jpg")
  assertTrue(name.toByteArray().size<=80);assertTrue(name.endsWith("Foto 999.jpg"))
  assertTrue(runCatching{PublicDeliveryNames.photoName("X","GENERAL",1000,"jpg")}.isFailure)
 }
 @Test fun photographNameIncludesRouteProgressiveCategoryAndStableNumber(){
  assertEquals("ALCANTARILLA - PE-28H - PR 40+200 - PANORAMICA - Foto 1.jpg",PublicDeliveryNames.photoName("ALCANTARILLA - PE-28H - PR 40+200","CULVERT_PANORAMIC",1,"jpg"))
 }
 @Test fun oldOperationalCapabilitiesProtectNewPreparedNamesWithoutSendingAnyPost(){
  var posts=0
  val client=okhttp3.OkHttpClient.Builder().addInterceptor{chain->if(chain.request().method!="GET")posts++
   okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("FICTITIOUS").body(okhttp3.ResponseBody.create(null,"{\"ok\":true,\"syncProtocol\":3,\"pathVersion\":4,\"stableFamilyNumbering\":true,\"photoCategoryVersion\":1,\"readablePhotoNamingVersion\":2}")).build()}.build()
  val http=AutomaticDriveHttp(client,"https://test.invalid/exec","");assertFalse(http.supportsPublicNames())
  val p=JSONObject().put("publicNamingVersion",1)
  assertTrue(runCatching{http.post(p)}.exceptionOrNull() is SyncPending)
  assertTrue(runCatching{http.reservePhotoNames(p)}.exceptionOrNull() is SyncPending);assertEquals(0,posts)
 }
 @Test fun localNamesPersistAcrossHeaderEditAndAreDistinctForDifferentPhotographs()=runBlocking{
  Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use{db->
   db.inventoryDao().insertRecord(testRecord("PUBLIC_NAMES",route="PE-28H",pr="40",sic="SIC-18").copy(assetType="ALCANTARILLA",startDistanceM=200.0))
   val job=LocalExportJob("PUBLIC_JOB","SIC/PUBLIC_NAMES","SIC","PUBLIC_NAMES",1)
   val a=PublicDeliveryNames.localPhoto(db,job,"PHOTO_1","CULVERT_PANORAMIC","jpg")
   val b=PublicDeliveryNames.localPhoto(db,job,"PHOTO_2","CULVERT_PANORAMIC","jpg")
   assertNotEquals(a,b);assertTrue(a.endsWith("Foto 1.jpg"));assertTrue(b.endsWith("Foto 2.jpg"))
   val record=db.inventoryDao().recordById("PUBLIC_NAMES")!!;db.inventoryDao().updateRecord(record.copy(routeCode="EDITED",startDistanceM=999.0))
   assertEquals(a,PublicDeliveryNames.localPhoto(db,job,"PHOTO_1","CULVERT_PANORAMIC","jpg"))
  }
 }
 @Test fun originalBackendContractAndDescriptiveCanonicalHashAgreeExactly(){
  val fixture=File(System.getProperty("gbo.public.names.fixture","../backend/candidato-original-reanudable/tests/PublicNamesContract.json"))
  val data=JSONObject(fixture.readText());val p=data.getJSONObject("prepared");val ack=data.getJSONObject("ack");val reservation=data.getJSONObject("reservation")
  assertEquals(p.getString("contentHash"),AutomaticDelivery.contentHash(p));assertTrue(AutomaticDelivery.validAck(p,ack))
  ReadablePhotoNames.remember(c,p,reservation);PublicDeliveryNames.remember(c,p,reservation)
  assertEquals(reservation.getJSONArray("photos").getJSONObject(0).getString("name"),ReadablePhotoNames.name(c,"FICTITIOUS_PHOTO","CULVERT_PANORAMIC"))
  val tampered=JSONObject(ack.toString()).put("logicalPath",ack.getString("logicalPath")+" - ALTERADO")
  assertFalse(AutomaticDelivery.validAck(p,tampered));val hash=AutomaticDelivery.contentHash(p);p.put("publicDisplayLabel","ALTERADO");assertNotEquals(hash,AutomaticDelivery.contentHash(p))
 }
}
