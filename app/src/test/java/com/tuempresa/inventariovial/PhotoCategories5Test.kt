package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.camera.PhotoCaptureMetadata
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.field.*
import com.tuempresa.inventariovial.repository.*
import com.tuempresa.inventariovial.model.form.*
import kotlinx.coroutines.*
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoCategories5Test {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    private fun jpg(name:String):String {
        val f=File(context.filesDir,"photo5/$name.jpg");f.parentFile!!.mkdirs()
        val b=Bitmap.createBitmap(24,18,Bitmap.Config.ARGB_8888)
        f.outputStream().use {assertTrue(b.compress(Bitmap.CompressFormat.JPEG,95,it))};b.recycle();return f.path
    }
    private suspend fun record(d:InventoryDatabase,id:String,sic:String="SIC-20",cls:String="12",type:String="1",ends:Boolean=true) {
        d.inventoryDao().insertRecord(testRecord(id,pr="10",route="PE-22A",sic=sic).copy(assetType="",latitude=null,longitude=null,
            endPrCode=if(ends)"10" else null,endDistanceM=if(ends)200.0 else null,sideCode=if(cls=="13")"S" else "D",updatedAt=1000,surveyDirection="INCREASING"))
        when(sic) {
            "SIC-18"->d.inventoryDao().insertSic18(Sic18Entity(id,"06","1",1,"1",1.0,1.0,"1","1"))
            "SIC-19"->d.inventoryDao().insertSic19(Sic19Entity(id,cls,type,"1","1","1"))
            "SIC-20"->d.inventoryDao().insertSic20(Sic20Entity(id,cls,type,1.0,1.0,"1","1"))
            "SIC-21"->d.inventoryDao().insertSic21(Sic21Entity(id,cls,type,"1","1"))
            "SIC-22"->d.inventoryDao().insertSic22(Sic22Entity(id,"20","1","1","R-1",null,"1",null,null,null))
        }
    }
    private suspend fun add(d:InventoryDatabase,id:String,categories:List<String>):List<PhotoEntity> {
        val paths=categories.mapIndexed {n,_->jpg("$id-$n")}
        InventoryPhotoEdits.save(d,id,paths,paths.zip(categories).toMap(),paths.associateWith {PhotoCaptureMetadata(100L,null)},1001)
        return d.inventoryDao().snapshot(id)!!.photos.sortedBy {it.photoIndex}
    }
    private suspend fun multiple(sic:String,cls:String,type:String="1") {db().use {d->
        val id="multiple-$sic-$cls";record(d,id,sic,cls,type)
        val categories=PhotoCategoryPolicy.categories(sic,classCode=cls,typeCode=type).flatMap {c->List(3){c.key}}
        val photos=add(d,id,categories);assertEquals(categories,photos.map {it.photoCategory});assertEquals(9,photos.size)
        assertEquals(9,photos.map {it.id}.distinct().size);assertTrue(photos.all {it.recordId==id && it.capturedAt==100L && it.syncStatus=="PENDING"})
    }}
    @Test fun badenHasTenIndependentPhotosAcrossThreeCategories()=runBlocking {db().use {d->
        record(d,"baden");val keys=List(3){"INICIO"}+List(2){"FIN"}+List(5){"OBSERVACION"}
        assertEquals(keys,add(d,"baden",keys).map {it.photoCategory})
    }}
    @Test fun guardwayAllowsMultipleInEachCategory()=runBlocking {multiple("SIC-21","19")}
    @Test fun wallDitchAndTunnelUseActualStartAndEndModels()=runBlocking {
        multiple("SIC-20","14");multiple("SIC-19","08");multiple("SIC-20","13")
    }
    @Test fun studsUseEndsOnlyWhenARealSegmentExists()=runBlocking {
        multiple("SIC-21","20")
        assertEquals(listOf("GENERAL"),PhotoCategoryPolicy.categories("SIC-21",classCode="20",hasEnds=false).map {it.key})
        assertEquals(listOf("GENERAL"),PhotoCategoryPolicy.categories("SIC-21",classCode="19",typeCode="2").map {it.key})
    }
    @Test fun verticalHasMultiplePanoramicAndElementPhotos()=runBlocking {db().use {d->
        record(d,"vertical","SIC-22",ends=false)
        assertEquals(listOf("PANORAMICA","ELEMENTO"),PhotoCategoryPolicy.categories("SIC-22").map {it.key})
        assertEquals(6,add(d,"vertical",List(3){"PANORAMICA"}+List(3){"ELEMENTO"}).size)
    }}
    @Test fun culvertRetainsKeysAndShared18AIdentity()=runBlocking {db().use {d->
        record(d,"culvert","SIC-18");val keys=PhotoCategoryPolicy.culvert.flatMap {c->List(2){c.key}}
        assertEquals(keys,add(d,"culvert",keys).map {it.photoCategory})
        assertEquals(PhotoCategoryPolicy.culvert,PhotoCategoryPolicy.categories("SIC-18A"))
    }}
    @Test fun bridgeUsesExactlyExistingScapCategories() {
        val keys=com.tuempresa.inventariovial.scap.domain.ScapPhotoCategories.labels.keys
        assertEquals(keys.toList(),PhotoCategoryPolicy.categories("SIC-17").map {it.key})
        assertEquals("AGUAS_ARRIBA",DriveInventorySchema.category("UPSTREAM"));assertEquals("AGUAS_ABAJO",DriveInventorySchema.category("DOWNSTREAM"))
    }
    @Test fun extraLinearModelsAndFordPhysicalEndsDoNotChangePointLocationContracts() {
        assertEquals(PhotoCategoryPolicy.linear,PhotoCategoryPolicy.categories("SIC-20",classCode="12",hasEnds=false))
        assertEquals(PhotoCategoryPolicy.linear,PhotoCategoryPolicy.categories("SIC-23",classCode="21",hasEnds=true))
        assertEquals(PhotoCategoryPolicy.linear,PhotoCategoryPolicy.categories("SIC-23",classCode="22",hasEnds=true))
        assertEquals(listOf("GENERAL"),PhotoCategoryPolicy.categories("SIC-23",classCode="23",hasEnds=false).map {it.key})
    }
    @Test fun independentFajasAndPointFormatsKeepGeneral() {
        listOf("SIC-08","SIC-09","SIC-13","SIC-23","SIC-34").forEach {assertEquals(listOf("GENERAL"),PhotoCategoryPolicy.categories(it).map {c->c.key})}
    }
    @Test fun offlineReopeningRetainsIdsOrderMetadataAndNoDuplicates()=runBlocking(Dispatchers.IO) {
        val name="photo5-reopen-${System.nanoTime()}";fun open()=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        val first=open();record(first,"offline");val photos=add(first,"offline",listOf("INICIO","INICIO","FIN"));first.close()
        open().use {d->
            assertEquals(photos,d.inventoryDao().snapshot("offline")!!.photos.sortedBy {it.photoIndex})
            InventoryPhotoEdits.save(d,"offline",photos.map {it.localPath},photos.associate {it.localPath to it.photoCategory!!},emptyMap(),1002)
            assertEquals(photos,d.inventoryDao().snapshot("offline")!!.photos.sortedBy {it.photoIndex})
        }
    }
    @Test fun replacementRetainsUuidRemoteIdentityOrderAndOriginalHistory()=runBlocking {db().use {d->
        record(d,"replace");val old=add(d,"replace",listOf("INICIO")).single().copy(driveFileId="confirmed-file",syncStatus="SYNCED")
        d.inventoryDao().updatePhotoMetadata(old);val original=File(old.originalPath).readBytes();val path=jpg("replacement")
        val capture=PhotoCaptureMetadata(200L,null,replacesPath=old.localPath)
        InventoryPhotoEdits.save(d,"replace",listOf(path),mapOf(path to "INICIO"),mapOf(path to capture),2000)
        val next=d.inventoryDao().snapshot("replace")!!.photos.single()
        assertEquals(old.id,next.id);assertEquals("confirmed-file",next.driveFileId);assertEquals(old.photoIndex,next.photoIndex)
        assertEquals(path,next.originalPath);assertEquals(200L,next.capturedAt);assertEquals("PENDING",next.syncStatus)
        assertArrayEquals(original,File(old.originalPath).readBytes())
        assertEquals(old.localPath,JSONObject(d.inventoryDao().photoHistory("replace").single().snapshotJson).getString("localPath"))
        InventoryPhotoEdits.save(d,"replace",listOf(path),mapOf(path to "INICIO"),mapOf(path to capture),2001)
        assertEquals(1,d.inventoryDao().photoHistory("replace").size)
    }}
    @Test fun recategorizationKeepsIdentityAndRecordsExplicitChange()=runBlocking {db().use {d->
        record(d,"category");val old=add(d,"category",listOf("GENERAL")).single()
        InventoryPhotoEdits.save(d,"category",listOf(old.localPath),mapOf(old.localPath to "FIN"),emptyMap(),2000)
        assertEquals(old.id,d.inventoryDao().snapshot("category")!!.photos.single().id)
        assertEquals("GENERAL",JSONObject(d.inventoryDao().photoHistory("category").single().snapshotJson).getString("photoCategory"))
        assertEquals("RECATEGORIZED",d.inventoryDao().photoHistory("category").single().action)
    }}
    @Test fun removalPreservesOriginalAndOtherEngineersAndScapPhotographs()=runBlocking {db().use {d->
        record(d,"remove");record(d,"other");val old=add(d,"remove",listOf("GENERAL")).single();val other=add(d,"other",listOf("GENERAL")).single()
        d.inventoryDao().insertPhoto(old.copy(id="inspection-photo",photoIndex=2,scapInspectionId="inspection"))
        InventoryPhotoEdits.save(d,"remove",emptyList(),emptyMap(),emptyMap(),2000)
        assertTrue(File(old.originalPath).isFile);assertEquals("REMOVED",d.inventoryDao().photoHistory("remove").single().action)
        assertEquals("inspection-photo",d.inventoryDao().snapshot("remove")!!.photos.single().id);assertEquals(other,d.inventoryDao().snapshot("other")!!.photos.single())
    }}
    @Test fun replacementDraftSurvivesReopeningWithCategoryAndBaseRevision() {
        val draft=CulvertEditingDraft(Sic18FormState(),emptyMap(),listOf("new"),mapOf("new" to "CULVERT_INLET"),
            mapOf("new" to PhotoCaptureMetadata(200L,null,replacesPath="old")),baseRevision=1000)
        CulvertEditDraft.save(context,"draft-photo5","synthetic-engineer",draft)
        assertEquals(draft,CulvertEditDraft.read(context,"draft-photo5","synthetic-engineer"))
    }
    @Test fun addingPhotosDoesNotRenumberExistingPhotos()=runBlocking {db().use {d->
        record(d,"add");val old=add(d,"add",listOf("INICIO","FIN"));val path=jpg("additional")
        InventoryPhotoEdits.save(d,"add",old.map {it.localPath}+path,mapOf(path to "OBSERVACION"),emptyMap(),2000)
        assertEquals(old,d.inventoryDao().snapshot("add")!!.photos.filter {it.id in old.map {p->p.id}}.sortedBy {it.photoIndex})
        assertEquals(3,d.inventoryDao().snapshot("add")!!.photos.maxOf {it.photoIndex})
    }}
    @Test fun actualAndroidPayloadsIncludeCategoriesMetadataExcelAndNoSib()=runBlocking(Dispatchers.IO) {db().use {d->
        for((id,sic,cls) in listOf(Triple("baden5","SIC-20","12"),Triple("muro5","SIC-20","14"),Triple("tunel5","SIC-20","13"),
            Triple("cuneta5","SIC-19","08"),Triple("guardavia5","SIC-21","19"),Triple("tacha5","SIC-21","20"),Triple("senal5","SIC-22","16"),Triple("alcantarilla5","SIC-18","06"))) {
            record(d,id,sic,cls,ends=sic!="SIC-22")
            val keys=PhotoCategoryPolicy.categories(sic,classCode=cls,typeCode="1").flatMap {c->List(2){c.key}}+"GENERAL"
            add(d,id,keys);AutomaticSync.request(d,"SIC",id,1000)
            val payload=AutomaticDelivery.prepare(context,d,d.automaticSyncDao().get("SIC/$id")!!)
            assertEquals(3,payload.getInt("syncProtocol"));assertEquals(4,payload.getInt("pathVersion"));assertEquals(1,payload.getInt("photoCategoryVersion"))
            assertFalse(payload.has("sibCode"));assertEquals("PENDING",payload.getJSONObject("georeferencing").getString("externalSubmetricStatus"))
            val files=payload.getJSONArray("files")
            for(n in 0 until files.length()) {val f=files.getJSONObject(n);val bytes=File(f.getString("localFile")).readBytes()
                if(f.getString("key")=="metadata") {val meta=JSONObject(bytes.toString(Charsets.UTF_8));assertTrue(meta.isNull("latitude"));assertEquals(keys.size,meta.getJSONArray("photos").length());assertEquals(id,meta.getJSONArray("photos").getJSONObject(0).getString("recordId"))}
                if(f.getString("key").startsWith("photo/")) assertEquals(DriveInventorySchema.category(f.getString("photoCategory")),f.getString("path").substringBefore('/'))
                f.put("base64",android.util.Base64.encodeToString(bytes,android.util.Base64.NO_WRAP));f.remove("localFile")
            }
            val out=File("build/outputs/cierre-1.4.5/fotografias/examples/$id.json");out.parentFile!!.mkdirs();out.writeText(payload.toString(2))
        }
    }}
    @Test fun incompleteDraftCannotDeliverPhotos()=runBlocking {db().use {d->
        record(d,"incomplete");d.inventoryDao().updateRecord(d.inventoryDao().recordById("incomplete")!!.copy(status="DRAFT"));add(d,"incomplete",listOf("INICIO"))
        AutomaticSync.request(d,"SIC","incomplete",1000)
        assertFalse(AutomaticDelivery.validated(d,d.automaticSyncDao().get("SIC/incomplete")!!))
    }}
    @Test fun historicalNullAndGeneralShareOneStableCategoryOrder()=runBlocking(Dispatchers.IO) {db().use {d->
        record(d,"order-general");val first=add(d,"order-general",listOf("GENERAL")).single()
        d.inventoryDao().insertPhoto(first.copy(id="old-null-category",localPath=jpg("old-null-category"),originalPath=jpg("old-null-original"),photoIndex=2,photoCategory=null))
        AutomaticSync.request(d,"SIC","order-general",1000)
        val p=AutomaticDelivery.prepare(context,d,d.automaticSyncDao().get("SIC/order-general")!!)
        val files=p.getJSONArray("files");val meta=(0 until files.length()).map {files.getJSONObject(it)}.single {it.getString("key")=="metadata"}
        val photos=JSONObject(File(meta.getString("localFile")).readText()).getJSONArray("photos")
        assertEquals(listOf(1,2),(0 until photos.length()).map {photos.getJSONObject(it).getInt("categoryOrder")})
        assertTrue((0 until photos.length()).all {photos.getJSONObject(it).getString("category")=="GENERAL"})
        assertNull(d.inventoryDao().photoById("old-null-category")!!.photoCategory)
    }}
    @Test fun additiveMigration17To18ValidatesRoomAndKeepsOriginalPhotos()=runBlocking(Dispatchers.IO) {
        val name="photo5-migration-${System.nanoTime()}"
        fun open()=Room.databaseBuilder(context,InventoryDatabase::class.java,name).addMigrations(*allInventoryMigrations()).build()
        val room=open();record(room,"historical");val old=add(room,"historical",listOf("GENERAL")).single();room.close()
        // Reconstruct the actual historical schema, including the pre-19 pavement photo table.
        // Merely changing user_version on today's database causes duplicate-column migrations.
        val schema17=JSONObject(File("schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/17.json").readText())
            .getJSONObject("database").getJSONArray("entities")
        val pavementPhotos=(0 until schema17.length()).map {schema17.getJSONObject(it)}.single {it.getString("tableName")=="pavement_damage_photos"}
        android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath(name).path,null,0).use {sql->
            sql.execSQL("DROP TABLE photo_history")
            sql.execSQL("DROP TABLE pavement_damage_photos") // Empty in this fictitious SIC-only fixture.
            sql.execSQL(pavementPhotos.getString("createSql").replace("\u0024{TABLE_NAME}","pavement_damage_photos"))
            val indices=pavementPhotos.getJSONArray("indices")
            for(i in 0 until indices.length())sql.execSQL(indices.getJSONObject(i).getString("createSql").replace("\u0024{TABLE_NAME}","pavement_damage_photos"))
            sql.version=17
        }
        open().use {d->assertEquals(20,d.openHelper.writableDatabase.version);assertEquals(old,d.inventoryDao().snapshot("historical")!!.photos.single());assertTrue(d.inventoryDao().photoHistory("historical").isEmpty())}
        open().use {d->assertEquals(old,d.inventoryDao().snapshot("historical")!!.photos.single())}
    }
}
