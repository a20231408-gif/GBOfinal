package com.tuempresa.inventariovial

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.tuempresa.inventariovial.core.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w800dp-h1200dp-mdpi",application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HotfixSyncUi146Test {
    private val models=androidx.lifecycle.ViewModelStore()
    private var model:com.tuempresa.inventariovial.viewmodel.InventoryViewModel?=null
    @get:Rule(order=0) val cleanup=object:org.junit.rules.TestWatcher() {
        override fun finished(description:org.junit.runner.Description) {models.clear();model?.field?.auth?.sessions?.logout()}
    }
    @get:Rule(order=1) val compose=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun usualPanelsHideTechnicalPhotoControlsForAllFamiliesAndKeepDiagnosticAvailable() {
        initializeIdleUiWorkers()
        var technical by mutableStateOf(false)
        compose.setContent {MaterialTheme {Column {
            if(technical)PhotoCompatibilityDiagnostic()
            else for(kind in listOf("SIC","SCAP","PAVEMENT")) AutomaticSyncPanel(kind,"TEST_AUTO_146",false)
        }}}
        compose.onNodeWithText("Consultar capacidad fotográfica").assertDoesNotExist()
        compose.onNodeWithText("Fotografías V2 en compilación",substring=true).assertDoesNotExist()
        compose.onNodeWithText("Verificar compatibilidad de fotografías con el servidor").assertDoesNotExist()
        compose.onAllNodesWithText("Drive: Borrador").assertCountEquals(3)
        compose.runOnIdle{technical=true}
        compose.onNodeWithText("Verificar compatibilidad de fotografías con el servidor").assertExists()
    }
    @Test fun actualWorkspaceContinueOpensVerticalFormWithOriginalUuidAndPhoto() {
        initializeIdleUiWorkers()
        val app=androidx.test.core.app.ApplicationProvider.getApplicationContext<Application>()
        android.provider.Settings.Secure.putString(app.contentResolver,android.provider.Settings.Secure.ANDROID_ID,"1461461461461461")
        val vm=com.tuempresa.inventariovial.viewmodel.InventoryViewModel(app);model=vm;models.put("inventory",vm)
        vm.field.auth.sessions.logout();vm.field.auth.sessions.login(com.tuempresa.inventariovial.auth.UserCacheEntity("test146","test146","Ficticio","ENGINEER",true,"",1,1,1))
        val id="TEST_AUTO_146_UI";val now=System.currentTimeMillis()
        val record=com.tuempresa.inventariovial.data.entity.InventoryRecordEntity(id,"SIC-22","VERTICAL","PE-22A","CD","0001",20.0,null,null,"D",null,null,null,null,
            java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),"FICTICIO","DRAFT","PENDING","PENDING",now,now)
        val file=java.io.File(app.cacheDir,"test146.jpg");val bitmap=android.graphics.Bitmap.createBitmap(24,24,android.graphics.Bitmap.Config.ARGB_8888)
        file.outputStream().use {bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,90,it)};bitmap.recycle()
        val photo=com.tuempresa.inventariovial.data.entity.PhotoEntity("photo146",id,1,file.path,true,null,null,null,"PENDING",now,photoCategory="GENERAL")
        kotlinx.coroutines.runBlocking {
            vm.field.dao.insertRecord(record)
            vm.field.dao.insertSic22(com.tuempresa.inventariovial.data.entity.Sic22Entity(id,"20","1","2","R-1",null,"1",null,null,null))
            vm.field.dao.insertPhoto(photo)
        }
        compose.setContent{MaterialTheme{InventoryWorkspace(vm,vm.field.auth)}}
        compose.waitUntil(20_000) {
            compose.waitForIdle();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            compose.onAllNodesWithText("1 borradores del día").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("dashboard").performScrollToNode(hasText("BORRADORES POR COMPLETAR"))
        compose.onNodeWithTag("continue-$id").performScrollTo().performClick()
        compose.waitUntil(20_000) {compose.waitForIdle();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            compose.onAllNodesWithText("Registrar Señal vertical").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Registrar Badén").assertDoesNotExist()
        val snapshot=kotlinx.coroutines.runBlocking {vm.field.dao.snapshot(id)!!}
        org.junit.Assert.assertEquals("SIC-22",snapshot.record.sicCode)
        org.junit.Assert.assertNull(snapshot.sic20)
        org.junit.Assert.assertEquals(photo,snapshot.photos.single())
    }
}
