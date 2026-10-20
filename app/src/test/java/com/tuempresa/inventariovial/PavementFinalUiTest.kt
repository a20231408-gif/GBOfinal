package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.pavement.ui.DamageDialog
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,qualifiers="w1280dp-h800dp-land")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementFinalUiTest {
    @get:Rule val ui=createComposeRule()
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    @Before fun before(){initializeIdleUiWorkers();runBlocking{LocalCopySettings.enabled(c,false)}}
    @Test fun patchChoiceHasRealPreviewAndAcceptPersistsSelectedModeCallback() {
        val s=visualSample();val d=visualDamage(29,1,.2,1.0,1.2,2.0,1).copy(correlativo=0)
        var saved:String?=null
        ui.setContent{MaterialTheme{DamageDialog(d,"",{},{fail("must use atomic patch save")},sample=s,patchSave={_,mode->saved=mode})}}
        ui.onNodeWithText("Adosado al borde más cercano").performScrollTo().assertExists()
        ui.onNodeWithTag("patch-projection-preview").performScrollTo().assertExists()
        ui.onNodeWithText("Interior: conservar ubicación").performScrollTo().performClick()
        ui.onNodeWithText("Aceptar").performClick()
        assertEquals(PavementPatchVisuals.INTERIOR,saved)
    }
    @Test fun copyResultsAreRenderedOnceAndRemainIdentifiedAfterScreenReopen() {
        val db=InventoryDatabase.getInstance(c);val s=visualSample().copy(id="TEST_FINAL_UI",updatedAt=2)
        runBlocking {
            db.pavementDao().saveSample(s)
            db.localFilesDao().saveJob(LocalExportJob("OLD_UI","PAVEMENT/${s.id}","PAVEMENT",s.id,1,state="READY",completed=4,total=4,message="Copia local completada: 4 archivos verificados en Descargas."))
            db.localFilesDao().saveJob(LocalExportJob("NOW_UI","PAVEMENT/${s.id}","PAVEMENT",s.id,2,state="FAILED",message="Copia local fallida: TEST_FAILURE"))
        }
        var open by mutableStateOf(true)
        ui.setContent{MaterialTheme{if(open)Column(Modifier.verticalScroll(rememberScrollState())){LocalFilesList("PAVEMENT/${s.id}")}}}
        fun verify() {
            ui.waitUntil(5000){ui.onAllNodesWithText("Revisión anterior 1",substring=true).fetchSemanticsNodes().size==1}
            ui.onAllNodesWithText("Copia local completada",substring=true).assertCountEquals(1)
            ui.onAllNodesWithText("Copia local fallida",substring=true).assertCountEquals(1)
            ui.onNodeWithText("Revisión actual 2",substring=true).assertExists()
        }
        verify();ui.runOnIdle{open=false};ui.waitForIdle();ui.runOnIdle{open=true};verify()
        runBlocking{assertEquals(2,db.localFilesDao().jobs().count{it.recordId==s.id});assertNull(db.automaticSyncDao().get("PAVEMENT/${s.id}"))}
    }
}
