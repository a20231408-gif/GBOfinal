package com.tuempresa.inventariovial

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.ProviderInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.export.*
import com.tuempresa.inventariovial.scap.ui.*
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.*
import org.robolectric.shadows.*
import java.io.File

/** Actual editor -> SAF publication -> Room -> in-app document screen -> exact ACTION_VIEW.
 * The provider is synthetic; these tests do not claim physical DocumentsUI or OPPO acceptance. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,qualifiers="w360dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScapFieldJourneyUiTest {
    @get:Rule val ui=createAndroidComposeRule<ComponentActivity>()
    private val tree=DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents","root")
    private lateinit var provider:SicDocumentsProvider
    @Before fun init(){
        initializeIdleUiWorkers();ShadowStatFs.registerStats(ui.activity.filesDir.path,1_000_000,900_000,900_000)
        provider=SicDocumentsProvider().apply{changeFolderDescendantIds=true;attachInfo(ui.activity,ProviderInfo().apply{
            authority="com.android.externalstorage.documents";exported=true;grantUriPermissions=true
            readPermission="android.permission.MANAGE_DOCUMENTS";writePermission="android.permission.MANAGE_DOCUMENTS"
        })};ShadowContentResolver.registerProviderInternal("com.android.externalstorage.documents",provider)
        val media=SicDownloadsProvider().apply{attachInfo(ui.activity,ProviderInfo().apply{authority="media"})}
        ShadowContentResolver.registerProviderInternal("media",media)
    }
    private fun capture(name:String){ui.runOnIdle{
        val v=ui.activity.window.decorView;val b=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888)
        v.draw(Canvas(b));val dir=System.getenv("GBO_TEST_EVIDENCE")?.let(::File) ?: File(ui.activity.cacheDir,"synthetic-ui")
        dir.mkdirs();File(dir,"$name.png").outputStream().use{assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,it))};b.recycle()
    }}
    private fun click(text:String)=ui.onNodeWithText(text).performScrollTo().performClick()
    @Test fun threeBridgesUseTheirOwnDocumentsAcrossRoomReopenGrantRenewalAndRename()=runBlocking {
        val c=ui.activity;val cat=ScapCatalog.load(c)
        fun open()=Room.databaseBuilder(c,InventoryDatabase::class.java,"field-journey.db").build()
        var db=open();var repo=SicConsolidationFixtures.repo(c,db)
        val ids=listOf(SicConsolidationFixtures.bridge(repo,"PRUEBA 5",route="28H"),
            SicConsolidationFixtures.bridge(repo,"PRUEBA 6",route="PE-3NG"),SicConsolidationFixtures.bridge(repo,"PRUEBA 7",route="PE-3NG"))
        val photo=File(c.filesDir,"fictitious-original.jpg");val image=Bitmap.createBitmap(18,18,Bitmap.Config.ARGB_8888)
        photo.outputStream().use{image.compress(Bitmap.CompressFormat.JPEG,95,it)};image.recycle()
        val photoId=repo.addPhotoRecord(ids[2],"GENERAL","FICTICIO");repo.replacePhoto(ids[2],photoId,photo.path)
        val originalHash=fileDigest(photo)
        // An unrelated historical pending record is a canary: local actions must not alter it.
        db.automaticSyncDao().put(com.tuempresa.inventariovial.core.DriveSyncIntent("SIC/FICTITIOUS_PENDING","SIC","FICTITIOUS_PENDING",7,state="QUEUED"))
        db.automaticSyncDao().put(com.tuempresa.inventariovial.core.DriveSyncIntent("SIC/FICTITIOUS_ACK","SIC","FICTITIOUS_ACK",4,syncedRevision=4,state="SYNCED",receiptJson="FICTITIOUS_PRESERVED_ACK"))
        val beforeOutbox=db.automaticSyncDao().all()
        var scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate);var ctl=ScapController(c,db,scope)
        var selected by mutableStateOf(ids.first());var visible by mutableStateOf(true);var generation by mutableIntStateOf(0)
        val models=androidx.lifecycle.ViewModelStore();val vm=InventoryViewModel(ApplicationProvider.getApplicationContext<Application>());models.put("journey",vm)
        ui.setContent{MaterialTheme{if(visible)key(generation,selected){
            val observed by repo.dao.observe(selected).collectAsState(null)
            val pending by ctl.pending.collectAsState();val error by ctl.error.collectAsState()
            observed?.let{ScapEditor(it,ctl,cat.forSnapshot(it),vm,"Guardado",pending,error)}
        }}}
        fun editor(id:String){ui.runOnIdle{selected=id};ui.waitUntil(20_000){ui.onAllNodesWithText("Exportar SCAP").fetchSemanticsNodes().size==1}}
        fun grant(){ui.waitUntil(10_000){shadowOf(c).peekNextStartedActivityForResult()!=null}
            val r=shadowOf(c).nextStartedActivityForResult;assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE,r.intent.action)
            assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE,shadowOf(c).nextStartedActivity.action)
            ui.runOnIdle{c.activityResultRegistry.dispatchResult(r.requestCode,Activity.RESULT_OK,Intent().setData(tree).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION))}}
        suspend fun export(id:String,needsGrant:Boolean=false):LocalSharedFile {
            editor(id);ui.onAllNodesWithText("Exportar SCAP").assertCountEquals(1)
            ui.onNodeWithText("Guardar en carpeta del puente").assertDoesNotExist();ui.onNodeWithText("Exportar carpeta del puente").assertDoesNotExist()
            ui.onNodeWithText("Exportar SCAP").performClick()
            ui.waitUntil(10_000){ui.onAllNodesWithText("Nombre del archivo SCAP").fetchSemanticsNodes().isNotEmpty()}
            click("Guardar");if(needsGrant)grant()
            ui.waitUntil(30_000){ui.onAllNodesWithText("ARCHIVO GUARDADO",substring=true).fetchSemanticsNodes().isNotEmpty()}
            val s=repo.dao.snapshot(id)!!;val name=ScapDocumentNames.excel(s)
            val f=db.localFilesDao().filesForRecord("SCAP/$id").last{it.name==name&&it.state=="READY"}
            assertEquals(f.size to f.sha256,AndroidSharedDocumentStore(c,tree).digest(Uri.parse(f.uri)))
            val wb=TemplateWorkbook(File(f.source).inputStream());assertTrue(wb.cell(1,"C4").textContent.contains(ScapDocumentNames.bridge(s)))
            assertTrue(f.relativePath.endsWith("/${ScapDocumentNames.route(s)}/PUENTES/Puente ${ScapDocumentNames.bridge(s)}/"))
            click("Abrir Excel");ui.waitUntil(10_000){shadowOf(c).peekNextStartedActivity()!=null}
            val intent=shadowOf(c).nextStartedActivity;assertEquals(Intent.ACTION_VIEW,intent.action);assertEquals(Uri.parse(f.uri),intent.data)
            assertEquals(-1,shadowOf(c).nextStartedActivityForResult.requestCode)
            assertNull(shadowOf(c).peekNextStartedActivityForResult());return f
        }
        suspend fun documents(id:String):LocalSharedFile {
            editor(id);ui.onAllNodesWithText("Ver documentos").onFirst().performClick()
            ui.onNodeWithText("Documentos del puente").assertExists()
            val s=repo.dao.snapshot(id)!!;val name=ScapDocumentNames.excel(s)
            ui.waitUntil(20_000){ui.onAllNodesWithText("ARCHIVO GUARDADO · $name").fetchSemanticsNodes().isNotEmpty()}
            ui.onNodeWithText("ABRIR UBICACIÓN DISPONIBLE").assertDoesNotExist()
            val f=db.localFilesDao().filesForRecord("SCAP/$id").last{it.name==name&&it.state=="READY"}
            ui.onNode(hasText("ABRIR ARCHIVO") and hasAnyAncestor(hasTestTag("saved-file-${f.id}"))).performScrollTo().performClick()
            ui.waitUntil(10_000){shadowOf(c).peekNextStartedActivity()!=null};val intent=shadowOf(c).nextStartedActivity
            assertEquals(Intent.ACTION_VIEW,intent.action);assertEquals(Uri.parse(f.uri),intent.data)
            assertEquals(-1,shadowOf(c).nextStartedActivityForResult.requestCode)
            if(id==ids[2]) {
                val picture=db.localFilesDao().filesForRecord("SCAP/$id").last{it.state=="READY"&&it.mimeType=="image/jpeg"&&!it.name.startsWith(".Historial - ")}
                ui.onNodeWithTag("bridge-documents").performScrollToNode(hasText("ARCHIVO GUARDADO · ${picture.name}"))
                ui.onNode(hasText("ABRIR ARCHIVO") and hasAnyAncestor(hasTestTag("saved-file-${picture.id}"))).performScrollTo().performClick()
                ui.waitUntil(10_000){shadowOf(c).peekNextStartedActivity()!=null}
                val photoIntent=shadowOf(c).nextStartedActivity;assertEquals(Uri.parse(picture.uri),photoIntent.data);assertEquals("image/jpeg",photoIntent.type)
                assertEquals(-1,shadowOf(c).nextStartedActivityForResult.requestCode)
                capture("documents-photo-own-bridge")
            }
            click("Volver a la ficha");return f
        }
        try {
            ids.forEachIndexed{i,id->export(id,i==0)}
            val folders=ids.map{ScapFolderExport.folderUri(c,db,it)};assertEquals(3,folders.toSet().size);assertFalse(folders.contains(null))
            ids.forEach{documents(it)}
            val foreign=db.localFilesDao().filesForRecord("SCAP/${ids[0]}").first{it.state=="READY"}
            assertTrue(runCatching{RecordDocuments.openTarget(c,db,"SCAP",ids[1],foreign)}.isFailure)
            assertEquals(beforeOutbox,db.automaticSyncDao().all())
            ui.runOnIdle{visible=false};ui.waitForIdle();scope.cancel();db.close()
            db=open();repo=SicConsolidationFixtures.repo(c,db);scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate);ctl=ScapController(c,db,scope)
            ui.runOnIdle{generation++;visible=true};ids.forEach{documents(it)}
            assertEquals(folders,ids.map{ScapFolderExport.folderUri(c,db,it)})
            c.contentResolver.releasePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            assertNull(ScapFolderExport.authorized(c,db,ids[2]));val count=provider.docs.size
            export(ids[2],true);assertEquals(count,provider.docs.size)
            val old=db.localFilesDao().filesForRecord("SCAP/${ids[2]}").filter{it.state=="READY"}
            repo.setField(ids[2],"inspection","singularityName","PRUEBA 7 RENOMBRADO")
            export(ids[2]);documents(ids[2]);assertEquals(originalHash,fileDigest(photo))
            assertTrue(old.all{File(it.source).isFile});assertTrue(db.localFilesDao().filesForRecord("SCAP/${ids[2]}").any{it.mimeType=="image/jpeg"&&it.sha256==originalHash.second})
            ids.take(2).forEach{documents(it)}
            editor(ids[2]);ui.onNodeWithText("Exportar SCAP").performClick()
            ui.waitUntil(10_000){ui.onAllNodesWithText("Nombre del archivo SCAP").fetchSemanticsNodes().isNotEmpty()};click("Cancelar")
            click("Opciones de exportación y diagnóstico");click("Guardar como…")
            ui.waitUntil(20_000){shadowOf(c).peekNextStartedActivityForResult()!=null};val request=shadowOf(c).nextStartedActivityForResult
            assertEquals(Intent.ACTION_CREATE_DOCUMENT,request.intent.action)
            val beforeCancel=provider.docs.size;val snapshot=repo.dao.snapshot(ids[2])!!
            ui.runOnIdle{c.activityResultRegistry.dispatchResult(request.requestCode,Activity.RESULT_CANCELED,null)}
            ui.waitUntil(10_000){ui.onAllNodesWithText("Guardado cancelado",substring=true).fetchSemanticsNodes().isNotEmpty()}
            assertEquals(beforeCancel,provider.docs.size);assertEquals(snapshot,repo.dao.snapshot(ids[2]))
            assertEquals(beforeOutbox,db.automaticSyncDao().all())
            assertFalse(db.localFilesDao().jobs().any{it.kind!="SCAP"})
            assertFalse(provider.docs.values.any{it.name.matches(Regex("SIC-[0-9]+[A-Z]?.xlsx"))&&it.parent?.let{p->provider.docs[p]?.name?.startsWith("Puente ")}==true})
        }finally{ui.runOnIdle{visible=false;models.clear()};ui.waitForIdle();scope.cancel();db.close()}
    }
    private fun widthReview(width:Int)=runBlocking {
        val c=ui.activity;val db=SicConsolidationFixtures.db(c);val repo=SicConsolidationFixtures.repo(c,db)
        val id=SicConsolidationFixtures.bridge(repo,"PRUEBA 7",route="PE-3NG");val s=repo.dao.snapshot(id)!!
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate);val ctl=ScapController(c,db,scope)
        val models=androidx.lifecycle.ViewModelStore();val vm=InventoryViewModel(ApplicationProvider.getApplicationContext<Application>());models.put("width",vm)
        var show by mutableStateOf(true);ui.setContent{MaterialTheme{if(show)ScapEditor(s,ctl,ScapCatalog.load(c).forSnapshot(s),vm,"Guardado",0,null)}}
        try {
            listOf("Exportar SCAP","Ver documentos","Guardar borrador").forEach{ui.onAllNodesWithText(it).assertCountEquals(1);ui.onNodeWithText(it).assertIsDisplayed()}
            capture("editor-$width")
            ui.onNodeWithText("Exportar SCAP").performClick();ui.waitUntil(10_000){ui.onAllNodesWithText("Nombre del archivo SCAP").fetchSemanticsNodes().isNotEmpty()}
            ui.onNodeWithText("Guardar",useUnmergedTree=true).performScrollTo().assertIsDisplayed()
            ui.onAllNodesWithText("Exportar SCAP").assertCountEquals(1);capture("export-$width")
            click("Cancelar");ui.onNodeWithText("Ver documentos").performClick();ui.onNodeWithText("Documentos del puente").assertIsDisplayed();capture("documents-$width")
            assertTrue(db.automaticSyncDao().all().isEmpty())
        }finally{ui.runOnIdle{show=false;models.clear()};ui.waitForIdle();scope.cancel();db.close()}
    }
    @Test @Config(qualifiers="w320dp-h800dp-mdpi") fun editor320()=widthReview(320)
    @Test @Config(qualifiers="w360dp-h800dp-mdpi") fun editor360()=widthReview(360)
    @Test @Config(qualifiers="w411dp-h900dp-mdpi") fun editor411()=widthReview(411)
}
