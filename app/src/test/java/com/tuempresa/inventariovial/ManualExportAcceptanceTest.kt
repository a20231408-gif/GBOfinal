package com.tuempresa.inventariovial

import android.app.Application
import android.content.*
import android.content.pm.ProviderInfo
import android.net.Uri
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.scap.export.*
import com.tuempresa.inventariovial.scap.domain.ScapPhotoPanel
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import org.robolectric.shadows.*
import java.io.File

/** Actual Android document-provider calls against synthetic files, never a device or Google. */
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ManualExportAcceptanceTest {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private lateinit var provider:SicDocumentsProvider
    private val authority="com.android.externalstorage.documents"
    private val tree get()=DocumentsContract.buildTreeDocumentUri(authority,"root")
    @Before fun init(){
        initializeIdleUiWorkers();ShadowStatFs.registerStats(c.filesDir.path,1_000_000,900_000,900_000)
        provider=SicDocumentsProvider().apply{attachInfo(c,ProviderInfo().apply{authority=this@ManualExportAcceptanceTest.authority;exported=true;grantUriPermissions=true;readPermission="android.permission.MANAGE_DOCUMENTS";writePermission="android.permission.MANAGE_DOCUMENTS"})}
        ShadowContentResolver.registerProviderInternal(authority,provider)
        c.contentResolver.takePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
    private fun destination(name:String):Uri=DocumentsContract.buildDocumentUri(authority,provider.createDocument("root",SicExcelWriter.XLSX_MIME,name))
    private fun workbook()=File(c.filesDir,"FICTICIO.xlsx").apply{outputStream().use{PavementExcel.workbook(it,listOf("Datos" to listOf(listOf("FICTICIO"),listOf("valor",15.0))))}}

    @Test fun saveAsUsesEditedNameAndExactBytesAndNeverProgramsDelivery()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val document=ManualDocumentExport.freeze(c,workbook(),"SIC",SicExcelWriter.XLSX_MIME,"SIC-18.xlsx")
            val uri=destination("Nombre elegido por el ingeniero.xlsx")
            assertEquals(uri,ManualDocumentExport.save(c,db,document,uri))
            assertEquals(document.size to document.sha256,AndroidSharedDocumentStore(c).digest(uri))
            assertEquals("Nombre elegido por el ingeniero.xlsx",db.localFilesDao().filesForRecord(db.localFilesDao().jobs().single().recordKey).single().name)
            assertTrue(db.automaticSyncDao().all().isEmpty());assertEquals(document,ManualDocument.parse(document.json()))
        }
    }
    @Test fun unrelatedNonemptyDestinationIsNeverOverwritten()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val uri=destination("Ocupado.xlsx");val original="ARCHIVO AJENO FICTICIO".toByteArray()
            provider.docs.getValue(DocumentsContract.getDocumentId(uri)).file.writeBytes(original)
            val p=ManualDocumentExport.freeze(c,workbook(),"SIC",SicExcelWriter.XLSX_MIME,"SIC-18.xlsx")
            assertTrue(runCatching{ManualDocumentExport.save(c,db,p,uri)}.isFailure)
            assertArrayEquals(original,provider.docs.getValue(DocumentsContract.getDocumentId(uri)).file.readBytes())
            assertTrue(db.localFilesDao().jobs().isEmpty());assertTrue(File(p.source).isFile)
        }
    }
    @Test fun editedSourceAndMalformedXlsxFailBeforeWriting()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val p=ManualDocumentExport.freeze(c,workbook(),"SIC",SicExcelWriter.XLSX_MIME,"SIC-18.xlsx")
            File(p.source).appendText("CAMBIO FICTICIO");val uri=destination("Vacio.xlsx")
            assertTrue(runCatching{ManualDocumentExport.save(c,db,p,uri)}.isFailure)
            assertEquals(0L,provider.docs.getValue(DocumentsContract.getDocumentId(uri)).file.length())
            val broken=File(c.filesDir,"ROTO.xlsx").apply{writeText("No es ZIP")}
            assertTrue(runCatching{ManualDocumentExport.freeze(c,broken,"SIC",SicExcelWriter.XLSX_MIME,"SIC-18.xlsx")}.isFailure)
        }
    }
    @Test fun scapSuggestedNameAndIncompleteWorkbookKeepActualCapturedDataAndDraftStatus()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val repo=SicConsolidationFixtures.repo(c,db);val id=SicConsolidationFixtures.bridge(repo,"PUENTE QUEBRADA",route="PE-3N")
            val original=repo.dao.snapshot(id)!!;assertEquals("SCAP - PE-3N - PUENTE QUEBRADA.xlsx",ScapDocumentNames.excel(original))
            val p=ScapLocalDocuments.prepare(c,original)
            assertTrue(p.notice.startsWith("BORRADOR"));assertTrue(p.notice.contains("pendientes"));assertFalse(p.name.contains("PROVISIONAL"))
            val w=TemplateWorkbook(File(p.source).inputStream());assertTrue(w.cell(1,"C4").textContent.contains("BORRADOR"))
            assertTrue(w.cell(1,"C4").textContent.contains("PUENTE QUEBRADA"));ManualDocumentExport.verifyXlsx(File(p.source))
            assertEquals(original,repo.dao.snapshot(id));assertTrue(db.automaticSyncDao().all().isEmpty())
            // A historical COMPLETE flag alone never turns incomplete capture into final output.
            val alleged=original.copy(inspection=original.inspection.copy(status="COMPLETE"))
            assertTrue(ScapLocalDocuments.prepare(c,alleged).notice.startsWith("BORRADOR"))
        }
    }
    @Test fun bridgeFolderHasCurrentScapMediaNoIndividualSicAndKeepsPrivateGeneration()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val repo=SicConsolidationFixtures.repo(c,db);val id=SicConsolidationFixtures.bridge(repo,"FICTICIO CARPETA")
            for(n in 1..3){
                val image=File(c.filesDir,"fixture/$n.jpg").apply{parentFile!!.mkdirs()}
                val bitmap=android.graphics.Bitmap.createBitmap(24,16,android.graphics.Bitmap.Config.ARGB_8888)
                image.outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,95,it)};bitmap.recycle()
                val photo=repo.addPhotoRecord(id,ScapPhotoPanel.categories[n-1].label,"FICTICIO $n");repo.replacePhoto(id,photo,image.path)
            }
            val sketch=File(c.filesDir,"fixture/croquis.png");val bitmap=android.graphics.Bitmap.createBitmap(20,20,android.graphics.Bitmap.Config.ARGB_8888)
            sketch.outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
            repo.addSketch(id,com.tuempresa.inventariovial.scap.data.ScapRepository.SKETCH_TYPES.first(),sketch.path)
            val original=repo.dao.snapshot(id)!!
            val originals=(original.photos.map{File(it.originalPath)}+original.sketches.map{File(it.localUri)}).associate{it.path to fileDigest(it)}
            val files=ScapFolderExport.save(c,db,original,tree)
            assertTrue(files.any{it.name==ScapDocumentNames.excel(original)})
            assertEquals(3,files.count{it.mimeType=="image/jpeg"});assertEquals(1,files.count{it.mimeType=="image/png"})
            for(file in files)assertEquals(file.size to file.sha256,AndroidSharedDocumentStore(c,tree).digest(Uri.parse(file.uri)))
            assertFalse(files.any{it.mimeType=="application/json"||RecordDocuments.sicFormat(it)!=null||it.name.contains("PROVISIONAL")})
            val job=db.localFilesDao().jobs().single{it.scope=="SAF_TREE"}
            assertTrue(File(LocalExportBuilder.work(c,job),"MANIFIESTO_LOCAL.json").isFile)
            val before=provider.docs.size;val again=ScapFolderExport.save(c,db,original,tree)
            assertEquals(files.map{it.uri},again.map{it.uri});assertEquals(before,provider.docs.size)
            assertTrue(db.automaticSyncDao().all().isEmpty());assertEquals(original,repo.dao.snapshot(id))
            originals.forEach{(path,digest)->assertEquals(digest,fileDigest(File(path)))}
            assertNull(c.getSharedPreferences("gbo_local_copies",0).getString("legacyTree",null))
        }
    }
    @Test fun interruptedTreeJobNeverFallsBackToDownloadsOrSchedulesDrive()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            db.localFilesDao().saveJob(LocalExportJob("FICTICIO_TREE","SCAP/FICTICIO","SCAP","FICTICIO",1,scope="SAF_TREE",state="RUNNING"))
            LocalCopyDrain.run(c,db,SicTestStorage())
            assertEquals("PARTIAL",db.localFilesDao().job("FICTICIO_TREE")!!.state)
            assertTrue(db.localFilesDao().files("FICTICIO_TREE").isEmpty());assertTrue(db.automaticSyncDao().all().isEmpty())
            assertTrue(runCatching{LocalCopies.retry(c,db,"FICTICIO_TREE")}.isFailure)
        }
    }
    @Test fun manualBridgeConsolidatePreservesRowsAndDoesNotReplacePublicWorkbook()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val r=SicConsolidationFixtures.repo(c,db);val a=SicConsolidationFixtures.bridge(r,"FICTICIO A");SicConsolidationFixtures.bridge(r,"FICTICIO B","10+200")
            val storage=SicTestStorage()
            for(format in listOf("SIC-17","SIC-17A","SIC-17B")){
                val current=LocalSicConsolidates.update(c,db,"PE-28H",format,storage)
                val bytes=current.file.readBytes();val public=storage.digest(current.uri)
                val manual=LocalSicConsolidates.prepareManual(c,db,"PE-28H",format)
                assertEquals(2,SicRouteWorkbook.inspect(File(manual.source).readBytes(),format))
                assertEquals(public,storage.digest(current.uri));assertArrayEquals(bytes,current.file.readBytes())
                val again=LocalSicConsolidates.prepareManual(c,db,"PE-28H",format)
                assertEquals(2,SicRouteWorkbook.inspect(File(again.source).readBytes(),format))
            }
            r.setSicField(a,"SIC-17A","bridgeName","FICTICIO A EDITADO")
            val edited=LocalSicConsolidates.prepareManual(c,db,"PE-28H","SIC-17A")
            assertEquals(2,SicRouteWorkbook.inspect(File(edited.source).readBytes(),"SIC-17A"))
            assertTrue(TemplateWorkbook(File(edited.source).inputStream()).sheet(1).documentElement.textContent.contains("FICTICIO A EDITADO"))
            assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }
}
