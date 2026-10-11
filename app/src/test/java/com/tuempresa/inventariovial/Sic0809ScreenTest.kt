package com.tuempresa.inventariovial

import android.Manifest
import android.app.Application
import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.UserCacheEntity
import com.tuempresa.inventariovial.data.entity.FieldSession
import com.tuempresa.inventariovial.sic.*
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.Shadows.shadowOf
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w800dp-h1200dp-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class Sic0809ScreenTest {
    private val models=androidx.lifecycle.ViewModelStore()
    private lateinit var vm:InventoryViewModel
    private val app get()=ApplicationProvider.getApplicationContext<Application>()
    @get:Rule(order=0) val cleanup=object:org.junit.rules.TestWatcher() {
        override fun finished(description:org.junit.runner.Description) {models.clear();if(::vm.isInitialized)vm.field.auth.sessions.logout()}
    }
    @get:Rule(order=1) val compose=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Before fun prepareOfflineSession() {
        try {androidx.work.WorkManager.getInstance(app)} catch(_:IllegalStateException) {
            androidx.work.WorkManager.initialize(app,androidx.work.Configuration.Builder().build())
        }
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)
        android.provider.Settings.Secure.putInt(app.contentResolver,android.provider.Settings.Secure.LOCATION_MODE,0)
        shadowOf(app.getSystemService(android.net.ConnectivityManager::class.java)).setActiveNetworkInfo(null)
        android.provider.Settings.Secure.putString(app.contentResolver,android.provider.Settings.Secure.ANDROID_ID,"1234567890abcdef")
        vm=InventoryViewModel(app);models.put("test",vm)
        runBlocking {withContext(Dispatchers.IO) {com.tuempresa.inventariovial.data.database.InventoryDatabase.getInstance(app).clearAllTables()}}
        vm.field.auth.sessions.login(UserCacheEntity("sic-engineer","sic-engineer","Ingeniero","ENGINEER",true,"",1,1,1))
        runBlocking {vm.field.dao.insertSession(FieldSession("field-session","Proyecto","sic-engineer","device","PE-22A","","CD",startTime=1))}
    }
    private fun waitLoaded() {compose.waitUntil(20_000) {
        compose.waitForIdle();shadowOf(android.os.Looper.getMainLooper()).idle()
        compose.onAllNodesWithText("Guardar registro").fetchSemanticsNodes().isNotEmpty() ||
            compose.onAllNodesWithTag("legacy-inventory-form").fetchSemanticsNodes().isNotEmpty()
    }}
    private fun scroll(text:String) {compose.onNodeWithTag("legacy-inventory-form").performScrollToNode(hasText(text))}
    private fun input(label:String,value:String) {scroll(label);compose.onNode(hasSetTextAction() and hasText(label)).performTextReplacement(value)}
    private fun capture(name:String) {
        lateinit var bitmap:Bitmap
        compose.runOnIdle {val view=compose.activity.window.decorView;bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(android.graphics.Canvas(bitmap))}
        val root=File(System.getProperty("user.dir"));val directory=File(if(File(root,"app").exists())root else root.parentFile,"docs/sic08-sic09-fix-2026-10-11/screenshots").apply {mkdirs()}
        File(directory,"$name.png").outputStream().use {assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}
    }
    private fun saveAndReopen(format:SicFieldFormat) {
        var open by mutableStateOf(true)
        var id by mutableStateOf<String?>(null)
        compose.setContent {InventarioVialTheme {Surface {if(open) SicFieldWorkspace(vm,format,id) {open=false} else Text("Ficha cerrada")}}}
        waitLoaded()
        scroll(format.code);compose.onNodeWithText(format.code).assertExists();compose.onNodeWithText(format.title).assertExists()
        scroll("Ruta");compose.onAllNodesWithText("PE-22A").onFirst().assertExists()
        compose.onAllNodesWithText("CD").onFirst().assertExists()
        capture("${format.code}_inicio")
        val state=fajaState(format)
        format.fields.filter {it.key !in setOf("routeCode","roadbedCode")}.forEach {field->
            scroll(field.label)
            if(field.options.isEmpty())input(field.label,state.text(field.key))
            else if(field.key=="typeCode") {
                compose.onNodeWithText("Seleccionar").performClick();compose.onNodeWithText("1 · Tránsito").performClick()
            } else compose.onNodeWithText("C · Creciente").performClick()
            for(text in listOf("Iniciar otro recorrido","Actualizar GPS inicial","Capturar GPS final","Completar anchos","Características de faja"))
                compose.onNodeWithText(text).assertDoesNotExist()
        }
        if(format==SicFieldFormat.SIC08)compose.onNodeWithText("Ancho útil (m)").assertDoesNotExist()
        else compose.onNodeWithText("Sentido del tránsito").assertDoesNotExist()
        val notes="Inspección ${format.code}\nTexto completo con tildes áéíóú"
        scroll("OBSERVACIONES");compose.onNodeWithTag("sic-observations").performTextReplacement(notes)
        capture("${format.code}_observaciones")
        scroll("Guardar registro");compose.onNodeWithText("Guardar registro").performClick()
        compose.waitUntil(20_000) {runBlocking {vm.field.dao.observeAllRecords().first().any {it.sicCode==format.code && it.status=="ACTIVE"}}}
        val saved=runBlocking {vm.field.dao.observeAllRecords().first().single {it.sicCode==format.code}}
        assertNull(saved.latitude);assertNull(saved.longitude);assertEquals(notes,saved.observations);assertEquals("field-session",saved.sessionId)
        assertEquals(1,runBlocking {vm.field.dao.observeAllRecords().first().size})
        scroll("← Volver");compose.onNodeWithText("← Volver").performClick();compose.onNodeWithText("Ficha cerrada").assertExists()
        compose.runOnIdle {id=saved.id;open=true};waitLoaded()
        scroll("OBSERVACIONES");compose.onNodeWithTag("sic-observations").assertTextContains(notes)
        compose.onNodeWithTag("sic-observations").performTextReplacement(notes+"\nActualizado")
        scroll("Guardar registro");compose.onNodeWithText("Guardar registro").performClick()
        compose.waitUntil(20_000) {runBlocking {vm.field.dao.recordById(saved.id)?.observations==notes+"\nActualizado"}}
        assertEquals(saved.id,runBlocking {vm.field.dao.recordById(saved.id)}!!.id)
        assertEquals(0,android.provider.Settings.Secure.getInt(app.contentResolver,android.provider.Settings.Secure.LOCATION_MODE))
        assertEquals(android.content.pm.PackageManager.PERMISSION_DENIED,app.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION))
    }
    @Test fun sic08CapturesSavesReopensAndEditsWithoutLocationPermissionOrInternet() {saveAndReopen(SicFieldFormat.SIC08)}
    @Test fun sic09CapturesSavesReopensAndEditsWithoutLocationPermissionOrInternet() {saveAndReopen(SicFieldFormat.SIC09)}
    @Test fun linkedHistoricalSic09OpensItsOwnScreenWithout08Fields() {
        runBlocking {
            val db=com.tuempresa.inventariovial.data.database.InventoryDatabase.getInstance(app)
            val repo=SicFieldRepository(db);repo.save("parent",fajaState(SicFieldFormat.SIC08),"sic-engineer",true)
            repo.save("child",fajaState(SicFieldFormat.SIC09).copy(sourceRecordId="parent"),"sic-engineer",true)
        }
        compose.setContent {InventarioVialTheme {Surface {InventoryElementWorkspace(vm,SicFieldFormat.SIC09,"child") {}}}}
        waitLoaded();compose.onNodeWithText("SIC-09").assertExists();compose.onNodeWithText("SIC-08").assertDoesNotExist()
        scroll("Ancho útil (m)");compose.onNode(hasSetTextAction() and hasText("Ancho útil (m)")).assertTextContains("3.2567")
        compose.onNodeWithText("Tipo").assertDoesNotExist();compose.onNodeWithText("Sentido del tránsito").assertDoesNotExist()
    }
    @Test fun menuSelectsEachFormatIndividually() {
        var opened:SicFieldFormat?=null
        compose.setContent {InventarioVialTheme {InventoryElementDashboard {opened=it}}}
        compose.onNodeWithText("Fajas de las calzadas").performClick();assertEquals(SicFieldFormat.SIC08,opened)
        compose.onNodeWithText("Ancho de las fajas").performClick();assertEquals(SicFieldFormat.SIC09,opened)
        compose.onNodeWithText("SIC-08 · SIC-09").assertDoesNotExist()
    }
}
