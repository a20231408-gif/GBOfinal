package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.media.*
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.ScapBridgeType
import com.tuempresa.inventariovial.scap.export.ScapFieldBackup
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import org.robolectric.shadows.ShadowStatFs
import java.io.File
import java.util.UUID

/** Actual Room, repository, local builder and drain; external storage and PdfDocument are test adapters. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,shadows=[SyntheticPdf146::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PublicLocalNamesTest {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build()
    @Before fun setup(){initializeIdleUiWorkers();ShadowStatFs.registerStats(c.filesDir.path,1_000_000,900_000,900_000)}
    private suspend fun bridge(d:InventoryDatabase,name:String="Correviento",code:String=""):String {
        val repo=ScapRepository(d,ScapCatalog.load(c));val id=repo.create("FICTICIO","FICTICIO",null,ScapBridgeType.MODULAR)
        repo.setField(id,"inspection","route","PE-28H");repo.setField(id,"inspection","singularityName",name)
        repo.setField(id,"inspection","bridgeCode",code);return id
    }
    private suspend fun job(d:InventoryDatabase,id:String,scope:String="FULL"):LocalExportJob {
        val rev=d.scapDao().inspection(id)!!.updatedAt
        return LocalCopies.request(c,d,"SCAP",id,rev,scope)
    }
    private fun base(plan:LocalPlan)=JSONObject(File(plan.files.single{it.name=="MANIFIESTO_LOCAL.json"}.source).readText()).getJSONObject("location").getString("localBase")
    private class Store:SharedDocumentStore {
        val data=linkedMapOf<String,ByteArray>();val paths=linkedMapOf<String,String>();var writes=0;var stop=false
        override fun reserve(file:LocalSharedFile,knownUri:Uri?):SharedDocumentStore.Slot {
            val path=file.relativePath+file.name;val uri="content://ficticio/"+Uri.encode(path);paths[uri]=path
            return SharedDocumentStore.Slot(Uri.parse(uri),data.containsKey(uri))
        }
        override fun checkSlot(slot:SharedDocumentStore.Slot,file:LocalSharedFile){require(paths[slot.uri.toString()]==file.relativePath+file.name)}
        override fun write(uri:Uri,source:File){data[uri.toString()]=source.readBytes();writes++}
        override fun digest(uri:Uri)=data.getValue(uri.toString()).let{it.size.toLong() to com.tuempresa.inventariovial.delivery.sha256(it)}
        override fun publish(slot:SharedDocumentStore.Slot,file:LocalSharedFile):Uri{if(stop){stop=false;throw CancellationException("FICTICIO_STOP")};return slot.uri}
    }
    @Test fun sectionBAndRealCode()=runBlocking {db().use{d->
        val id=bridge(d,code="001");val before=d.scapDao().snapshot(id)!!
        assertEquals("Correviento",before.values()["singularityName"]);assertEquals("Correviento",before.inspection.bridgeName)
        assertTrue(ScapCatalog.load(c).forSnapshot(before).fields.any{it.section=="B"&&it.key=="singularityName"})
        val plan=LocalExportBuilder.build(c,d,job(d,id));assertTrue(base(plan).endsWith("/Puente 001 - Correviento/"))
        assertEquals(ScapFieldBackup.capture(before).toString(),ScapFieldBackup.capture(d.scapDao().snapshot(id)!!).toString())
        assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
    @Test fun legacySic17AName()=runBlocking {db().use{d->
        val r=testRecord("FICTICIO_SIC17A",sic="SIC-17",route="PE-28H").copy(assetType="PUENTE",status="DRAFT")
        d.inventoryDao().insertRecord(r)
        val detail=Sic17AEntity(r.id,bridgeName="Santa Rosa",bridgeCode="P-002")
        d.inventoryDao().saveSic17A(detail)
        val before=d.inventoryDao().snapshot(r.id)!!
        val j=LocalExportJob("FICTICIO_SIC17A","SIC/${r.id}","SIC",r.id,r.updatedAt)
        val destination=LocalDocumentLocation.resolve(d,j,r.routeCode,"PUENTES","PUENTE")
        assertTrue(destination.base.endsWith("/Puente P-002 - Santa Rosa/"))
        assertEquals(before,d.inventoryDao().snapshot(r.id));assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
    @Test fun unnamedAndTwinNames()=runBlocking {db().use{d->
        val nameless=bridge(d,"","");assertTrue(base(LocalExportBuilder.build(c,d,job(d,nameless))).contains("Puente - PE-28H"))
        val a=bridge(d);val b=bridge(d)
        val first=base(LocalExportBuilder.build(c,d,job(d,a)));val second=base(LocalExportBuilder.build(c,d,job(d,b)))
        assertTrue(first.endsWith("/Puente - Correviento/"));assertTrue(second.endsWith("/Puente - Correviento (2)/"))
        assertEquals(first,base(LocalExportBuilder.build(c,d,job(d,a))))
        assertEquals(second,base(LocalExportBuilder.build(c,d,job(d,b))))
    }}
    @Test fun concurrentBindingsSurviveReopen()=runBlocking {
        val name="names.db";val parent="Download/GBO_Ruta_Clara/PE-28H/PUENTES/"
        fun open()=Room.databaseBuilder(c,InventoryDatabase::class.java,name).build()
        val names=open().use{d->coroutineScope{(1..8).map{n->async(Dispatchers.IO){PublicLocalFolderNames.reserve(d,"SCAP/FICTICIO_$n",parent,"Puente - Ñandú")}}.awaitAll()}}
        assertEquals(8,names.distinct().size)
        open().use{d->for(n in 1..8)assertEquals(names[n-1],PublicLocalFolderNames.reserve(d,"SCAP/FICTICIO_$n",parent,"Puente - Ñandú"))}
    }
    @Test fun renameKeepsHistoricalCopyAndNoRemoteIdentityChanges()=runBlocking {db().use{d->
        val id=bridge(d);val j=job(d,id);val old=LocalExportBuilder.build(c,d,j);val oldPlan=old.json()
        val store=Store();d.localFilesDao().saveJob(j.copy(manifestJson=oldPlan));LocalCopyDrain.run(c,d,store)
        val previous=store.data.mapValues{it.value.copyOf()};val revision=d.scapDao().inspection(id)!!.updatedAt
        val ack=JSONObject().put("ok",true).put("key",j.recordKey).put("revision",revision).put("folderId","FICTICIO_FOLDER").put("logicalPath","PE-28H/PUENTES/PUENTE 49")
        val remote=DriveSyncIntent(j.recordKey,"SCAP",id,revision,revision,"SYNCED",lastSyncedAt=1,receiptJson=ack.toString());d.automaticSyncDao().put(remote)
        ScapRepository(d,ScapCatalog.load(c)).setField(id,"inspection","singularityName","Santa Rosa")
        val next=job(d,id);val plan=LocalExportBuilder.build(c,d,next);assertTrue(base(plan).endsWith("/Puente - Santa Rosa/"))
        assertFalse(base(plan).contains("49"));assertEquals(remote,d.automaticSyncDao().get(j.recordKey))
        d.localFilesDao().saveJob(next.copy(manifestJson=plan.json()));LocalCopyDrain.run(c,d,store)
        for((k,v)in previous)assertArrayEquals(v,store.data.getValue(k))
        assertEquals(oldPlan,d.localFilesDao().job(j.id)!!.manifestJson)
        assertEquals("PE-28H/PUENTES/PUENTE 49",JSONObject(File(plan.files.single{it.name=="MANIFIESTO_LOCAL.json"}.source).readText()).getJSONObject("location").getString("remoteLogicalPath"))
    }}
    @Test fun replayAndInterruptedCopyKeepUrisAndBytes()=runBlocking {db().use{d->
        val id=bridge(d);val repo=ScapRepository(d,ScapCatalog.load(c))
        val image=File(c.filesDir,"foto-ficticia.jpg");val b=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888);image.outputStream().use{b.compress(Bitmap.CompressFormat.JPEG,95,it)};b.recycle()
        repo.addPhoto(id,image.path,ScapRepository.PHOTO_CATEGORIES.first(),null);repo.addSketch(id,"PLAN",image.path)
        val video=File(c.filesDir,"ficticio.mp4").apply{writeBytes(byteArrayOf(0,0,0,16,102,116,121,112))};val vh=fileDigest(video)
        VideoDatabase.get(c).videos().put(VideoEntity("FICTICIO_VIDEO","SCAP/$id","UPSTREAM","Aguas arriba",video.path,"video/mp4",vh.first,vh.second,1))
        val j=job(d,id);val original=fileDigest(image);val snapshot=ScapFieldBackup.capture(d.scapDao().snapshot(id)!!).toString()
        val store=Store();store.stop=true;assertTrue(runCatching{LocalCopyDrain.run(c,d,store)}.isFailure)
        LocalCopyDrain.run(c,d,store);assertEquals("READY",d.localFilesDao().job(j.id)!!.state)
        val files=d.localFilesDao().files(j.id);assertTrue(files.any{it.mimeType=="video/mp4"});assertTrue(files.any{it.mimeType.endsWith("spreadsheetml.sheet")});assertTrue(files.any{it.relativePath.contains("FOTOGRAFIAS")});assertTrue(files.any{it.relativePath.contains("CROQUIS")})
        val writes=store.writes;val count=store.data.size;LocalCopies.requestCurrent(c,d,"SCAP",id);LocalCopyDrain.run(c,d,store)
        assertEquals(writes,store.writes);assertEquals(count,store.data.size)
        for(f in d.localFilesDao().files(j.id))assertEquals(f.size to f.sha256,store.digest(Uri.parse(f.uri)))
        assertEquals(original,fileDigest(image));assertEquals(vh,fileDigest(video));assertEquals(snapshot,ScapFieldBackup.capture(d.scapDao().snapshot(id)!!).toString());assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
    @Test fun sic20AndPavementUseActualFields()=runBlocking {db().use{d->
        for((type,code,title)in listOf(Triple("MURO","14","Muro"),Triple("BADEN","12","Badén"),Triple("TUNEL","13","Túnel"))) {
            val r=testRecord("FICTICIO_$code",sic="SIC-20",route="PE-28C",pr="0010",distance=60.0).copy(assetType=type,status="DRAFT")
            d.inventoryDao().insertRecord(r)
            val j=LocalExportJob("FICTICIO_$code","SIC/${r.id}","SIC",r.id,r.updatedAt)
            val p=LocalExportBuilder.build(c,d,j);assertTrue(base(p).endsWith("/$title - PE-28C - PR 10+060/"));assertEquals(r,d.inventoryDao().recordById(r.id))
        }
        val s=visualSample().copy(id="FICTICIO_PAV",tramo="Tramo 3",muestra=2,updatedAt=1);d.pavementDao().saveSample(s)
        val p=LocalExportBuilder.build(c,d,LocalExportJob("FICTICIO_PAV","PAVEMENT/${s.id}","PAVEMENT",s.id,1))
        assertTrue(base(p).endsWith("/Pavimento rígido - Tramo 3 - Muestra 2/"));assertTrue(p.files.any{it.name.endsWith(".pdf")});assertTrue(p.files.any{it.name=="Datos.xlsx"})
        assertEquals(s,d.pavementDao().sample(s.id));assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
    @Test fun unicodeLongNamesAndReservedPaths()=runBlocking {db().use{d->
        val parent="Download/GBO_Ruta_Clara/PE-28H/PUENTES/"
        assertEquals("Ñandú - Águila",PublicLocalFolderNames.component("Ñandu\u0301 - Águila"))
        for(v in listOf("../..\\CON:<x>|?*\n","", " . ","NUL.txt","Ñ".repeat(10000))) {
            val n=PublicLocalFolderNames.component(v);assertTrue(n.isNotBlank());assertTrue(n.toByteArray().size<=96);assertFalse(n.contains('/'));assertFalse(n.contains('\\'));assertFalse(n=="..")
        }
        val a=PublicLocalFolderNames.reserve(d,"SCAP/A",parent,"Ñ".repeat(200));val b=PublicLocalFolderNames.reserve(d,"SCAP/B",parent,"Ñ".repeat(200));assertNotEquals(a,b);assertTrue(b.endsWith(" (2)"));assertTrue(b.toByteArray().size<=96)
        assertNotEquals(PublicLocalFolderNames.reserve(d,"SCAP/C",parent,"Puente - Rosa"),PublicLocalFolderNames.reserve(d,"SCAP/D",parent,"puente - rosa"))
    }}
    @Test fun explicitRegenerationKeepsLegacyPlanAndAllScopes()=runBlocking {db().use{d->
        val id=bridge(d);val first=job(d,id);val raw=File(c.filesDir,"anterior.txt").apply{writeText("FICTICIO ANTERIOR")};val digest=fileDigest(raw)
        val path="Download/GBO_Ruta_Clara/PE-28H/PUENTES/PUENTE_UUID_$id/REVISIONES/R_${first.revision}/"
        for(scope in listOf("FULL","PHOTOS","ZIP")) {
            val j=job(d,id,scope);val f=LocalSharedFile("FICTICIO_"+scope,j.id,j.recordKey,path,"anterior.txt","text/plain",digest.first,digest.second,raw.path,"content://anterior/$scope","READY")
            val plan=LocalPlan(listOf(f),emptyList()).json();d.localFilesDao().saveJob(j.copy(state="READY",manifestJson=plan));d.localFilesDao().saveFile(f)
            val next=job(d,id,scope);assertEquals(scope+"_READABLE_V2",next.scope);assertNotEquals(j.id,next.id);assertEquals(plan,d.localFilesDao().job(j.id)!!.manifestJson);assertEquals(f,d.localFilesDao().files(j.id).single())
            val built=LocalExportBuilder.build(c,d,next);assertEquals(2,built.publicNamingVersion)
            val expected=when(scope){"FULL"->"Documentos y medios";"PHOTOS"->"Fotografías";else->"Paquete ZIP"}
            assertTrue(LocalCopyPresentation.context(next,next.revision,true).contains(expected))
            assertTrue(built.files.all{!it.relativePath.contains("UUID")});assertEquals(next.id,job(d,id,scope).id)
            if(scope=="ZIP")assertTrue(built.files.single().name.endsWith(".zip"))
            if(scope=="PHOTOS")assertFalse(built.files.any{it.name.endsWith(".xlsx")})
        }
        assertEquals(digest,fileDigest(raw));assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
    @Test fun existingForeignPlanRetainsItsFolderAndDamagedBindingFailsClosed()=runBlocking {db().use{d->
        val parent="Download/GBO_Ruta_Clara/PE-28H/PUENTES/"
        val file=LocalSharedFile("OLD","OLD","SCAP/OLD",parent+"Puente - Rosa/REVISIONES/R_1/","old.json","application/json",1,"a".repeat(64),"FICTICIO")
        d.localFilesDao().saveJob(LocalExportJob("OLD","SCAP/OLD","SCAP","OLD",1,state="READY",manifestJson=LocalPlan(listOf(file),emptyList()).json()))
        assertEquals("Puente - Rosa (2)",PublicLocalFolderNames.reserve(d,"SCAP/NEW",parent,"Puente - Rosa"))
        d.localFilesDao().saveChoice(DeviceDeliveryChoice("LOCAL_PUBLIC_FOLDER_V2/DAMAGED","{}","LOCAL_PUBLIC_FOLDER_V2",0))
        assertTrue(runCatching{PublicLocalFolderNames.reserve(d,"SCAP/OTHER",parent,"Otro")}.isFailure)
        assertEquals(file,LocalPlan.parse(d.localFilesDao().job("OLD")!!.manifestJson!!).files.single())
    }}
    @Test fun manualDocumentsAndIndividualPhotosHaveReadableFolders()=runBlocking {db().use{d->
        val f=File(c.filesDir,"Resumen.xlsx").apply{writeText("FICTICIO DOCUMENTO")};val digest=fileDigest(f)
        LocalCopies.document(c,d,f,"SIC","application/octet-stream","PE-28H")
        LocalCopies.document(c,d,f,"SIC","application/octet-stream","PE-28H")
        assertEquals(1,d.localFilesDao().jobs().size);val p=LocalPlan.parse(d.localFilesDao().jobs().single().manifestJson!!)
        assertTrue(p.files.single().relativePath.endsWith("/Exportación - Resumen/"));assertEquals(digest,p.files.single().let{it.size to it.sha256})
        val photo=File(c.filesDir,"individual.jpg").apply{writeBytes(byteArrayOf(-1,-40,1,2))};LocalCopies.photo(c,d,photo)
        assertTrue(LocalPlan.parse(d.localFilesDao().jobs().single{it.kind=="PHOTO"}.manifestJson!!).files.single().relativePath.contains("/Fotografía individual/"))
        assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
}
