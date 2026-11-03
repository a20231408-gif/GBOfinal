package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.delivery.sha256
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.pavement.PavementCoordinateDisplay
import com.tuempresa.inventariovial.pavement.PavementSic
import com.tuempresa.inventariovial.pavement.PavementSicFormat
import com.tuempresa.inventariovial.media.*
import com.tuempresa.inventariovial.road.SicTableOrder
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.export.*
import kotlinx.coroutines.runBlocking
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import org.robolectric.shadows.ShadowStatFs
import java.io.*
import java.util.Locale
import java.util.zip.ZipFile
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FieldCorrections20261023Test {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build()
    @Test fun exactConfirmedNamesDoNotInventGlobalNumbersAndRejectBadAckOrChangedBytes() {
        val raw=File(c.filesDir,"FICTICIO_MIRROR.jpg").apply{writeBytes(byteArrayOf(1,2,3))};val hash=sha256(raw.readBytes())
        val paths=mutableListOf<String>()
        for(n in 1..2) {
            val i=deliveryFixture();val p=JSONObject(i.preparedJson!!);val a=JSONObject(i.receiptJson!!)
            val key="SIC/FICTICIO_TABLET_$n";p.put("key",key);a.put("key",key)
            p.getJSONArray("files").getJSONObject(0).put("sha256",hash).put("localFile",raw.path)
            val folder="PE-28H/PUENTES/PUENTE $n";a.put("logicalPath",folder)
            a.getJSONArray("files").getJSONObject(0).put("sha256",hash).put("logicalPath","$folder/GENERAL/F01.jpg")
            val j=LocalExportJob("MIRROR_$n",key,"SIC","FICTICIO_TABLET_$n",2)
            val plan=DriveMirrorPlan.build(j,p,a);val f=plan.files.single()
            paths+=f.relativePath+f.name;assertEquals(DriveMirrorPlan.ROOT+"$folder/GENERAL/F01.jpg",paths.last())
            assertEquals(hash,f.sha256);assertEquals(plan,DriveMirrorPlan.build(j,p,a))
            assertTrue(runCatching{DriveMirrorPlan.build(j,p,JSONObject(a.toString()).put("revision",1))}.isFailure)
            raw.writeText("FICTICIO_CAMBIO");assertTrue(runCatching{DriveMirrorPlan.build(j,p,a)}.isFailure);raw.writeBytes(byteArrayOf(1,2,3))
            val old=f.copy(id="FICTICIO_OLD",sha256="d".repeat(64),state="READY",uri="content://ficticio/old")
            val archived=DriveMirrorPlan.preservePrevious(plan,listOf(old),2)
            assertTrue(archived.files.single().relativePath.startsWith(DriveMirrorPlan.ROOT+"_REVISIONES_CONFIRMADAS/R_2/"))
            assertEquals(old.sha256,"d".repeat(64));assertEquals(archived,DriveMirrorPlan.preservePrevious(plan,listOf(old),2))
            assertEquals(plan,DriveMirrorPlan.preservePrevious(plan,listOf(f.copy(state="READY")),2))
        }
        assertEquals(2,paths.distinct().size)
        assertTrue(runCatching{DriveMirrorPlan.relative("PE-28H/../OTRA")}.isFailure)
        assertTrue(runCatching{DriveMirrorPlan.relative("PE-28H//FOTO")}.isFailure)
    }
    @Test fun provisionalOfflineIdentityIsStableAndNoAckOrSequenceIsInvented()=runBlocking {db().use{d->
        val j=LocalExportJob("LOCAL","SIC/FICTICIO_UUID","SIC","FICTICIO_UUID",1)
        d.inventoryDao().insertRecord(testRecord("FICTICIO_UUID",sic="SIC-18",route="PE-28H"))
        val a=LocalDocumentLocation.resolve(d,j,"PE-28H","ALCANTARILLAS","ALCANTARILLA")
        assertTrue(a.base.startsWith(DriveMirrorPlan.ROOT+"PE-28H/ALCANTARILLAS/"));assertFalse(a.base.contains("FICTICIO_UUID"))
        assertNull(a.remotePath);assertEquals(a,LocalDocumentLocation.resolve(d,j,"PE-28H","ALCANTARILLAS","OTRO NOMBRE"))
        assertEquals(5,a.json().getInt("publicFolderNamingVersion"))
        assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
    @Test fun videoPublicNameRequiresItsOwnConfirmedIdentityNotOnlyParentAck() {
        val v=VideoEntity("FICTICIO_VIDEO","SCAP/FICTICIO","GENERAL","General","unused.mp4","video/mp4",3,"a".repeat(64),1,
            state="SYNCED",fileId="FICTICIO_VIDEO_FILE",uploadedBytes=3)
        assertNull(VideoNames.confirmedRelativePath(c,v))
        val ack=JSONObject().put("videoNamingVersion",2).put("videoNumber",1).put("elementNumber",2).put("categoryCode","GEN")
            .put("fileName","001_PUE2_GEN.mp4").put("categoryFolder","GENERAL").put("fileId",v.fileId)
        VideoNames.accept(c,v,ack)
        assertEquals("VIDEOS/GENERAL/001_PUE2_GEN.mp4",VideoNames.confirmedRelativePath(c,v))
        assertNull(VideoNames.confirmedRelativePath(c,v.copy(state="UPLOADING")))
        assertNull(VideoNames.confirmedRelativePath(c,v.copy(sha256="b".repeat(64))))
        assertNull(VideoNames.confirmedRelativePath(c,v.copy(retired=true)))
    }
    @Test fun documentStagingPriorityDoesNotMutateManifestOrStarveMedia() {
        val files=JSONArray().put(JSONObject().put("key","photo/a").put("mimeType","image/jpeg"))
            .put(JSONObject().put("key","metadata").put("mimeType","application/json"))
            .put(JSONObject().put("key","scap").put("mimeType",SicExcelWriter.XLSX_MIME))
            .put(JSONObject().put("key","photo/b").put("mimeType","image/jpeg"))
            .put(JSONObject().put("key","field").put("mimeType","application/pdf"))
        val before=files.toString();assertEquals(listOf(2,4,1,0,3),DocumentTransferOrder.indices(files));assertEquals(before,files.toString())
    }
    @Test fun actualHttpAdapterStagesDocumentsFirstAndReusesStagesAfterLostCommitResponse() {
        val dir=File(c.filesDir,"FICTICIO_HTTP").apply{mkdirs()}
        val photo=File(dir,"photo.jpg");val image=Bitmap.createBitmap(12,12,Bitmap.Config.ARGB_8888)
        photo.outputStream().use{image.compress(Bitmap.CompressFormat.JPEG,95,it)};image.recycle()
        val doc=File(dir,"SCAP.xlsx").apply{writeText("FICTICIO_STAGE_ONLY")}
        fun entry(key:String,f:File,mime:String)=JSONObject().put("key",key).put("path",f.name).put("localFile",f.path).put("mimeType",mime).put("sha256",sha256(f.readBytes()))
        val p=JSONObject().put("key","SCAP/FICTICIO_HTTP").put("kind","SCAP").put("recordId","FICTICIO_HTTP")
            .put("baseRevision",0).put("revision",1).put("contentHash","f".repeat(64))
            .put("files",JSONArray().put(entry("photo/f",photo,"image/jpeg")).put(entry("scap",doc,SicExcelWriter.XLSX_MIME)))
        val before=p.toString();val staged=mutableListOf<String>();var commits=0
        val client=OkHttpClient.Builder().addInterceptor{chain->
            val buffer=Buffer();chain.request().body!!.writeTo(buffer);val payload=JSONObject(buffer.readUtf8())
            val reply=if(payload.optString("action")=="stageRecordV3") {
                val f=payload.getJSONObject("file");staged+=f.getString("key")
                JSONObject().put("ok",true).put("key",payload.getString("key")).put("revision",1)
                    .put("fileKey",f.getString("key")).put("sha256",f.getString("sha256")).put("stageFileId","FICTICIO_STAGE_"+staged.size)
            } else {commits++;if(commits==1)throw IOException("FICTICIO_LOST_RESPONSE");JSONObject().put("ok",true)}
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("FICTICIO").body(reply.toString().toResponseBody("application/json".toMediaType())).build()
        }.build()
        val http=AutomaticDriveHttp(client,"https://example.invalid/isolated","FICTICIO_TOKEN")
        assertTrue(runCatching{http.post(p)}.isFailure);assertTrue(http.post(p).getBoolean("ok"))
        assertEquals(listOf("scap","photo/f"),staged);assertEquals(2,commits);assertEquals(before,p.toString())
    }
    @Test fun coordinatePresentationNeverRoundsUneditedStoredGeometry() {
        val x=1.234567891;assertEquals("1,23",PavementCoordinateDisplay.text(x,Locale.forLanguageTag("es-ES")))
        assertEquals(x,PavementCoordinateDisplay.value("1,23",x,false),0.0)
        assertEquals(2.75,PavementCoordinateDisplay.value("2,75",x,true),0.0)
        assertTrue(runCatching{PavementCoordinateDisplay.value("NaN",x,true)}.isFailure)
    }
    @Test fun sicRowsReallyAppearNumericallyOrderedInXlsxRegardlessOfRoadbedDirectionOrDate() {
        val cases=listOf("C" to ("0012" to 58.0),"A" to ("0000" to 301.0),"E" to ("0002" to 229.0),"B" to ("0000" to 820.0),"D" to ("0011" to 0.0))
        val rows=cases.mapIndexed{n,(id,pos)->val r=testRecord(id,pos.first,pos.second,route="PE-28H",roadbed=if(n%2==0)"CI"else"CD").copy(surveyDirection=if(n%2==0)"DECREASING"else"INCREASING",observations=id)
            InventoryRecordForExport(r,sic19=Sic19Entity(id,"08","2","1","1","1"))}
        assertEquals(listOf("A","B","E","D","C"),rows.map{it.record}.sortedWith(SicTableOrder.comparator).map{it.id})
        val out=ByteArrayOutputStream();SicExcelWriter.write(out,rows,SicExportFormat.SIC19,ExportHeading())
        val f=File(c.cacheDir,"SIC-19-ORDENADO-FICTICIO.xlsx").apply{writeBytes(out.toByteArray())}
        val w=TemplateWorkbook(out.toByteArray().inputStream())
        assertEquals(listOf("0000","0000","0002","0011","0012"),(14..18).map{w.cell(1,"C$it").textContent})
        val dest=File("../docs/campo-20261023/ejemplos/SIC-19-ORDENADO-FICTICIO.xlsx").apply{parentFile!!.mkdirs()};f.copyTo(dest,true)
    }
    @Test fun pavementSic15OrdersByRegistrationWithoutReinterpretingPrAsChronology() {
        val damages=listOf(visualDamage(22,1,.2,.2,1.0,1.0,1),visualDamage(22,2,1.2,.2,2.0,1.0,2))
        var sample=visualSample()
        for((n,d)in damages.withIndex())sample=sample.copy(sicDataJson=PavementSic.update(sample,PavementSicFormat.SIC15,d.id,
            mapOf("faja" to "1","prInicio" to if(n==0)"0012"else"0000","distanciaInicio" to "10","prFin" to "0012","distanciaFin" to "20","longitud" to "2","ancho" to "1","gravedad" to "2")))
        val before=sample.sicDataJson;val out=ByteArrayOutputStream();PavementSic.write(out,sample,damages,PavementSicFormat.SIC15)
        val w=TemplateWorkbook(out.toByteArray().inputStream())
        val prs=w.sheet(1).nodes("row").filter{it.getAttribute("r").toInt()>=3}.map{it.children().filter{cell->cell.localName=="c"}[3].textContent}
        assertEquals(listOf("0012","0000"),prs);assertEquals(before,sample.sicDataJson)
        File("../docs/campo-20261023/ejemplos/SIC-15-ORDENADO-FICTICIO.xlsx").apply{parentFile!!.mkdirs();writeBytes(out.toByteArray())}
    }
    @Test fun moreThan32PhotosExportLocalCompanionAndAllOriginalsWithoutRelaxingRemoteTemplate()=runBlocking {db().use{d->
        ShadowStatFs.registerStats(c.filesDir.path,1_000_000,900_000,900_000)
        val r=ScapRepository(d,ScapCatalog.load(c));val id=r.create("FICTICIO","TABLET_FICTICIA",null,ScapBridgeType.MODULAR)
        r.setField(id,"inspection","route","PE-28H")
        val f=File(c.filesDir,"FICTICIO_33.jpg");val b=Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888)
        f.outputStream().use{b.compress(Bitmap.CompressFormat.JPEG,90,it)};b.recycle()
        repeat(33){r.addPhoto(id,f.path,ScapRepository.PHOTO_CATEGORIES.first(),null)}
        val s=r.dao.snapshot(id)!!;val original=fileDigest(f);val before=ScapFieldBackup.capture(s).toString()
        assertTrue(ScapDelivery.exporter(c,s).review(s).errors.any{it.contains("32")})
        val j=LocalExportJob("FICTICIO_33", "SCAP/$id","SCAP",id,s.inspection.updatedAt,"ZIP")
        val plan=LocalExportBuilder.build(c,d,j);assertTrue(plan.problems.toString(),plan.problems.isEmpty())
        ZipFile(plan.files.single().source).use{z->val entries=z.entries().asSequence().toList()
            assertEquals(33,entries.count{it.name.contains("FOTOGRAFIAS/")})
            assertTrue(entries.any{it.name.endsWith("CONTINUACION_FOTOGRAFICA.html")})
            val book=entries.single{it.name.endsWith("SCAP_PROVISIONAL.xlsx")}
            val w=z.getInputStream(book).use{TemplateWorkbook(it)}
            assertEquals(32,w.parts.keys.count{it.startsWith("xl/media/scap_4_")})
        }
        assertEquals(original,fileDigest(f));assertEquals(before,ScapFieldBackup.capture(r.dao.snapshot(id)!!).toString());assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
    @Test fun mappingsUseStoredSourcesAndNeverOverwriteManualEditsAcrossFormats()=runBlocking{db().use{d->
        val r=ScapRepository(d,ScapCatalog.load(c));val id=r.create("FICTICIO","FICTICIO",null,ScapBridgeType.MODULAR)
        r.setField(id,"inspection","bridgeCode","FICTICIO-P");r.setField(id,"inspection","totalLengthM","18.75")
        r.setSicField(id,"SIC-17","dimension1LengthM","20.5")
        r.setField(id,"inspection","totalLengthM","19.25")
        val s=r.dao.snapshot(id)!!;val a=ScapToSicMapper.mapToSic17(s)
        assertEquals("20.5",a.fields.single{it.sicField=="dimension1LengthM"}.value)
        assertTrue(a.fields.single{it.sicField=="dimension1LengthM"}.notes.startsWith("REVISAR:"))
        for(map in listOf(ScapToSicMapper.mapToSic17A(s),ScapToSicMapper.mapToSic17B(s)))assertEquals("FICTICIO-P",map.fields.single{it.sicField=="bridgeCode"}.value)
        assertEquals(s,r.dao.snapshot(id));assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
}
