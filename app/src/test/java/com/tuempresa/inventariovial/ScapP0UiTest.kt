package com.tuempresa.inventariovial

import android.app.Application
import android.content.Intent
import android.content.ContentUris
import android.content.pm.ProviderInfo
import android.provider.MediaStore
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.scap.ui.*
import com.tuempresa.inventariovial.scap.domain.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import org.robolectric.shadows.*
import java.io.File
import java.util.zip.ZipFile

/** Real Compose -> controller -> Room outbox -> real builder/store over controlled Android providers. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,qualifiers="w1100dp-h1400dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScapP0UiTest {
    @get:Rule val ui=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private lateinit var provider:SicDownloadsProvider
    @Before fun setup(){
        initializeIdleUiWorkers()
        ShadowStatFs.registerStats(ui.activity.filesDir.path,1_000_000,900_000,900_000)
        provider=SicDownloadsProvider().apply{attachInfo(ui.activity,ProviderInfo().apply{authority="media"})}
        ShadowContentResolver.registerProviderInternal("media",provider)
    }
    @Test fun actualFiftyFourPhotoDialogHasDurableProgressAndVerifiedOpenAndLocationAfterNavigation()=runBlocking {
        val c=ui.activity
        SicConsolidationFixtures.db(c).use{db->
            val id=ScapP0Fixture.create(c,db);val s=db.scapDao().snapshot(id)!!
            val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main);val controller=ScapController(c,db,scope)
            val show=mutableStateOf(true);val action=mutableStateOf<String?>(null);var status:String?=null;var busy by mutableStateOf(false)
            ui.setContent{MaterialTheme{Surface{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
                ScapExportActions(true,busy,{},{action.value="SCAP:${System.nanoTime()}"})
                if(show.value)ScapExportPanel(s,true,controller,requestedAction=action.value,onHandled={action.value=null},onStatus={b,m->busy=b;status=m})
            }}}}
            ui.onNodeWithText("EXPORTAR SCAP").performClick()
            ui.onNodeWithText("Confirmar exportación local").assertExists()
            ui.onNodeWithText("Preparar copia local").performClick()
            ui.waitUntil(20_000){runBlocking(Dispatchers.IO){db.localFilesDao().jobs().any{it.scope=="ZIP"}}}
            ui.waitUntil(20_000){status=="Preparando SCAP…"}
            val job=db.localFilesDao().jobs().single{it.scope=="ZIP"};assertEquals("QUEUED",job.state)
            ui.runOnIdle{show.value=false}
            assertEquals(job.id,db.localFilesDao().jobs().single{it.scope=="ZIP"}.id)
            LocalCopyDrain.run(c,db,AndroidSharedDocumentStore(c),limit=30)
            assertEquals("READY",db.localFilesDao().job(job.id)!!.state)
            ui.runOnIdle{show.value=true}
            // The parent callback is cleared on leaving the panel; the durable result is the real UI + URI.
            try{ui.waitUntil(30_000){ui.onAllNodesWithTag("scap-open-file").fetchSemanticsNodes().isNotEmpty()}}catch(e:Throwable){
                val trace=File("../tmp/scap-p0-20261027/DIAGNOSTICO_UI_FICTICIO.txt")
                trace.writeText("status=$status; show=${show.value}; job="+db.localFilesDao().job(job.id)!!.state+"; files="+db.localFilesDao().files(job.id).map{it.state}+"\n")
                trace.appendText(ui.onRoot(useUnmergedTree=true).printToString())
                throw AssertionError("No se restauro el resultado: status=$status; job="+db.localFilesDao().job(job.id)!!.state+"; files="+db.localFilesDao().files(job.id).map{it.state},e)
            }
            assertTrue(ui.onAllNodesWithText("SCAP guardado correctamente").fetchSemanticsNodes().isNotEmpty())
            ui.onNodeWithTag("scap-open-file").performScrollTo().performClick()
            var opened:Intent?=null
            ui.waitUntil(10_000){if(opened==null)opened=org.robolectric.Shadows.shadowOf(c).nextStartedActivity;opened!=null}
            assertEquals(Intent.ACTION_VIEW,opened!!.action);assertEquals("application/zip",opened!!.type)
            assertEquals("content",opened!!.data!!.scheme)
            val entry=provider.entries.getValue(ContentUris.parseId(opened!!.data!!))
            assertTrue(entry.values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH).startsWith("Download/"))
            ZipFile(entry.file).use{z->assertEquals(54,z.entries().asSequence().count{"FOTOGRAFIAS" in it.name.split('/')})}
            ui.onNodeWithTag("scap-open-location").performScrollTo().performClick()
            val location=org.robolectric.Shadows.shadowOf(c).nextStartedActivity
            assertEquals(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS,location.action)
            assertEquals(s,db.scapDao().snapshot(id));assertEquals(0,db.automaticSyncDao().get("SCAP/$id")!!.syncedRevision)
            scope.cancel()
        }
    }
    @Test fun publicWriteFailureIsVisibleAndRetryReusesThePackageWithoutFalselyConfirmingDrive()=runBlocking {
        val c=ui.activity
        SicConsolidationFixtures.db(c).use{db->
            val id=ScapP0Fixture.create(c,db,1);val s=db.scapDao().snapshot(id)!!
            val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main);val controller=ScapController(c,db,scope)
            val job=controller.localScapPackage(id).await();var message:String?=null
            ui.setContent{MaterialTheme{Surface{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
                ScapExportPanel(s,true,controller,onStatus={_,m->message=m})
            }}}}
            val real=AndroidSharedDocumentStore(c)
            val failing=object:SharedDocumentStore by real {
                override fun write(uri:android.net.Uri,source:File){
                    val bad=File(c.cacheDir,"FICTICIO_CORTADO.zip").apply{writeBytes(source.readBytes().take(7).toByteArray())}
                    real.write(uri,bad)
                }
            }
            LocalCopyDrain.run(c,db,failing)
            ui.waitUntil(20_000){message=="No se pudo guardar SCAP"}
            ui.onNodeWithTag("scap-open-file").assertDoesNotExist()
            ui.onNodeWithText("REINTENTAR COPIA LOCAL").performScrollTo().performClick()
            ui.waitUntil(20_000){runBlocking(Dispatchers.IO){db.localFilesDao().job(job.id)?.state=="QUEUED"}}
            LocalCopyDrain.run(c,db,real)
            ui.waitUntil(20_000){message=="SCAP guardado correctamente"}
            ui.onNodeWithTag("scap-open-file").assertExists()
            assertEquals(s,db.scapDao().snapshot(id));assertFalse(com.tuempresa.inventariovial.core.SyncIntentPresentation.confirmed(db.automaticSyncDao().get("SCAP/$id")))
            scope.cancel()
        }
    }
    @Test fun missingHistoricalDateNavigatesToTheActualSicDateFieldAndPersistsCorrection()=runBlocking {
        val c=ui.activity
        SicConsolidationFixtures.db(c).use{db->
            val repo=SicConsolidationFixtures.repo(c,db);val id=SicConsolidationFixtures.bridge(repo,"TEST_FECHA")
            repo.setField(id,"inspection","lastInspection","01/01/2010");repo.setSicField(id,"SIC-17","surveyDate","")
            val s=mutableStateOf(repo.dao.snapshot(id)!!);val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main);val controller=ScapController(c,db,scope)
            val action=mutableStateOf<String?>("SIC-17:fecha");var message:String?=null
            ui.setContent{MaterialTheme{Surface{
                val destination by controller.destination.collectAsState()
                CompositionLocalProvider(LocalScapDestination provides destination) {
                    if(destination?.second?.section=="SIC")ScapBridgeSicPanel(s.value,controller,true)
                    else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){ScapExportPanel(s.value,true,controller,requestedAction=action.value,onHandled={action.value=null},onStatus={_,m->message=m})}
                }
            }}}
            ui.waitUntil(20_000){message?.contains("Fecha")==true}
            ui.onNodeWithText("Falta fecha de inspección para SIC-17").assertExists()
            ui.onNodeWithText("COMPLETAR FECHA").performScrollTo().performClick()
            assertEquals("SIC-17",controller.destination.value!!.second.repeatableItemId)
            assertEquals("surveyDate",controller.destination.value!!.second.fieldKey)
            ui.onNodeWithTag("sic-input-SIC-17-surveyDate").performScrollTo().performTextInput("27/10/2026")
            ui.waitUntil(20_000){controller.pending.value==0}
            val saved=repo.dao.snapshot(id)!!;assertEquals("27/10/2026",saved.values()["sic17.surveyDate"])
            assertEquals("01/01/2010",saved.values()["lastInspection"])
            assertFalse(com.tuempresa.inventariovial.scap.export.ScapSicExporter.previews(saved).single{it.format=="SIC-17"}.missingRequired.contains("Fecha"))
            scope.cancel()
        }
    }
}
