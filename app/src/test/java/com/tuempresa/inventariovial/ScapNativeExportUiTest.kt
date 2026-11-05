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
    @Test fun secondarySaveAsRequestsNativeExcelAndCancelPreservesCapture()=runBlocking {
        SicConsolidationFixtures.db(ui.activity).use{db->
            val repo=SicConsolidationFixtures.repo(ui.activity,db);val id=SicConsolidationFixtures.bridge(repo,"PUENTE QUEBRADA",route="PE-3N")
            val before=repo.dao.snapshot(id)!!;val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
            val controller=ScapController(ui.activity,db,scope);var message:String?=null
            ui.setContent{MaterialTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
                ScapExportPanel(before,true,controller,onStatus={_,m->message=m})
            }}}
            ui.onNodeWithText("Opciones de exportación y diagnóstico").performScrollTo().performClick()
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
            ui.onNodeWithTag("scap-export-direct").performScrollTo().performClick()
            ui.waitUntil(10000){ui.onAllNodesWithText("Nombre del archivo SCAP").fetchSemanticsNodes().isNotEmpty()}
            ui.onNodeWithText("Guardar",useUnmergedTree=true).performScrollTo().performClick()
            ui.waitUntil(10000){shadowOf(ui.activity).peekNextStartedActivityForResult()!=null}
            val request=shadowOf(ui.activity).nextStartedActivityForResult
            assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE,request.intent.action)
            ui.runOnIdle{ui.activity.activityResultRegistry.dispatchResult(request.requestCode,Activity.RESULT_CANCELED,null)}
            ui.waitUntil(5000){message?.startsWith("Carpeta cancelada")==true}
            assertFalse(db.localFilesDao().jobs().any{it.scope=="SAF_TREE"});assertTrue(db.automaticSyncDao().all().isEmpty())
            assertEquals(before,repo.dao.snapshot(id));scope.cancel()
        }
    }    @Test fun saveAsProposesCurrentBridgeFolderRatherThanPreviousBridge()=runBlocking {
        val c=ui.activity
        val provider=SicDocumentsProvider().apply{attachInfo(c,android.content.pm.ProviderInfo().apply{authority="com.android.externalstorage.documents";exported=true;grantUriPermissions=true;readPermission="android.permission.MANAGE_DOCUMENTS";writePermission="android.permission.MANAGE_DOCUMENTS"})}
        org.robolectric.shadows.ShadowContentResolver.registerProviderInternal("com.android.externalstorage.documents",provider)
        val tree=android.provider.DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents","root")
        SicConsolidationFixtures.db(c).use{db->val repo=SicConsolidationFixtures.repo(c,db)
            val previous=SicConsolidationFixtures.bridge(repo,"PRUEBA 2");val id=SicConsolidationFixtures.bridge(repo,"PRUEBA 3")
            com.tuempresa.inventariovial.scap.export.ScapFolderExport.save(c,db,repo.dao.snapshot(previous)!!,tree)
            com.tuempresa.inventariovial.scap.export.ScapFolderExport.save(c,db,repo.dao.snapshot(id)!!,tree)
            val expected=com.tuempresa.inventariovial.scap.export.ScapFolderExport.folderUri(c,db,id)
            val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main);val controller=ScapController(c,db,scope)
            val snapshot=repo.dao.snapshot(id)!!
            ui.setContent{MaterialTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){ScapExportPanel(snapshot,true,controller)}}}
            ui.onNodeWithText("Opciones de exportación y diagnóstico").performScrollTo().performClick()
            ui.onNodeWithTag("scap-save-excel").performScrollTo().performClick()
            ui.waitUntil(20000){shadowOf(c).peekNextStartedActivityForResult()!=null}
            val request=shadowOf(c).nextStartedActivityForResult
            assertEquals(expected,request.intent.getParcelableExtra<android.net.Uri>(android.provider.DocumentsContract.EXTRA_INITIAL_URI))
            assertNotEquals(com.tuempresa.inventariovial.scap.export.ScapFolderExport.folderUri(c,db,previous),expected)
            ui.runOnIdle{c.activityResultRegistry.dispatchResult(request.requestCode,Activity.RESULT_CANCELED,null)};scope.cancel()
        }
    }
    private fun provider():SicDocumentsProvider {
        val p=SicDocumentsProvider().apply{attachInfo(ui.activity,android.content.pm.ProviderInfo().apply{authority="com.android.externalstorage.documents";exported=true;grantUriPermissions=true;readPermission="android.permission.MANAGE_DOCUMENTS";writePermission="android.permission.MANAGE_DOCUMENTS"})}
        org.robolectric.shadows.ShadowContentResolver.registerProviderInternal("com.android.externalstorage.documents",p);return p
    }
    @Test fun primaryPe3ngExportIgnoresPreviousPe22aAndSecondaryCancelPublishesNothing()=runBlocking {
        val provider=provider();val c=ui.activity;val tree=android.provider.DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents","root")
        SicConsolidationFixtures.db(c).use{db->val repo=SicConsolidationFixtures.repo(c,db)
            val previous=SicConsolidationFixtures.bridge(repo,"ANTERIOR",route="PE-22A")
            com.tuempresa.inventariovial.scap.export.ScapFolderExport.save(c,db,repo.dao.snapshot(previous)!!,tree)
            val id=SicConsolidationFixtures.bridge(repo,"PRUEBA 2",route="PE-3NG");val before=repo.dao.snapshot(id)!!
            val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main);val controller=ScapController(c,db,scope);var message:String?=null
            ui.setContent{MaterialTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){ScapExportPanel(before,true,controller,onStatus={_,m->message=m})}}}
            ui.onNodeWithTag("scap-export-direct").performScrollTo().performClick()
            ui.waitUntil(10000){ui.onAllNodesWithText("Nombre del archivo SCAP").fetchSemanticsNodes().isNotEmpty()}
            ui.onNodeWithText("Nombre del archivo SCAP").assertExists()
            assertNull(shadowOf(c).peekNextStartedActivityForResult())
            ui.onNodeWithText("Guardar",useUnmergedTree=true).performScrollTo().performClick()
            ui.waitUntil(20000){message?.startsWith("ARCHIVO GUARDADO")==true}
            assertNull(shadowOf(c).peekNextStartedActivityForResult())
            assertTrue(message!!.contains("PE-3NG/PUENTES/Puente PRUEBA 2/SCAP - PE-3NG - PRUEBA 2.xlsx"));assertTrue(message!!.contains("BORRADOR"))
            val file=db.localFilesDao().filesForRecord("SCAP/$id").single{it.name=="SCAP - PE-3NG - PRUEBA 2.xlsx"}
            val store=com.tuempresa.inventariovial.localfiles.AndroidSharedDocumentStore(c,tree);assertEquals(file.size to file.sha256,store.digest(android.net.Uri.parse(file.uri)))
            val wb=com.tuempresa.inventariovial.scap.export.TemplateWorkbook(java.io.File(file.source).inputStream())
            assertTrue(wb.cell(1,"C4").textContent.contains("PRUEBA 2"))
            assertFalse(db.localFilesDao().filesForRecord("SCAP/$id").any{it.relativePath.contains("PE-22A")||it.name.contains("PROVISIONAL")})
            val publicCount=provider.docs.size
            ui.onNodeWithText("Opciones de exportación y diagnóstico").performScrollTo().performClick()
            ui.onNodeWithTag("scap-save-excel").performScrollTo().performClick();ui.waitUntil(20000){shadowOf(c).peekNextStartedActivityForResult()!=null}
            val request=shadowOf(c).nextStartedActivityForResult;assertEquals(Intent.ACTION_CREATE_DOCUMENT,request.intent.action)
            ui.runOnIdle{c.activityResultRegistry.dispatchResult(request.requestCode,Activity.RESULT_CANCELED,null)}
            ui.waitUntil(5000){message?.startsWith("Guardado cancelado")==true};assertEquals(publicCount,provider.docs.size)
            assertEquals(before,repo.dao.snapshot(id));assertTrue(db.automaticSyncDao().all().isEmpty());scope.cancel()
        }
    }
    @Test fun absentAndRevokedSafGrantRequestOnlyBaseAuthorizationFromPrimaryHeader()=runBlocking {
        provider();val c=ui.activity;val tree=android.provider.DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents","root")
        SicConsolidationFixtures.db(c).use{db->val repo=SicConsolidationFixtures.repo(c,db);val id=SicConsolidationFixtures.bridge(repo,"PRUEBA 2",route="PE-3NG")
            assertNull(com.tuempresa.inventariovial.scap.export.ScapFolderExport.authorized(c,db,id))
            com.tuempresa.inventariovial.scap.export.ScapFolderExport.save(c,db,repo.dao.snapshot(id)!!,tree)
            c.contentResolver.releasePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            assertNull(com.tuempresa.inventariovial.scap.export.ScapFolderExport.authorized(c,db,id))
            val before=repo.dao.snapshot(id)!!;val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main);val controller=ScapController(c,db,scope)
            var requestAction by mutableStateOf<String?>(null)
            ui.setContent{MaterialTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){ScapExportPanel(before,true,controller,requestedAction=requestAction,onHandled={requestAction=null})}}}
            ui.runOnIdle{requestAction="SCAP:HEADER"}
            ui.waitUntil(10000){ui.onAllNodesWithText("Nombre del archivo SCAP").fetchSemanticsNodes().isNotEmpty()}
            ui.onNodeWithText("Nombre del archivo SCAP").assertExists();ui.onNodeWithText("Guardar",useUnmergedTree=true).performScrollTo().performClick()
            ui.waitUntil(10000){shadowOf(c).peekNextStartedActivityForResult()!=null};val request=shadowOf(c).nextStartedActivityForResult
            assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE,request.intent.action)
            ui.runOnIdle{c.activityResultRegistry.dispatchResult(request.requestCode,Activity.RESULT_CANCELED,null)}
            assertEquals(before,repo.dao.snapshot(id));assertTrue(db.automaticSyncDao().all().isEmpty());scope.cancel()
        }
    }

}
