package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.media.*
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.export.*
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import org.robolectric.shadows.ShadowStatFs
import java.io.*
import java.util.UUID
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScapPhotoPanelTest {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private val evidence get()=File("../docs/scap-panel-oficial-20261023/evidencias").apply{mkdirs()}
    @Before fun space(){ShadowStatFs.registerStats(c.filesDir.path,1_000_000,900_000,900_000)}
    private fun image():File=File(c.filesDir,"PANEL_FICTICIO.jpg").apply{
        val bitmap=Bitmap.createBitmap(40,30,Bitmap.Config.ARGB_8888)
        outputStream().use{bitmap.compress(Bitmap.CompressFormat.JPEG,95,it)};bitmap.recycle()
    }
    private suspend fun populate(r:ScapRepository,named:Boolean=false):String {
        val id=r.create("FICTICIO","FICTICIO",null,ScapBridgeType.MODULAR);val img=image()
        r.setField(id,"inspection","route","PE-28H");r.setField(id,"inspection","sic17.roadbedCode","CD");r.setField(id,"inspection","progressive","10+100")
        if(named)r.setField(id,"inspection","singularityName","FICTICIO PANEL")
        for(n in listOf(20,15,8,3,1)){
            val photo=r.addPhotoRecord(id,ScapPhotoPanel.categories[n-1].label,"EVIDENCIA FICTICIA $n")
            r.replacePhoto(id,photo,img.path)
        };r.saveDraft(id);return id
    }
    @Test fun officialTwentyConceptsAreExactInBothCatalogsAndAllNamesAreHumanAndUnique() {
        val expected="""VISTA PANORAMICA LADO DERECHO
VISTA PANORAMICA LADO IZQUIERDO
VISTA PERFIL
ACCESO IZQUIERDO DEL PUENTE
ACCESO DERECHO DEL PUENTE
SEÑALES INFORMATIVAS ACCESO IZQUIERDO
SEÑALES INFORMATIVAS ACCESO DERECHO
SUPERFICIE DE RODADURA
IMAGEN AGUAS ARRIBA SUPERESTRUCTURA
IMAGEN AGUAS ABAJO SUPERESTRUCTURA
VEREDA, BARANDAS Y GUARDAVIA AGUAS ARRIBA
VEREDA, BARANDAS Y GUARDAVIA AGUAS ABAJO
TUBERIA DE DRENAJE AGUAS ARRIBA
TUBERIA DE DRENAJE AGUAS ABAJO
ESTRIBO IZQUIERDO
ESTRIBO DERECHO
PILARES
TABLERO VISTA INFERIOR
VISTA DE CAUCE AGUAS ARRIBA
VISTA DE CAUCE AGUAS ABAJO""".lines()
        assertEquals(expected,ScapPhotoPanel.categories.map{it.label})
        for(type in ScapBridgeType.entries)assertEquals(expected,ScapCatalog.load(c).forType(type).photoNames)
        assertEquals(expected,ScapCatalog.load(c).photoNames)
        assertEquals((1..20).toList(),ScapPhotoPanel.categories.map{it.order})
        assertEquals(20,ScapPhotoPanel.categories.map{it.folder}.distinct().size)
        assertEquals(20,ScapPhotoPanel.categories.map{it.shortName}.distinct().size)
        for(cat in ScapPhotoPanel.categories){assertTrue(cat.folder.matches(Regex("[0-9]{2}_[A-Z0-9_]+")));assertTrue(cat.fileName(1,"jpg",7).length<=80)}
        assertEquals("01_PAN_DER_001.jpg",ScapPhotoPanel.categories.first().fileName(1))
        assertEquals("18_PUE7_TABLERO_INF_001.mp4",ScapPhotoPanel.categories[17].fileName(1,"mp4",7))
    }
    @Test fun diskDraftReopensWithFivePositions()=runBlocking {
        // Keep native Windows SQLite/WAL paths short; do not change the production journal mode.
        val name="panel-${UUID.randomUUID().toString().take(8)}.db";var id="";var old:ScapInspectionSnapshot?=null
        File(evidence,"ROOM_REAPERTURA_FICTICIA.json").writeText(JSONObject().put("databasePathLength",c.getDatabasePath(name).absolutePath.length).put("nativeSqlite",true).put("inMemory",false).toString(2))
        Room.databaseBuilder(c,InventoryDatabase::class.java,name).build().use{db->val r=ScapRepository(db,ScapCatalog.load(c));id=populate(r);old=r.dao.snapshot(id)}
        Room.databaseBuilder(c,InventoryDatabase::class.java,name).build().use{db->val s=db.scapDao().snapshot(id)!!
            assertEquals(old,s);assertEquals(listOf(1,3,8,15,20),ScapPhotoPanel.ordered(s).map{ScapPhotoPanel.category(s,it)!!.order})
            assertEquals(old!!.photos.map{it.id}.toSet(),s.photos.map{it.id}.toSet());assertEquals(5,s.photos.map{it.photoIndex}.distinct().size)
            assertEquals(5,s.photos.count{it.photoCategory!!.startsWith("SCAP20_")});assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }
    @Test fun realScapWorkbookAndZipUseOfficialOrderAndNamesAndRetainOriginalJpegBytes()=runBlocking {
        Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use{db->
            val r=ScapRepository(db,ScapCatalog.load(c));val id=populate(r);val s=r.dao.snapshot(id)!!
            val bytes=ByteArrayOutputStream();ScapDelivery.exporter(c,s).write(bytes,s){File(it).readBytes()}
            val w=TemplateWorkbook(bytes.toByteArray().inputStream())
            for((index,n)in listOf(1,3,8,15,20).withIndex())assertEquals(ScapPhotoPanel.categories[n-1].label+": EVIDENCIA FICTICIA $n",w.cell(4,"D${11+index}").textContent)
            File(evidence,"SCAP_PANEL_01_03_08_15_20_FICTICIO.xlsx").writeBytes(bytes.toByteArray())
            val bundle=ScapDelivery.bundle(c,s);val paths=mutableListOf<String>()
            ZipFile(bundle).use{zip->
                val photos=zip.entries().asSequence().filter{it.name.startsWith("fotos/")}.toList();assertEquals(5,photos.size)
                for((entry,p)in photos.zip(ScapPhotoPanel.ordered(s))){paths+=entry.name;assertEquals(ScapPhotoPanel.photoPath(s,p),entry.name);assertArrayEquals(File(p.originalPath).readBytes(),zip.getInputStream(entry).readBytes())}
                assertEquals(paths.sorted(),paths);assertEquals(paths.size,paths.distinct().size)
            }
            File(evidence,"NOMBRES_ZIP_FICTICIO.txt").writeText(paths.joinToString("\n"))
            assertEquals(s,r.dao.snapshot(id));assertEquals(ScapDelivery.sha256(bundle),ScapDelivery.sha256(ScapDelivery.bundle(c,s)))
        }
    }
    @Test fun tabletPlanUsesSameNumberedFoldersWithoutUuidOrHashInMediaNamesAndPreservesItsSources()=runBlocking {
        Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use{db->
            val r=ScapRepository(db,ScapCatalog.load(c));val id=populate(r,true);val s=r.dao.snapshot(id)!!
            val plan=LocalExportBuilder.build(c,db,LocalExportJob("PANEL_LOCAL_FICTICIO","SCAP/$id","SCAP",id,s.inspection.updatedAt,scope="PHOTOS"))
            val photos=plan.files.filter{it.mimeType=="image/jpeg"};assertEquals(5,photos.size)
            for((f,p)in photos.zip(ScapPhotoPanel.ordered(s))){val path=ScapPhotoPanel.photoPath(s,p,root="FOTOGRAFIAS");assertTrue("category path: ${f.relativePath} expected $path",f.relativePath.endsWith(path.substringBeforeLast('/')+"/"));assertEquals("Official panel filename must remain unchanged",path.substringAfterLast('/'),f.name);assertTrue("name length: ${f.name}",f.name.length<=80);assertEquals(ScapDelivery.sha256(File(p.originalPath)),f.sha256);assertFalse(f.name.contains(p.id))}
            assertEquals(5,photos.map{it.name}.distinct().size)
            File(evidence,"PLAN_LOCAL_FICTICIO.json").writeText(plan.json())
            assertEquals(s,r.dao.snapshot(id));assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }
    @Test fun historicalMappingIsReadOnlyAndAmbiguousSidesNeverGetInvented()=runBlocking {
        Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use{db->
            val r=ScapRepository(db,ScapCatalog.load(c));val id=r.create("FICTICIO","FICTICIO",null)
            val p=r.addPhotoRecord(id,"HISTORICO");r.setField(id,p,"photoName","ACCESO IZQUIERDO")
            val original=r.dao.snapshot(id)!!;assertEquals("OTHER",original.photos.single().photoCategory)
            assertEquals(4,ScapPhotoPanel.category(original,original.photos.single())!!.order)
            assertEquals(original,r.dao.snapshot(id));assertEquals("ACCESO IZQUIERDO",original.values(p)["photoName"])
            for(label in listOf("AGUAS ARRIBA","AGUAS ABAJO","TUBERIA DE DRENAJE AGUAS ARRIBA-ABAJO","ALA IZQUIERDA AGUAS ARRIBA"))assertNull(label,ScapPhotoPanel.resolve("OTHER",label))
        }
    }
    @Test fun multiplePhotosMoveOnlyWithinTheirOfficialCategoryWithoutReassigningOthers()=runBlocking {
        Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use{db->
            val r=ScapRepository(db,ScapCatalog.load(c));val id=populate(r)
            val first=r.dao.snapshot(id)!!.photos.single{it.photoCategory=="SCAP20_01"}
            val second=r.addPhotoRecord(id,ScapPhotoPanel.categories.first().label)
            val before=r.dao.snapshot(id)!!;r.movePanelPhoto(id,second,-1);val after=r.dao.snapshot(id)!!
            assertEquals(listOf(second,first.id),ScapPhotoPanel.ordered(after).filter{it.photoCategory=="SCAP20_01"}.map{it.id})
            assertEquals(before.photos.filter{it.photoCategory!="SCAP20_01"}.toSet(),after.photos.filter{it.photoCategory!="SCAP20_01"}.toSet())
        }
    }
    @Test fun officialPhotosAndVideosWaitBeforeAnyPostWhenServerDoesNotAnnouncePanelTwo() {
        var posts=0
        val client=OkHttpClient.Builder().addInterceptor{chain->
            if(chain.request().method!="GET")posts++
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("FICTICIO").body("""{"ok":true,"syncProtocol":3,"pathVersion":4,"stableFamilyNumbering":true,"photoCategoryVersion":1,"readablePhotoNamingVersion":2,"videoProtocol":1,"videoScope":"SCAP_ONLY","readableVideoNamingVersion":2}""".toResponseBody()).build()
        }.build()
        val p=JSONObject().put("kind","SCAP").put("files",JSONArray().put(JSONObject().put("photoCategory","SCAP20_01")))
        val http=AutomaticDriveHttp(client,"https://example.invalid/ficticio","")
        assertTrue(runCatching{http.reservePhotoNames(p)}.exceptionOrNull() is SyncPending)
        assertTrue(runCatching{http.post(p)}.exceptionOrNull() is SyncPending)
        val video=VideoEntity("FICTICIO_VIDEO","SCAP/FICTICIO","SCAP20_18",ScapPhotoPanel.categories[17].label,"unused","video/mp4",1,"a".repeat(64),1)
        val transport=VideoHttp(client,"https://example.invalid/ficticio","");assertTrue(transport.available())
        assertTrue(runCatching{transport.begin(video,1)}.exceptionOrNull() is VideoPanelBackendRequired);assertEquals(0,posts)
    }
    @Test fun videoAcknowledgementRequiresTheExactOfficialCategoryFolderNameAndIdentity() {
        val cat=ScapPhotoPanel.categories[17]
        val v=VideoEntity("FICTICIO_VIDEO","SCAP/FICTICIO",cat.key,cat.label,"unused","video/mp4",3,"a".repeat(64),1)
        val ack=JSONObject().put("videoNamingVersion",2).put("videoNumber",1).put("elementNumber",7).put("categoryCode",cat.key).put("categoryFolder",cat.folder).put("fileName",cat.fileName(1,"mp4",7)).put("fileId","FICTICIO_FILE")
        VideoNames.accept(c,v,ack);assertEquals("VIDEOS/${cat.folder}/18_PUE7_TABLERO_INF_001.mp4",VideoNames.confirmedRelativePath(c,v.copy(state="SYNCED",uploadedBytes=3,fileId="FICTICIO_FILE")))
        assertTrue(runCatching{VideoNames.validate(JSONObject(ack.toString()).put("categoryFolder",ScapPhotoPanel.categories[18].folder),v)}.isFailure)
        assertTrue(runCatching{VideoNames.validate(JSONObject(ack.toString()).put("videoNamingVersion",1),v)}.isFailure)
    }
    @Test fun officialPhotoReservationsPersistAndRejectWrongPositionBridgeOrZeroCounter() {
        val cat=ScapPhotoPanel.categories.first()
        val p=JSONObject().put("kind","SCAP").put("key","SCAP/FICTICIO_RESERVA").put("recordId","FICTICIO_RESERVA").put("identityId","FICTICIO_PUENTE")
            .put("revision",1).put("routeCode","PE-28H").put("familyPath","PUENTES").put("family","PUENTE").put("pathVersion",4)
            .put("files",JSONArray().put(JSONObject().put("key","photo/FICTICIO_FOTO").put("photoCategory",cat.key).put("path","fotos/01_PAN_DER_001.jpg")))
        fun ack(name:String)=JSONObject().put("ok",true).put("photoNamingVersion",2).put("key",p.getString("key")).put("revision",1)
            .put("identityId","FICTICIO_PUENTE").put("folderId","FICTICIO_FOLDER").put("logicalPath","PE-28H/PUENTES/PUENTE 7")
            .put("photos",JSONArray().put(JSONObject().put("id","FICTICIO_FOTO").put("category",cat.key).put("name",name).put("historical",false)))
        for(name in listOf("02_PUE7_PAN_DER_001.jpg","01_PUE8_PAN_DER_001.jpg","01_PUE7_PAN_DER_000.jpg"))assertTrue(name,runCatching{ReadablePhotoNames.remember(c,p,ack(name))}.isFailure)
        val valid=cat.fileName(1,"jpg",7);ReadablePhotoNames.remember(c,p,ack(valid));ReadablePhotoNames.remember(c,p,ack(valid))
        assertEquals(valid,ReadablePhotoNames.name(c,"FICTICIO_FOTO",cat.key))
        assertTrue(runCatching{ReadablePhotoNames.remember(c,p,ack(cat.fileName(2,"jpg",7)))}.isFailure)
    }
    @Test fun officialVideoTransportNegotiatesPanelTwoAndValidatesItsNormalV1AckWithoutARealNetwork() {
        val cat=ScapPhotoPanel.categories[17]
        val v=VideoEntity("FICTICIO_HTTP_VIDEO","SCAP/FICTICIO",cat.key,cat.label,"unused","video/mp4",3,"a".repeat(64),1)
        var posts=0
        val client=OkHttpClient.Builder().addInterceptor{chain->
            val result=if(chain.request().method=="GET")JSONObject().put("ok",true).put("videoProtocol",1).put("videoScope","SCAP_ONLY").put("readableVideoNamingVersion",2).put("scapPhotoPanelVersion",2)
            else {posts++;JSONObject().put("ok",true).put("videoProtocol",1).put("parentKey",v.parentKey).put("mediaId",v.id).put("sha256",v.sha256).put("sizeBytes",3).put("complete",true)
                .put("videoNamingVersion",2).put("videoNumber",1).put("elementNumber",7).put("categoryCode",cat.key).put("categoryFolder",cat.folder).put("fileName",cat.fileName(1,"mp4",7)).put("fileId","FICTICIO_FILE")}
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("FICTICIO").body(result.toString().toResponseBody()).build()
        }.build()
        val http=VideoHttp(client,"https://example.invalid/ficticio","");assertTrue(http.available())
        assertEquals("18_PUE7_TABLERO_INF_001.mp4",http.begin(v,1).getString("fileName"));assertTrue(http.finish(v.copy(fileId="FICTICIO_FILE",parentRevision=1)).getBoolean("complete"));assertEquals(2,posts)
    }
    @Test fun officialLocalVideoPlanKeepsCategoryAndExistingElementAssociationWithoutRenamingPrivateOriginal()=runBlocking {
        Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use{db->
            val r=ScapRepository(db,ScapCatalog.load(c));val id=populate(r,true);var s=r.dao.snapshot(id)!!
            val code=r.catalog.forSnapshot(s).elements.first().code
            val photo=r.addPhotoRecord(id,ScapPhotoPanel.categories[14].label,elementCode=code);assertEquals(code,r.dao.snapshot(id)!!.photos.single{it.id==photo}.scapElementCode)
            s=r.dao.snapshot(id)!!
            val cat=ScapPhotoPanel.categories[17];val raw=File(c.filesDir,"FICTICIO_VIDEO_ORIGINAL.mp4").apply{writeText("SYNTHETIC_BYTES_ONLY_NOT_A_REAL_VIDEO")}
            val v=VideoEntity("FICTICIO_LOCAL_VIDEO","SCAP/$id",cat.key,cat.label,raw.path,"video/mp4",raw.length(),VideoPolicy.hash(raw),1)
            VideoDatabase.get(c).videos().put(v)
            val plan=LocalExportBuilder.build(c,db,LocalExportJob("FICTICIO_VIDEO_PLAN","SCAP/$id","SCAP",id,s.inspection.updatedAt))
            val saved=plan.files.single{it.mimeType=="video/mp4"};assertEquals("18_TABLERO_INF_001.mp4",saved.name);assertTrue(saved.relativePath.endsWith("VIDEOS/18_TABLERO_VISTA_INFERIOR/"))
            assertEquals(v.sha256,saved.sha256);assertEquals(v,VideoDatabase.get(c).videos().get(v.id));assertEquals(v.sha256,VideoPolicy.hash(raw));assertEquals("FICTICIO_VIDEO_ORIGINAL.mp4",raw.name)
            File(evidence,"PLAN_VIDEO_FICTICIO_SOLO_BYTES.json").writeText(plan.json())
        }
    }
    @Test fun alternateScapAckValidatorUsesTheSameOfficialPathsAsItsExportedBundle()=runBlocking {
        Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use{db->
            val r=ScapRepository(db,ScapCatalog.load(c));val id=populate(r);val s=r.dao.snapshot(id)!!;val hash="b".repeat(64)
            val paths=listOf("SCAP.xlsx","manifest.json")+ScapPhotoPanel.ordered(s).map{ScapPhotoPanel.photoPath(s,it)}
            val ack=JSONObject().put("ok",true).put("protocol",1).put("recordKind","SCAP").put("scapInspectionId",id).put("revision",s.inspection.updatedAt).put("sha256",hash).put("fileId","FICTICIO_FILE").put("folderId","FICTICIO_FOLDER")
                .put("files",JSONArray(paths.mapIndexed{i,p->JSONObject().put("path",p).put("fileId","FICTICIO_$i").put("folderId","FICTICIO_FOLDER")}))
            assertTrue(ScapDriveSync.validAck(ack,s,hash))
            ack.getJSONArray("files").getJSONObject(2).put("path","fotos/INCORRECTO.jpg");assertFalse(ScapDriveSync.validAck(ack,s,hash))
        }
    }
}
