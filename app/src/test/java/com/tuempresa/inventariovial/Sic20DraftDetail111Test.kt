package com.tuempresa.inventariovial

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.core.InventoryDraftStore
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,qualifiers="w1280dp-h800dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Sic20DraftDetail111Test {
    @get:Rule val ui=createComposeRule()
    private val app get()=ApplicationProvider.getApplicationContext<Application>()
    private val models=androidx.lifecycle.ViewModelStore()
    @After fun close(){models.clear();AuthGraph.get(app).sessions.logout()}
    private fun checkDraft(code:String,title:String,contradictoryAsset:String?=null) {
        initializeIdleUiWorkers()
        AuthGraph.get(app).sessions.login(UserCacheEntity("TEST111","test","FICTICIO","ENGINEER",true,"",1,1,1))
        runBlocking{com.tuempresa.inventariovial.localfiles.LocalCopySettings.enabled(app,false)}
        val vm=InventoryViewModel(app);models.put("TEST111",vm)
        val db=InventoryDatabase.getInstance(app);db.syncContext=null
        val id="TEST111_${UUID.randomUUID()}";val photo=File(app.filesDir,"$id.jpg")
        val bitmap=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888)
        photo.outputStream().use{bitmap.compress(Bitmap.CompressFormat.JPEG,95,it)};bitmap.recycle()
        val request=InventorySaveRequest("SIC-20",Sic20Identity.contractualAsset(code),"PE-3N","CD","0010","10","0011","180",if(code=="13")"S"else"D",Double.NaN,Double.NaN,null,"20/10/2026","TEST111 OBS",photo.path,
            SicFormDetail.Sic20(Sic20FormState(classCode=code,dimension1M="2",dimension2M="3")),recordId=id)
        runBlocking{InventoryDraftStore.save(app,db,request)}
        val before=InventoryDraftStore.read(app,id)!!.toString()
        runBlocking{assertNull(db.inventoryDao().snapshot(id)!!.sic20)}
        if(contradictoryAsset!=null)runBlocking{db.inventoryDao().updateRecord(db.inventoryDao().recordById(id)!!.copy(assetType=contradictoryAsset))}
        ui.setContent{MaterialTheme{RecordDetailScreen(vm,id){}}}
        ui.waitUntil(20000){ui.onAllNodesWithText("EDITAR").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("EDITAR").performScrollTo().performClick()
        if(contradictoryAsset!=null) {
            ui.waitUntil(20000){ui.onAllNodesWithText("Borrador conservado").fetchSemanticsNodes().isNotEmpty()}
            ui.onNodeWithTag("legacy-inventory-form").assertDoesNotExist()
            assertEquals(before,InventoryDraftStore.read(app,id)!!.toString())
            assertEquals(contradictoryAsset,runBlocking{db.inventoryDao().recordById(id)!!.assetType})
            assertTrue(photo.isFile);return
        }
        ui.waitUntil(20000){ui.onAllNodesWithTag("legacy-inventory-form").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("legacy-inventory-form").performScrollToNode(hasText("Registrar $title"))
        ui.onNodeWithText("Registrar $title").assertExists()
        ui.onNodeWithTag("legacy-inventory-form").performScrollToNode(hasText("OBSERVACIONES"))
        ui.onNode(hasSetTextAction()and hasText("OBSERVACIONES")).assertTextContains("TEST111 OBS")
        assertEquals(before,InventoryDraftStore.read(app,id)!!.toString())
        assertTrue(photo.isFile)
        runBlocking{assertEquals(id,db.inventoryDao().recordById(id)!!.id)}
    }
    @Test fun wallDraftFromDetailMustOpenWall(){checkDraft("14","Muro")}
    @Test fun tunnelDraftFromDetailMustOpenTunnel(){checkDraft("13","Túnel")}
    @Test fun fordDraftFromDetailMustOpenFord(){checkDraft("12","Badén")}
    @Test fun contradictoryHistoricalHeaderIsBlockedWithoutOpeningOrChangingEitherIdentity(){checkDraft("14","Muro","BADEN")}
}
