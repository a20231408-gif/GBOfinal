package com.tuempresa.inventariovial

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.model.RoadAssetType
import com.tuempresa.inventariovial.sic.*
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

/** Native Compose captures of production screens, not hand-drawn mockups. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w800dp-h1200dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SicUiParityTest {
    @get:Rule val compose=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val app get()=ApplicationProvider.getApplicationContext<Application>()
    @Before fun isolateNativeTestDatabase() = runBlocking {
        try {androidx.work.WorkManager.getInstance(app)} catch(_:IllegalStateException) {
            androidx.work.WorkManager.initialize(app,androidx.work.Configuration.Builder().build())
        }
        com.tuempresa.inventariovial.auth.AuthGraph.get(app).sessions.logout()
        withContext(Dispatchers.IO) {com.tuempresa.inventariovial.data.database.InventoryDatabase.getInstance(app).clearAllTables()}
        app.getSharedPreferences("survey_context",android.content.Context.MODE_PRIVATE).edit().clear().commit()
        Unit
    }
    private fun output():File {
        val wd=File(requireNotNull(System.getProperty("user.dir")))
        return File(if(File(wd,"app").isDirectory)wd else wd.parentFile,"docs/sic-ui-parity-evidence/screenshots").also {it.mkdirs()}
    }
    private fun scroll(text:String) {compose.onNodeWithTag("legacy-inventory-form").performScrollToNode(hasText(text))}
    private fun waitForLoadedForm() {
        compose.waitUntil(10000) {
            compose.waitForIdle()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            val buttons=compose.onAllNodesWithText("Guardar borrador").fetchSemanticsNodes()
            buttons.size==1 && !buttons.single().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)
        }
    }
    private fun assertHeader(title:String) {
        waitForLoadedForm()
        compose.onNodeWithText("Guardar borrador").assertIsDisplayed()
        compose.onNodeWithText("← Volver").assertIsDisplayed()
        compose.onNodeWithText(title).assertIsDisplayed()
        scroll("Sentido de las progresivas")
        for(text in listOf("Ruta","Calzada","Sentido de las progresivas","Creciente","Decreciente","Iniciar otro recorrido"))compose.onNodeWithText(text).assertExists()
        compose.onAllNodes(hasSetTextAction() and hasText("Ruta")).assertCountEquals(0)
        compose.onAllNodes(hasSetTextAction() and hasText("Calzada")).assertCountEquals(0)
        compose.onNodeWithText("Identificación").assertDoesNotExist()
        scroll("Guardar borrador")
    }
    private fun capture(name:String,title:String) {
        assertHeader(title)
        val bounds=JSONObject()
        for(label in listOf("Guardar borrador","← Volver",title,"Ruta","Calzada","Sentido de las progresivas")) {
            val rect=compose.onNodeWithText(label).fetchSemanticsNode().boundsInRoot
            bounds.put(if(label==title)"title" else label,listOf(rect.left,rect.top,rect.right,rect.bottom).joinToString(","))
        }
        File(output(),"$name.bounds.json").writeText(bounds.toString(2))
        screenshot(name)
    }
    private fun screenshot(name:String) {
        // PixelCopy does not complete in this Robolectric host. Draw the real Android
        // Compose view to a native bitmap; no replacement UI or synthetic illustration.
        lateinit var bitmap:Bitmap
        compose.runOnIdle {
            val view=compose.activity.window.decorView
            bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
        }
        File(output(),"$name.png").outputStream().use {assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}
    }
    private fun newForm(format:SicFieldFormat,onBack:()->Unit={}) {
        val vm=InventoryViewModel(app)
        compose.setContent {InventarioVialTheme {Surface {SicFieldWorkspace(vm,format,onBack=onBack)}}}
        waitForLoadedForm()
    }
    @Test fun canonicalCulvertUsesExtractedLegacyFormAndProducesScreenshot() {
        val vm=InventoryViewModel(app)
        compose.setContent {InventarioVialTheme {Surface {AssetFormScreen(RoadAssetType.CULVERT,vm,{},{})}}}
        capture("A_Alcantarilla_SIC18","Registrar Alcantarilla")
    }
    @Test fun fajaUsesLegacyFlowWithOne08And09FormAndProducesScreenshot() {
        newForm(SicFieldFormat.SIC08)
        capture("B_Faja_SIC08_09","Registrar Faja")
        scroll("Características de faja");compose.onNodeWithText("Características de faja").assertExists()
        scroll("Ancho de faja");compose.onNodeWithText("Ancho de faja").assertExists()
        scroll("Revisar y finalizar");compose.onNodeWithText("Revisar y finalizar").performClick()
        compose.onNodeWithText("Verificar registro antes de guardar").assertExists()
        compose.onNodeWithText("Confirmar y guardar").assertIsNotEnabled()
        screenshot("F_Faja_revision_legacy")
        compose.onNodeWithText("Revisar",useUnmergedTree=true).performClick()
        scroll("Registrar Faja")
        compose.onNodeWithText("Registrar Faja").assertExists()
    }
    @Test fun criticalPointUsesLegacyFlowAndProducesScreenshot() {
        newForm(SicFieldFormat.SIC13)
        capture("C_PuntoCritico_SIC13","Registrar Punto Crítico")
        scroll("Clase");compose.onNodeWithText("Clase").assertExists()
        compose.onNodeWithText("Lado").assertExists()
        scroll("Revisar y finalizar");compose.onNodeWithText("Revisar y finalizar").assertExists()
    }
    @Test fun accidentUsesLegacyFlowAndProducesScreenshot() {
        newForm(SicFieldFormat.SIC34)
        capture("D_Accidente_SIC34","Registrar Accidente")
        scroll("Nivel");compose.onNodeWithText("Nivel").assertExists()
        compose.onNodeWithText("Faja").assertExists();compose.onNodeWithText("Tipo").assertExists()
        scroll("Descripción");compose.onNodeWithText("Descripción").assertExists()
    }
    @Test fun newFormsUseRealRouteCatalogRoadwayChipsAndTraversalReset() {
        newForm(SicFieldFormat.SIC08)
        val catalog=com.tuempresa.inventariovial.road.ProjectRouteCatalog.load(app)
        compose.onAllNodesWithText("Seleccionar")[0].performClick()
        compose.onNodeWithText(catalog.routeCodes.first()).performClick()
        compose.onNodeWithText("${catalog.routes.first().itemLabel} · ${catalog.routes.first().segmentCode}").assertExists()
        scroll("Sentido de las progresivas")
        compose.onNodeWithText("Decreciente").performClick().assertIsSelected()
        compose.onNodeWithText("Iniciar otro recorrido").performClick()
        compose.onNodeWithText("Nuevo recorrido").assertExists()
        compose.onNodeWithText("Reiniciar continuidad").assertExists()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Decreciente").assertIsSelected()
    }
    @Test fun leavingAndReopeningLegacyFajaKeepsUuidRawFieldsDirectionAndJoinedWidths() {
        val vm=InventoryViewModel(app)
        vm.field.auth.sessions.login(com.tuempresa.inventariovial.auth.UserCacheEntity("parity-owner","parity-owner","Inspector","OPERATOR",true,"",1,1,1))
        var open by mutableStateOf(true)
        var recordId by mutableStateOf<String?>(null)
        compose.setContent {InventarioVialTheme {Surface {
            if(open)SicFieldWorkspace(vm,SicFieldFormat.SIC08,recordId) {open=false} else Text("Ficha cerrada")
        }}}
        waitForLoadedForm()
        scroll("Sentido de las progresivas");compose.onNodeWithText("Decreciente").performClick()
        scroll("Características de faja")
        compose.onNode(hasSetTextAction() and hasText("Faja")).performTextInput("7")
        scroll("Ancho útil (m)")
        compose.onNode(hasSetTextAction() and hasText("Ancho útil (m)")).performTextInput("3,")
        scroll("Guardar borrador");compose.onNodeWithText("Guardar borrador").performClick()
        compose.waitUntil(10000) {compose.onAllNodesWithText("✓ Borrador guardado en tablet").fetchSemanticsNodes().isNotEmpty()}
        val records=runBlocking {vm.field.dao.observeAllRecords().first()}
        val parent=records.single {it.sicCode=="SIC-08"}
        val width=records.single {it.sicCode=="SIC-09"}
        runBlocking {assertEquals("DECREASING",vm.field.dao.sicExtension(parent.id)!!.values()["_direction"])}
        compose.onNodeWithText("← Volver").performClick();compose.onNodeWithText("Ficha cerrada").assertExists()
        compose.runOnIdle {recordId=parent.id;open=true}
        waitForLoadedForm()
        scroll("Sentido de las progresivas");compose.onNodeWithText("Decreciente").assertIsSelected()
        scroll("Características de faja");compose.onNode(hasSetTextAction() and hasText("Faja")).assertTextContains("7")
        scroll("Ancho útil (m)");compose.onNode(hasSetTextAction() and hasText("Ancho útil (m)")).assertTextContains("3,")
        runBlocking {
            assertEquals(parent.id,vm.field.dao.sicExtension(width.id)!!.sourceRecordId)
            assertEquals(parent.id,vm.field.dao.recordById(parent.id)!!.id)
            assertEquals(2,vm.field.dao.observeAllRecords().first().size)
        }
    }
    @Test fun sic18aDestinationOpensCulvertLegacyFormWithoutIndependentHeaderOrWorkspace() {
        val vm=InventoryViewModel(app)
        vm.field.auth.sessions.login(com.tuempresa.inventariovial.auth.UserCacheEntity("parity-owner","parity-owner","Inspector","OPERATOR",true,"",1,1,1))
        runBlocking {
            vm.field.dao.insertRecord(testRecord(id="joined-culvert",sic="SIC-18"))
            vm.field.dao.insertSic18(com.tuempresa.inventariovial.data.entity.Sic18Entity("joined-culvert","06","3",2,"2",1.25,null,"3","1",sectionShape="CIRCULAR"))
        }
        compose.setContent {InventarioVialTheme {Surface {InventoryElementWorkspace(vm,SicFieldFormat.SIC18A,"joined-culvert") {}}}}
        waitForLoadedForm()
        compose.onNodeWithTag("legacy-inventory-form").assertExists()
        compose.onNodeWithText("Registrar Alcantarilla").assertExists()
        scroll("Completar detalle de condición mala");compose.onNodeWithText("Completar detalle de condición mala").performClick()
        scroll("Detalle de condición mala")
        compose.onNodeWithText("Falla estructural").assertExists()
        compose.onNodeWithText("Falla funcional").assertExists()
        compose.onAllNodes(hasSetTextAction() and hasText("Ruta")).assertCountEquals(0)
        screenshot("E_SIC18A_embebido_legacy")
    }
    @Test fun linkedSic09DestinationReopensOneFajaFormWithBothContracts() {
        val vm=InventoryViewModel(app)
        val dao=vm.field.dao
        val db=com.tuempresa.inventariovial.data.database.InventoryDatabase.getInstance(app)
        runBlocking {
            val state=SicFieldState(SicFieldFormat.SIC08,mapOf("routeCode" to "PE-3N","roadbedCode" to "UC","faja" to "2","typeCode" to "1","directionCode" to "C","startPrCode" to "0010","startDistanceM" to "5","endPrCode" to "0011","endDistanceM" to "20","surveyDate" to "09/10/2026"))
            SicFieldRepository(db).saveFaja("joined-faja",state,"joined-width",mapOf("usefulWidthM" to "3","totalWidthM" to "4"),"parity-owner",false)
        }
        compose.setContent {InventarioVialTheme {Surface {InventoryElementWorkspace(vm,SicFieldFormat.SIC09,"joined-width") {}}}}
        compose.waitUntil(10000) {compose.onAllNodesWithText("Registrar Faja").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("legacy-inventory-form").assertExists()
        compose.onNodeWithText("SIC-08 · SIC-09").assertExists()
        scroll("Ancho útil (m)");compose.onNode(hasSetTextAction() and hasText("Ancho útil (m)")).assertTextContains("3")
        runBlocking {assertEquals("joined-faja",dao.sicExtension("joined-width")!!.sourceRecordId);assertEquals(2,dao.observeAllRecords().first().size)}
    }
}
