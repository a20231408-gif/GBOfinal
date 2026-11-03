package com.tuempresa.inventariovial

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.tuempresa.inventariovial.scap.ui.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.*
import org.robolectric.shadows.ShadowStatFs

/** Verifies native Android contracts; does not pretend to operate a physical DocumentsUI. */
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class,qualifiers="w400dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScapNativeExportUiTest {
    @get:Rule val ui=createAndroidComposeRule<ComponentActivity>()
    @Before fun init(){initializeIdleUiWorkers();ShadowStatFs.registerStats(ui.activity.filesDir.path,1_000_000,900_000,900_000)}
    @Test fun primaryExportImmediatelyRequestsNativeExcelSaveAsAndCancelPreservesCapture()=runBlocking {
        SicConsolidationFixtures.db(ui.activity).use{db->
            val repo=SicConsolidationFixtures.repo(ui.activity,db);val id=SicConsolidationFixtures.bridge(repo,"PUENTE QUEBRADA",route="PE-3N")
            val before=repo.dao.snapshot(id)!!;val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
            val controller=ScapController(ui.activity,db,scope);var message:String?=null
            ui.setContent{MaterialTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
                ScapExportPanel(before,true,controller,onStatus={_,m->message=m})
            }}}
            ui.onNodeWithTag("scap-save-excel").performScrollTo().performClick()
            ui.waitUntil(20000){shadowOf(ui.activity).peekNextStartedActivityForResult()!=null}
            val request=shadowOf(ui.activity).nextStartedActivityForResult
            assertEquals(Intent.ACTION_CREATE_DOCUMENT,request.intent.action)
            assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",request.intent.type)
            assertEquals("SCAP - PE-3N - PUENTE QUEBRADA.xlsx",request.intent.getStringExtra(Intent.EXTRA_TITLE))
            // AndroidX CreateDocument supplies ACTION_CREATE_DOCUMENT, MIME and title;
            // CATEGORY_OPENABLE is not part of that contract on this library version.
            ui.runOnIdle{ui.activity.activityResultRegistry.dispatchResult(request.requestCode,Activity.RESULT_CANCELED,null)}
            ui.waitUntil(5000){message?.startsWith("Guardado cancelado")==true}
            assertEquals(before,repo.dao.snapshot(id));assertTrue(db.automaticSyncDao().all().isEmpty())
            assertFalse(db.localFilesDao().jobs().any{it.scope.startsWith("ZIP")})
            scope.cancel()
        }
    }
    @Test fun bridgeFolderHasItsOwnNativeTreeContractAndNoAutomaticPublicationOnCancel()=runBlocking {
        SicConsolidationFixtures.db(ui.activity).use{db->
            val repo=SicConsolidationFixtures.repo(ui.activity,db);val id=SicConsolidationFixtures.bridge(repo,"FICTICIO CARPETA")
            val before=repo.dao.snapshot(id)!!;val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
            val controller=ScapController(ui.activity,db,scope);var message:String?=null
            ui.setContent{MaterialTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
                ScapExportPanel(before,true,controller,onStatus={_,m->message=m})
            }}}
            ui.onNodeWithTag("scap-save-folder").performScrollTo().performClick()
            val request=shadowOf(ui.activity).nextStartedActivityForResult
            assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE,request.intent.action)
            ui.runOnIdle{ui.activity.activityResultRegistry.dispatchResult(request.requestCode,Activity.RESULT_CANCELED,null)}
            ui.waitUntil(5000){message?.startsWith("Carpeta cancelada")==true}
            assertFalse(db.localFilesDao().jobs().any{it.scope=="SAF_TREE"});assertTrue(db.automaticSyncDao().all().isEmpty())
            assertEquals(before,repo.dao.snapshot(id));scope.cancel()
        }
    }
}
