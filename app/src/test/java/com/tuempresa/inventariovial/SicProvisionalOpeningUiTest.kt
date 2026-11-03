package com.tuempresa.inventariovial

import android.app.Application
import android.content.pm.ProviderInfo
import android.content.pm.ResolveInfo
import android.content.pm.ActivityInfo
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.Sic18Entity
import com.tuempresa.inventariovial.export.LocalSicConsolidates
import com.tuempresa.inventariovial.localfiles.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.shadows.ShadowStatFs

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SicProvisionalOpeningUiTest {
    @get:Rule val ui=createComposeRule()
    @Test fun openButtonSwitchesFromProvisionalToCurrentConsolidateAfterAsyncPublication() {
        initializeIdleUiWorkers();val c=ApplicationProvider.getApplicationContext<Application>()
        ShadowStatFs.registerStats(c.filesDir.path,1_000_000,900_000,900_000)
        val provider=SicDownloadsProvider().apply{attachInfo(c,ProviderInfo().apply{authority="media";exported=false})}
        ShadowContentResolver.registerProviderInternal("media",provider)
        val db=InventoryDatabase.getInstance(c);val id="TEST_UI_PR200"
        val job=LocalExportJob("UI_DRAFT", "SIC/$id","SIC",id,1)
        runBlocking{
            LocalCopySettings.enabled(c,false)
            db.inventoryDao().insertRecord(testRecord(id,"0010",200.0,created=1,status="DRAFT",route="PE-22A",sic="SIC-18"))
            db.localFilesDao().saveJob(job);LocalCopyDrain.run(c,db,limit=30)
        }
        val item=LocalInventoryItem("SIC",id,"SIC-18","Alcantarilla PR 10+200","PE-22A","10+200",10200.0,"CD","INCREASING","ACTIVE",2)
        ui.setContent{MaterialTheme{androidx.compose.foundation.layout.Column{DirectLocalDocuments(item,job.copy(state="READY"))}}}
        ui.waitUntil(5000){ui.onAllNodesWithText("Abrir provisional").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("Documento provisional: contiene datos pendientes y no es una entrega definitiva.").assertExists()
        val actual=runBlocking {
            db.inventoryDao().updateRecord(db.inventoryDao().recordById(id)!!.copy(status="ACTIVE",updatedAt=2))
            db.inventoryDao().insertSic18(Sic18Entity(id,"06","3",2,"2",1.25,null,"3","1"))
            LocalSicConsolidates.update(c,db,"PE-22A","SIC-18")
        }
        ui.waitUntil(5000){ui.onAllNodesWithText("Documento principal: consolidado SIC verificado en EXCEL_SIC.").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("Abrir provisional").assertDoesNotExist()
        ui.onNodeWithText("Ver archivos anteriores").assertExists()
        shadowOf(c.packageManager).addResolveInfoForIntent(SharedFileActions.view(actual.uri,com.tuempresa.inventariovial.export.SicExcelWriter.XLSX_MIME),ResolveInfo().apply{
            activityInfo=ActivityInfo().apply{packageName="test.viewer";name="test.viewer.ExcelActivity";exported=true;enabled=true}
        })
        ui.onNodeWithText("Abrir archivo").performClick()
        try{ui.waitUntil(5000){shadowOf(c).peekNextStartedActivity()!=null}}catch(e:Throwable){throw AssertionError(ui.onAllNodes(isRoot()).printToString(),e)}
        assertEquals(actual.uri,shadowOf(c).nextStartedActivity.data)
        runBlocking{assertTrue(db.localFilesDao().files(job.id).any{it.name=="SIC-18.xlsx"&&RecordDocuments.provisional(it)});assertTrue(db.automaticSyncDao().all().isEmpty())}
    }
}
