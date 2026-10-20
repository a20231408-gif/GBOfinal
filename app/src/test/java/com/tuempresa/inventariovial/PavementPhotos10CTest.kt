package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.pavement.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.IOException
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementPhotos10CTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    private fun sessions()=AuthSessionManager().apply {login(UserCacheEntity("u","u","Inspector","OPERATOR",true,"",0,0,0))}
    private fun sample(id:String="s")=PavementSampleEntity(id,"T","R10",1,"Derecho","UC","00+000","00+040",5.0,40.0,"ASPHALT","2026-10-12","u","Inspector",updatedAt=1)
    private fun damage(id:String="d",sampleId:String="s")=PavementDamageEntity(id,sampleId,1,"ASPHALT",7,"Grieta de borde","L","LINEAR",1.0,2.0,2.0,15.0,null,1.0,13.0,null,null,null,"")
    private suspend fun seed(d:InventoryDatabase) {d.pavementDao().saveSample(sample());d.pavementDao().saveDamage(damage());d.pavementDao().saveDamage(damage("same-type").copy(correlativo=2))}
    private fun image(format:Bitmap.CompressFormat=Bitmap.CompressFormat.JPEG)=File(context.cacheDir,"10c-${UUID.randomUUID()}.img").apply {
        val b=Bitmap.createBitmap(24,32,Bitmap.Config.ARGB_8888);outputStream().use {assertTrue(b.compress(format,95,it))};b.recycle()
    }
    @Test fun cameraAndImportsStayBoundToExactDamageAndShareSamplePanel()=runBlocking {db().use {d->
        seed(d);val repo=PavementRepository(d,sessions());val file=image()
        try {
            val before=PavementCalculator(context).calculate(sample(),d.pavementDao().damages("s"))
            repo.savePhoto(PavementDamagePhotoEntity("camera","d",file.path),"s")
            repo.savePhoto(PavementDamagePhotoEntity("gallery","d",file.path),"s")
            repo.savePhoto(PavementDamagePhotoEntity("other-photo","same-type",file.path),"s")
            assertEquals(setOf("camera","gallery"),repo.photos("d").map {it.id}.toSet())
            assertEquals(listOf("other-photo"),repo.photos("same-type").map {it.id})
            assertEquals(3,repo.samplePhotos("s").first().size)
            assertEquals(before,PavementCalculator(context).calculate(sample(),d.pavementDao().damages("s")))
            d.pavementDao().saveSample(sample("other-sample").copy(muestra=2));d.pavementDao().saveDamage(damage("foreign","other-sample"))
            assertTrue(runCatching {repo.savePhoto(PavementDamagePhotoEntity("wrong","foreign",file.path),"s")}.isFailure)
            assertTrue(d.automaticSyncDao().all().isEmpty())
        }finally {file.delete()}
    }}
    @Test fun importedPngAndJpegExifOriginalsAreNotReencoded() {
        for(format in listOf(Bitmap.CompressFormat.PNG,Bitmap.CompressFormat.JPEG)) {
            val source=image(format)
            if(format==Bitmap.CompressFormat.JPEG)ExifInterface(source.path).apply {setAttribute(ExifInterface.TAG_ORIENTATION,"6");saveAttributes()}
            val before=source.readBytes();val imported=PavementPhotos.importImage(context,Uri.fromFile(source))
            val target=File(context.cacheDir,"copy-${UUID.randomUUID()}.jpg")
            try {
                assertArrayEquals(before,imported.readBytes());DeliveryJpeg.copy(imported,target)
                assertTrue(JpegEnvelope.valid(target.readBytes()));assertArrayEquals(before,imported.readBytes())
                if(format==Bitmap.CompressFormat.JPEG)assertEquals("6",ExifInterface(target.path).getAttribute(ExifInterface.TAG_ORIENTATION))
            }finally {source.delete();imported.delete();target.delete()}
        }
    }
    @Test fun replacementAndRetirementKeepOriginalsAndHistory()=runBlocking {db().use {d->
        seed(d);val repo=PavementRepository(d,sessions());val old=image();val replacement=image()
        try {
            repo.savePhoto(PavementDamagePhotoEntity("old","d",old.path),"s")
            assertTrue(runCatching {repo.replacePhoto("old",PavementDamagePhotoEntity("new","same-type",replacement.path),"s")}.isFailure)
            assertEquals(listOf("old"),repo.photos("d").map {it.id})
            repo.replacePhoto("old",PavementDamagePhotoEntity("new","d",replacement.path),"s")
            assertEquals(listOf("new"),repo.photos("d").map {it.id})
            assertEquals("new",d.pavementDao().photo("old")!!.replacedByPhotoId)
            assertTrue(runCatching {repo.savePhoto(PavementDamagePhotoEntity("old","d",replacement.path),"s")}.isFailure)
            repo.deletePhoto("new","d",File(context.filesDir,"images"));repo.deletePhoto("new","d",File(context.filesDir,"images"))
            assertTrue(repo.photos("d").isEmpty());assertEquals(2,repo.photoHistory("d").first().size)
            assertTrue(old.exists() && replacement.exists());assertTrue(runCatching {repo.deleteDamage(damage())}.isFailure)
        }finally {old.delete();replacement.delete()}
    }}
    @Test fun originalsAndActiveAndRetiredAssociationSurviveOfflineReopen()=runBlocking {
        val name="photos10c-${UUID.randomUUID()}";fun open()=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        var d=open();val file=image();val auth=sessions()
        try {
            seed(d);val repo=PavementRepository(d,auth)
            repo.savePhoto(PavementDamagePhotoEntity("keep","same-type",file.path),"s")
            repo.savePhoto(PavementDamagePhotoEntity("retired","d",file.path),"s");repo.deletePhoto("retired","d",File(context.filesDir,"images"))
            d.close();d=open();val reopened=PavementRepository(d,auth)
            assertEquals("keep",reopened.samplePhotos("s").first().single().id)
            assertEquals("same-type",reopened.photos("same-type").single().damageId)
            assertEquals("retired",reopened.photoHistory("d").first().single().id)
            assertTrue(file.exists());assertTrue(d.automaticSyncDao().all().isEmpty())
        }finally {d.close();context.deleteDatabase(name);file.delete()}
    }
    @Test fun migration18to19KeepsExistingPhotoIdsPathsAndClosedSamples()=runBlocking {
        val name="migration10c-${UUID.randomUUID()}";val path=context.getDatabasePath(name);path.parentFile!!.mkdirs()
        val schema=JSONObject(File("schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/18.json").readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(path,null).use {old->
            val entities=schema.getJSONArray("entities")
            for(n in 0 until entities.length()) {
                val entity=entities.getJSONObject(n);val table=entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}",table))
                val indices=entity.optJSONArray("indices") ?: JSONArray();for(k in 0 until indices.length())old.execSQL(indices.getJSONObject(k).getString("createSql").replace("\${TABLE_NAME}",table))
            }
            old.execSQL("INSERT INTO pavement_samples (id,tramo,ruta,muestra,carril,calzada,prInicial,prFinal,anchoM,largoM,surface,fecha,inspectorUserId,inspectorNombre,status,createdAt,updatedAt) VALUES ('s','T','R',1,'Derecho','UC','00+000','00+040',5,40,'ASPHALT','2026-10-12','u','Inspector','CLOSED',1,2)")
            old.execSQL("INSERT INTO pavement_damages (id,sampleId,correlativo,surface,code,name,geometry,createdAt) VALUES ('d','s',1,'ASPHALT',7,'Grieta','LINEAR',1)")
            old.execSQL("INSERT INTO pavement_damage_photos VALUES ('historic','d','/original/unchanged.jpg',1)");old.version=18
        }
        val d=Room.databaseBuilder(context,InventoryDatabase::class.java,name).addMigrations(InventoryDatabase.MIGRATION_18_19,InventoryDatabase.MIGRATION_19_20).build()
        try {assertEquals("CLOSED",d.pavementDao().sample("s")!!.status);val p=d.pavementDao().photos("d").single();assertEquals("historic",p.id);assertEquals("/original/unchanged.jpg",p.localPath);assertNull(p.retiredAt)}
        finally {d.close();context.deleteDatabase(name)}
    }
    @Config(shadows=[PdfDocument10CShadow::class]) @Test fun existingQueueAcceptsJpegAndOnlyConfirmsValidAckWithIdempotentRetry()=runBlocking {db().use {d->
        seed(d);val repo=PavementRepository(d,sessions());val original=image(Bitmap.CompressFormat.PNG)
        val captured=image()
        try {
            repo.savePhoto(PavementDamagePhotoEntity("png","d",original.path),"s")
            repo.savePhoto(PavementDamagePhotoEntity("camera","same-type",captured.path),"s")
            repo.closeSample("s");val revision=d.automaticSyncDao().get("PAVEMENT/s")!!.revision
            // Surface preparation errors directly; do not obscure them with the drain's safe user-facing message.
            AutomaticDelivery.prepare(context,d,d.automaticSyncDao().get("PAVEMENT/s")!!)
            val backend=Backend();backend.loseAck=true
            AutomaticSyncDrain.run(context,d,{backend},null)
            assertEquals("Preparation failure: ${d.automaticSyncDao().get("PAVEMENT/s")}",1,backend.posts)
            assertEquals(0,d.automaticSyncDao().get("PAVEMENT/s")!!.syncedRevision)
            AutomaticSyncDrain.run(context,d,{backend},null)
            val intent=d.automaticSyncDao().get("PAVEMENT/s")!!;assertEquals(intent.error,"SYNCED",intent.state);assertEquals(revision,intent.syncedRevision)
            assertEquals(1,backend.mutations);assertEquals(2,backend.posts)
            val p=backend.payload!!;val files=p.getJSONArray("files")
            val photoEntries=(0 until files.length()).map {files.getJSONObject(it)}.filter {it.getString("key").startsWith("photo/")}
            assertEquals(setOf("photo/png","photo/camera"),photoEntries.map {it.getString("key")}.toSet())
            photoEntries.forEach {f->val bytes=File(f.getString("localFile")).readBytes();assertTrue(JpegEnvelope.valid(bytes));assertEquals(com.tuempresa.inventariovial.delivery.sha256(bytes),f.getString("sha256"))}
            val sampleJson=(0 until files.length()).map {files.getJSONObject(it)}.single {it.getString("key")=="sample_json"}
            val json=File(sampleJson.getString("localFile")).readText();assertTrue(json.contains("same-type"));assertTrue(json.contains("png"))
            assertTrue(original.exists());assertEquals("camera",repo.photos("same-type").single().id)
        }finally {original.delete();captured.delete()}
    }}
    @Config(shadows=[PdfDocument10CShadow::class]) @Test fun schema19PreservesTheSourceHashOfExistingSchema18PavementSnapshots()=runBlocking {db().use {d->
        seed(d);val repo=PavementRepository(d,sessions());val file=image()
        try {
            repo.savePhoto(PavementDamagePhotoEntity("z-old-first","d",file.path,createdAt=123),"s")
            repo.savePhoto(PavementDamagePhotoEntity("a-old-second","d",file.path+".second",createdAt=123),"s")
            val second=File(file.path+".second");file.copyTo(second)
            repo.closeSample("s")
            val sample=d.pavementDao().sample("s")!!;val damages=d.pavementDao().damages("s")
            val legacyPhotos=listOf(listOf(
                "PavementDamagePhotoEntity(id=z-old-first, damageId=d, localPath=${file.path}, createdAt=123)",
                "PavementDamagePhotoEntity(id=a-old-second, damageId=d, localPath=${file.path}.second, createdAt=123)"),emptyList())
            assertEquals(listOf("z-old-first","a-old-second"),d.pavementDao().photos("d").map {it.id})
            val legacy=sample.toString()+damages.toString()+legacyPhotos.toString()+emptyList<PavementSectionEntity>().toString()
            val intent=d.automaticSyncDao().get("PAVEMENT/s")!!
            val prepared=AutomaticDelivery.prepare(context,d,intent)
            assertEquals(com.tuempresa.inventariovial.delivery.sha256(legacy.toByteArray()),prepared.getString("sourceHash"))
            assertTrue(AutomaticDelivery.current(d,intent,prepared))
            assertEquals(prepared.toString(),AutomaticDelivery.prepare(context,d,intent).toString())
        }finally {File(file.path+".second").delete();file.delete()}
    }}
    private class Backend:AutomaticDriveTransport {
        var posts=0;var mutations=0;var loseAck=false;var payload:JSONObject?=null;private var receipt:JSONObject?=null
        override fun supportsV3()=true
        override fun supportsFamilyPaths()=true
        override fun post(p:JSONObject):JSONObject {
            posts++;payload=JSONObject(p.toString())
            if(receipt==null) {
                mutations++;val prefix="${p.getString("routeCode")}/EVALUACION_SUPERFICIAL/${p.getString("tramo")}/${p.getString("surface")}/MUESTRA 1"
                val files=JSONArray();val expected=p.getJSONArray("files")
                for(n in 0 until expected.length()) {val f=expected.getJSONObject(n);files.put(JSONObject().put("key",f.getString("key")).put("sha256",f.getString("sha256")).put("fileId","file-$n").put("folderId","folder").put("requestedPath",f.getString("path")).put("logicalPath",prefix+"/"+f.getString("path")))}
                val aggregates=JSONArray();val rows=p.getJSONArray("sicRows")
                val grouped=(0 until rows.length()).map {rows.getJSONObject(it)}.groupBy {it.getString("format")}
                for((format,items) in grouped)aggregates.put(JSONObject().put("format",format).put("fileId","excel-$format").put("folderId","excel-folder").put("sha256","a".repeat(64)).put("logicalPath","${p.getString("routeCode")}/EXCEL_SIC/$format.xlsx").put("recordIds",JSONArray(items.map {it.getString("recordId")})))
                receipt=JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",4).put("key",p.getString("key")).put("revision",p.getLong("revision")).put("contentHash",p.getString("contentHash")).put("folderId","folder").put("logicalPath",prefix).put("files",files).put("sicFiles",aggregates)
            }
            assertTrue("Mock ACK: ${receipt}",AutomaticDelivery.validAck(p,requireNotNull(receipt)))
            if(loseAck) {loseAck=false;throw IOException("Simulated lost ACK")}
            return JSONObject(receipt.toString())
        }
    }
}
