package com.tuempresa.inventariovial

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.UserCacheEntity
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.sic.*
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,qualifiers="w800dp-h1280dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OperatorFlow7ScreenTest {
    private val models=androidx.lifecycle.ViewModelStore()
    private lateinit var vm:InventoryViewModel
    private val app get()=ApplicationProvider.getApplicationContext<Application>()
    private val db get()=InventoryDatabase.getInstance(app)
    @get:Rule(order=0) val cleanup=object:org.junit.rules.TestWatcher() {
        override fun finished(description:org.junit.runner.Description) {models.clear();if(::vm.isInitialized)vm.field.auth.sessions.logout()}
    }
    @get:Rule(order=1) val compose=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Before fun prepare() {
        try {androidx.work.WorkManager.getInstance(app)} catch(_:IllegalStateException) {androidx.work.WorkManager.initialize(app,androidx.work.Configuration.Builder().build())}
        vm=InventoryViewModel(app);models.put("test",vm)
        runBlocking {withContext(Dispatchers.IO) {db.clearAllTables()}}
        vm.field.auth.sessions.login(UserCacheEntity("operator7","operator7","Ingeniero de prueba","ENGINEER",true,"",1,1,1))
    }
    private fun seed()=runBlocking {
        val r=testRecord("operator7-record",pr="41",distance=325.5,route="PE-22A",sic="SIC-18").copy(observations="Observación de prueba",latitude=null,longitude=null)
        db.inventoryDao().insertRecord(r)
        db.inventoryDao().insertSic18(Sic18Entity(r.id,"06","1",1,"1",1.0,1.0,"1","1"))
        val f=File(app.filesDir,"operator7.jpg")
        val b=Bitmap.createBitmap(40,30,Bitmap.Config.ARGB_8888);b.eraseColor(android.graphics.Color.GREEN)
        f.outputStream().use {b.compress(Bitmap.CompressFormat.JPEG,95,it)};b.recycle()
        db.inventoryDao().insertPhoto(PhotoEntity("operator7-photo",r.id,1,f.path,true,null,null,null,"PENDING",1))
    }
    private fun waitFor(text:String) {compose.waitUntil(20_000) {compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()}}
    private fun click(text:String) {compose.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToNode(hasText(text));compose.onNodeWithText(text).performClick()}
    private fun shot(name:String) {compose.runOnIdle {
        val view=compose.activity.window.decorView;val b=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(b))
        val f=File("../docs/flujo-operador-7/screenshots/$name.png");f.parentFile!!.mkdirs();f.outputStream().use {b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle()
    }}
    @Test fun detailNavigatesSheetGalleryExcelAndBackUsingSameUuid() {
        seed();var exited=false
        compose.setContent {com.tuempresa.inventariovial.ui.theme.InventarioVialTheme {Surface {RecordDetailScreen(vm,"operator7-record") {exited=true}}}}
        waitFor("VER FICHA");compose.onNodeWithText("operator7-record",substring=true).assertDoesNotExist()
        shot("01-detalle-vertical-datos-ficticios")
        click("VER FICHA");compose.onNodeWithText("Ficha completa · consulta").assertExists()
        click("Volver al detalle");click("VER FOTOGRAFÍAS")
        compose.onNodeWithText("GENERAL (1)").assertExists();compose.onNodeWithTag("review-photo-0").performClick()
        compose.onNodeWithText("Cerrar").performClick();compose.waitForIdle()
        click("Volver al detalle");click("VER EXCEL SIC")
        compose.onNodeWithText("Excel SIC · PE-22A").assertExists()
        click("Generar / actualizar copia local");waitFor("Abrir XLSX local")
        compose.onNodeWithText("Abrir XLSX local").assertHasClickAction()
        click("Volver al detalle");click("Volver");assertTrue(exited)
    }
    @Test @Config(qualifiers="w1280dp-h800dp-mdpi") fun landscapeActionsRemainReachableAndReturnToHistory() {
        seed();compose.setContent {com.tuempresa.inventariovial.ui.theme.InventarioVialTheme {Surface {RecordHistoryScreen(vm) {}}}}
        waitFor("Ver detalle");click("Ver detalle");waitFor("VER FICHA")
        listOf("VER FICHA","VER FOTOGRAFÍAS","VER EXCEL SIC","EDITAR").forEach {compose.onNodeWithText(it).performScrollTo().assertIsDisplayed().assertHasClickAction()}
        shot("02-detalle-horizontal-datos-ficticios")
        click("Volver");waitFor("Registros locales");compose.onNodeWithText("Ver detalle").assertExists()
    }
    @Test fun completingDraftOpensItsDetailAndSchedulesAutomaticDelivery() {
        runBlocking {SicFieldRepository(db).save("draft7",fajaState(SicFieldFormat.SIC08),"operator7",false)}
        compose.setContent {com.tuempresa.inventariovial.ui.theme.InventarioVialTheme {Surface {SicFieldWorkspace(vm,SicFieldFormat.SIC08,"draft7") {}}}}
        compose.waitUntil(20_000) {compose.onAllNodesWithTag("legacy-inventory-form").fetchSemanticsNodes().isNotEmpty()}
        click("Guardar registro");waitFor("VER FICHA")
        runBlocking {
            assertEquals("ACTIVE",db.inventoryDao().recordById("draft7")!!.status)
            assertNotNull(db.automaticSyncDao().get("SIC/draft7"))
            assertEquals(1,db.inventoryDao().recordsForExport("SIC-08",null).size)
        }
    }
    @Test fun scapOrderDestinationOpensOnlyItsInspectionAndExistingSections() {
        val ids=runBlocking {
            val catalog=com.tuempresa.inventariovial.scap.catalog.ScapCatalog.load(app)
            val repo=com.tuempresa.inventariovial.scap.data.ScapRepository(db,catalog)
            val first=repo.create("Inspector","test",null,com.tuempresa.inventariovial.scap.domain.ScapBridgeType.SLAB)
            val other=repo.create("Otro inspector","test",null,com.tuempresa.inventariovial.scap.domain.ScapBridgeType.SLAB)
            repo.addPhotoRecord(first,"INSPECCIÓN UNO","Foto primera inspección")
            repo.addPhotoRecord(other,"INSPECCIÓN DOS","Foto ajena")
            first to db.scapDao().snapshot(first)!!.inspection.roadRecordId
        }
        compose.setContent {com.tuempresa.inventariovial.ui.theme.InventarioVialTheme {Surface {RecordDetailScreen(vm,ids.second) {}}}}
        waitFor("VER FOTOGRAFÍAS (1)")
        compose.onNodeWithText("VER FOTOGRAFÍAS (1)").performClick()
        waitFor("FOTOGRAFÍAS · F.2 PANEL FOTOGRÁFICO")
        compose.onNode(hasText("INSPECCIÓN UNO") and hasSetTextAction()).assertExists()
        compose.onNodeWithText("INSPECCIÓN DOS").assertDoesNotExist()
        compose.onNodeWithText("VER EXCEL SIC / SCAP").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(ids.first,vm.scap.selected.value)}
        compose.onNodeWithText("VER FICHA").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(ids.first,vm.scap.snapshot.value!!.inspection.id)}
    }
}