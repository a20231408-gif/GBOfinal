package com.tuempresa.inventariovial

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.ProviderInfo
import android.provider.DocumentsContract
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.scap.ui.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.*
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.shadows.ShadowStatFs

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,qualifiers="w1100dp-h1400dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SicConsolidationUiTest {
    @get:Rule val ui=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private lateinit var provider:SicDocumentsProvider
    private val authority="com.android.externalstorage.documents"
    @Before fun before(){
        initializeIdleUiWorkers();ShadowStatFs.registerStats(ui.activity.filesDir.path,1_000_000,900_000,900_000)
        provider=SicDocumentsProvider().apply{attachInfo(ui.activity,ProviderInfo().apply{
            authority=this@SicConsolidationUiTest.authority;exported=true;grantUriPermissions=true
            readPermission="android.permission.MANAGE_DOCUMENTS";writePermission="android.permission.MANAGE_DOCUMENTS"
        })};ShadowContentResolver.registerProviderInternal(authority,provider)
    }
    private fun checkExport(incomplete:Boolean)=runBlocking {
        val context=ui.activity
        SicConsolidationFixtures.db(context).use{db->
            val r=SicConsolidationFixtures.repo(context,db);val id=SicConsolidationFixtures.bridge(r,"FICTICIO_UI")
            if(incomplete){r.setSicField(id,"SIC-17","spans","");r.setSicField(id,"SIC-17B","wearingSurface","")}
            val original=r.dao.snapshot(id)!!;val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
            val controller=ScapController(context,db,scope);val action=mutableStateOf<String?>(null);var status:String?=null
            ui.setContent{MaterialTheme{Surface{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
                ScapExportPanel(original,true,controller,requestedAction=action.value,onHandled={action.value=null},onStatus={_,m->status=m})
            }}}}
            try{
                for(format in listOf("SIC-17","SIC-17A","SIC-17B")){
                    ui.runOnIdle{action.value="$format:${System.nanoTime()}"}
                    ui.waitUntil(20_000){shadowOf(context).peekNextStartedActivityForResult()!=null}
                    val request=shadowOf(context).nextStartedActivityForResult
                    assertEquals(Intent.ACTION_CREATE_DOCUMENT,request.intent.action)
                    assertEquals(SicExcelWriter.XLSX_MIME,request.intent.type)
                    assertEquals("$format.xlsx",request.intent.getStringExtra(Intent.EXTRA_TITLE))
                    val documentId=provider.createDocument("root",SicExcelWriter.XLSX_MIME,"Elegido-$format.xlsx")
                    val uri=DocumentsContract.buildDocumentUri(authority,documentId)
                    ui.runOnIdle{context.activityResultRegistry.dispatchResult(request.requestCode,Activity.RESULT_OK,Intent().setData(uri))}
                    ui.waitUntil(20_000){status?.startsWith("ARCHIVO GUARDADO")==true}
                    val file=provider.docs.getValue(documentId).file
                    assertEquals(1,SicRouteWorkbook.inspect(file.readBytes(),format))
                    if(incomplete&&format!="SIC-17A"){
                        assertTrue(status!!.contains("BORRADOR"))
                        val book=com.tuempresa.inventariovial.scap.export.TemplateWorkbook(file.inputStream())
                        assertTrue(book.parts.containsKey("docProps/custom.xml"))
                        val preview=com.tuempresa.inventariovial.scap.export.ScapSicExporter.previews(original).single{it.format==format}
                        assertTrue(preview.values.indices.filter{preview.values[it]==null}.all{i->book.cell(1,"${('A'+i)}14").textContent.isBlank()})
                    }
                    ui.onNodeWithText("ABRIR ARCHIVO").performScrollTo().performClick()
                    ui.waitUntil(5000){shadowOf(context).peekNextStartedActivity()!=null}
                    val opened=shadowOf(context).nextStartedActivity
                    assertEquals(Intent.ACTION_VIEW,opened.action);assertEquals(uri,opened.data)
                    // Robolectric also mirrors startActivity (requestCode=-1) in
                    // the activity-for-result queue; consume that verified VIEW.
                    val mirrored=shadowOf(context).nextStartedActivityForResult
                    assertEquals(Intent.ACTION_VIEW,mirrored.intent.action);assertEquals(uri,mirrored.intent.data)
                    assertEquals(-1,mirrored.requestCode)
                }
                assertEquals(original,r.dao.snapshot(id));assertTrue(db.automaticSyncDao().all().isEmpty())
                assertTrue(db.localFilesDao().jobs().all{it.scope=="SAF"})
            }finally{scope.cancel()}
        }
    }
    @Test fun eachBridgeSicUsesNativeSaveAsVerifiedConsolidateAndExactSavedUri()=checkExport(false)
    @Test fun missingSicFieldsRemainBlankAndDraftThroughNativeSaveWithoutSchedulingDrive()=checkExport(true)
}
