package com.tuempresa.inventariovial

import android.app.Application
import android.content.ContentUris
import android.content.pm.ProviderInfo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import android.provider.MediaStore
import com.tuempresa.inventariovial.export.SicRouteWorkbook
import com.tuempresa.inventariovial.scap.ui.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,qualifiers="w1100dp-h1400dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SicConsolidationUiTest {
    @get:Rule val ui=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private lateinit var provider:SicDownloadsProvider
    @Before fun before(){
        initializeIdleUiWorkers()
        provider=SicDownloadsProvider().apply{attachInfo(ui.activity,ProviderInfo().apply{authority="media"})}
        ShadowContentResolver.registerProviderInternal("media",provider)
    }
    @Test fun actualExportPanelPublishesEachSicThroughRoomWithoutLaunchingCreateDocument()=runBlocking {
        val context=ui.activity
        SicConsolidationFixtures.db(context).use{db->
            val r=SicConsolidationFixtures.repo(context,db);val id=SicConsolidationFixtures.bridge(r,"FICTICIO_UI")
            val s=r.dao.snapshot(id)!!;val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main);val controller=ScapController(context,db,scope)
            val action=mutableStateOf<String?>(null);var lastStatus:String?=null
            ui.setContent{MaterialTheme{Surface{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                ScapExportActions(true,false,{action.value="SIC-17A:principal"},{action.value="SCAP:principal"})
                ScapExportPanel(s,true,controller,requestedAction=action.value,onHandled={action.value=null},onStatus={_,m->lastStatus=m})
            }}}}
            ui.onNodeWithText("EXPORTAR SIC").performClick()
            ui.waitUntil(30_000){provider.entries.values.any{it.values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)=="SIC-17A.xlsx"}}
            ui.waitUntil(30_000){lastStatus?.contains("SIC-17A actualizado")==true}
            for(format in listOf("SIC-17","SIC-17B")) {
                ui.runOnIdle{action.value="$format:${System.nanoTime()}"}
                ui.waitUntil(30_000){lastStatus?.contains("$format actualizado")==true}
                val file=provider.entries.values.single{it.values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)=="$format.xlsx"}
                assertEquals(1,SicRouteWorkbook.inspect(file.file.readBytes(),format))
                assertEquals("Download/GBO Inventario Vial/PE-28H/EXCEL_SIC/",file.values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH))
            }
            ui.onNodeWithText("ABRIR ARCHIVO").performScrollTo().performClick()
            var opened:android.content.Intent?=null
            ui.waitUntil(20_000){if(opened==null)opened=org.robolectric.Shadows.shadowOf(context).nextStartedActivity;opened!=null}
            assertEquals(android.content.Intent.ACTION_VIEW,opened!!.action)
            assertEquals("SIC-17B.xlsx",provider.entries.getValue(ContentUris.parseId(opened!!.data!!)).values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME))
            ui.onNodeWithText("ABRIR UBICACIÓN DISPONIBLE").performScrollTo().performClick()
            var location:android.content.Intent?=null
            ui.waitUntil(10000){if(location==null)location=org.robolectric.Shadows.shadowOf(context).nextStartedActivity;location!=null}
            assertEquals(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS,location!!.action)

            assertTrue(provider.entries.values.all{it.values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME).startsWith("SIC-")})
            assertTrue(db.automaticSyncDao().all().isEmpty());scope.cancel()
        }
    }
    @Test fun missingSicFieldsExportBlankProvisionalRowsWithoutInventingOrSchedulingDrafts()=runBlocking {
        val context=ui.activity
        SicConsolidationFixtures.db(context).use{db->
            val r=SicConsolidationFixtures.repo(context,db);val id=SicConsolidationFixtures.bridge(r,"FICTICIO_INCOMPLETO")
            r.setSicField(id,"SIC-17","spans","");r.setSicField(id,"SIC-17B","wearingSurface","")
            val s=r.dao.snapshot(id)!!;val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main);val controller=ScapController(context,db,scope)
            val action=mutableStateOf<String?>(null);var message:String?=null
            ui.setContent{MaterialTheme{Surface{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
                ScapExportPanel(s,true,controller,requestedAction=action.value,onHandled={action.value=null},onStatus={_,m->message=m})
            }}}}
            for(format in listOf("SIC-17","SIC-17B")) {
                ui.runOnIdle{action.value="$format:${System.nanoTime()}"}
                ui.waitUntil(30_000){message?.contains("$format actualizado")==true}
                val file=provider.entries.values.single{it.values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)=="$format.xlsx"}
                val book=com.tuempresa.inventariovial.scap.export.TemplateWorkbook(file.file.inputStream())
                assertTrue(book.parts.containsKey("docProps/custom.xml"));assertEquals(1,SicRouteWorkbook.inspect(file.file.readBytes(),format))
                val preview=com.tuempresa.inventariovial.scap.export.ScapSicExporter.previews(s).single{it.format==format}
                assertTrue(preview.values.indices.filter{preview.values[it]==null}.all{index->book.cell(1,"${('A'+index)}14").textContent.isBlank()})
            }
            assertTrue(provider.entries.isNotEmpty());assertTrue(db.automaticSyncDao().all().isEmpty());scope.cancel()
        }
    }
}
