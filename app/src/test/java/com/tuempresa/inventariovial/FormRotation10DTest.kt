package com.tuempresa.inventariovial

import android.app.Application
import android.graphics.Bitmap
import android.media.ExifInterface
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.Sic18Entity
import com.tuempresa.inventariovial.field.*
import com.tuempresa.inventariovial.repository.EngineeringEdits
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w800dp-h1200dp-mdpi",application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FormRotation10DTest {
    private val models=androidx.lifecycle.ViewModelStore()
    @get:Rule(order=0) val cleanup=object:org.junit.rules.TestWatcher(){override fun finished(d:org.junit.runner.Description){models.clear()}}
    @get:Rule(order=1) val compose=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val app get()=ApplicationProvider.getApplicationContext<Application>()
    private lateinit var vm:InventoryViewModel
    @Before fun setup()=runBlocking {
        android.provider.Settings.Secure.putString(app.contentResolver,android.provider.Settings.Secure.ANDROID_ID,"synthetic-rotation-10d")
        try {androidx.work.WorkManager.getInstance(app)}catch(_:IllegalStateException){androidx.work.WorkManager.initialize(app,androidx.work.Configuration.Builder().build())}
        val db=InventoryDatabase.getInstance(app);db.syncContext=null
        withContext(Dispatchers.IO){db.clearAllTables()}
        vm=InventoryViewModel(app);models.put("inventory",vm)
        vm.field.auth.sessions.login(UserCacheEntity("rotation-10d","rotation-10d","Synthetic inspector","OPERATOR",true,"",1,1,1))
    }
    private fun waitForm() {compose.waitUntil(10000){val nodes=compose.onAllNodesWithText("Guardar borrador").fetchSemanticsNodes();nodes.size==1 && !nodes.single().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)}}
    private fun scroll(label:String){compose.onNodeWithTag("legacy-inventory-form").performScrollToNode(hasText(label))}
    @Test fun createAndEditCoreHaveIdenticalFieldsForEveryLegacySicAndNoCulvertEndOrGnssSuggestion() {
        var code by mutableStateOf("SIC-18");var mode by mutableStateOf(SicFormMode.CREATE)
        compose.setContent {MaterialTheme {Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
            SicFormCore(mode,code,"synthetic",if(code=="SIC-20")"14" else "",testAxis(),SicCoreDraft("R","CD","0010","7","0020","9")){}
        }}}
        fun labels()=compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map {it.config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString {text->text.text}}.sorted()
        for(sic in listOf("SIC-17","SIC-18","SIC-19","SIC-20","SIC-21","SIC-22","SIC-23")) {
            compose.runOnIdle {code=sic;mode=SicFormMode.CREATE};val create=labels()
            compose.runOnIdle {mode=SicFormMode.EDIT};assertEquals("$sic fields",create,labels())
            compose.onNodeWithText("Ubicación sugerida por GNSS",substring=true).assertDoesNotExist()
            if(sic in setOf("SIC-18","SIC-22"))compose.onNodeWithText("Ubicación fin").assertDoesNotExist()
            else compose.onNodeWithText("Ubicación fin").assertExists()
        }
    }
    @Test fun workspaceRestoresIncompleteCulvertWithoutReturningHomeOrCreatingRoomRevision() {
        val restore=StateRestorationTester(compose)
        restore.setContent {MaterialTheme {InventoryWorkspace(vm,vm.field.auth)}}
        compose.onNodeWithText("NUEVO REGISTRO").performClick()
        compose.onNodeWithText("Alcantarilla",substring=true).performScrollTo().performClick()
        compose.onNodeWithText("Nuevo elemento").performClick()
        waitForm();scroll("OBSERVACIONES")
        compose.onNode(hasSetTextAction() and hasText("OBSERVACIONES")).performTextInput("Unsaved rotation 10D")
        val before=runBlocking {vm.field.dao.observeAllRecords().first().map {it.id to it.updatedAt}}
        restore.emulateSavedInstanceStateRestore()
        // The restored LazyColumn is already at OBSERVACIONES; the header is not composed.
        compose.onNodeWithTag("legacy-inventory-form").assertExists();scroll("OBSERVACIONES")
        compose.onNode(hasSetTextAction() and hasText("OBSERVACIONES")).assertTextContains("Unsaved rotation 10D")
        compose.onNodeWithText("NUEVO REGISTRO").assertDoesNotExist()
        assertEquals(before,runBlocking {vm.field.dao.observeAllRecords().first().map {it.id to it.updatedAt}})
    }
    @Test fun editingRestoresUnsavedTextAndKeepsHistoricalEndWhenCoreIsSaved()=runBlocking {
        val id="historical-culvert-10d";val r=testRecord(id=id,sic="SIC-18",route="PE-3N").copy(assetType="ALCANTARILLA",observations="Historical observations")
        withContext(Dispatchers.IO){vm.field.dao.insertRecord(r);vm.field.dao.insertSic18(Sic18Entity(id,"06","3",1,"2",1.5,null,"1","1",sectionShape="CIRCULAR"))}
        val restore=StateRestorationTester(compose)
        restore.setContent {MaterialTheme {CulvertUnifiedScreen(vm,id){}}}
        waitForm();compose.waitUntil(10000){compose.onAllNodesWithText("OBSERVACIONES").fetchSemanticsNodes().isNotEmpty() || compose.onAllNodesWithText("Ruta").fetchSemanticsNodes().isNotEmpty()}
        scroll("OBSERVACIONES");compose.onNode(hasSetTextAction() and hasText("OBSERVACIONES")).performTextReplacement("Changed but unsaved")
        restore.emulateSavedInstanceStateRestore();waitForm();scroll("OBSERVACIONES")
        compose.onNode(hasSetTextAction() and hasText("OBSERVACIONES")).assertTextContains("Changed but unsaved")
        assertEquals(r.updatedAt,vm.field.dao.recordById(id)!!.updatedAt)
        withContext(Dispatchers.IO){
            val db=InventoryDatabase.getInstance(app);val snapshot=db.inventoryDao().snapshot(id)!!
            EngineeringEdits.save(db,id,EngineeringEdits.form(snapshot),emptyList(),emptyMap(),expectedUpdatedAt=r.updatedAt,core=SicCoreDraft.from(r).copy(startDistance="123.5"))
            val saved=db.inventoryDao().recordById(id)!!
            assertEquals(id,saved.id);assertEquals(r.endPrCode,saved.endPrCode);assertEquals(r.endDistanceM,saved.endDistanceM)
            assertEquals(123.5,saved.startDistanceM,0.0);assertTrue(saved.updatedAt>r.updatedAt)
            val exported=RecordDetailPresentation.exportRow(db.inventoryDao().snapshot(id)!!)
            assertEquals(123.5,exported.record.startDistanceM,0.0)
        }
    }
    @Test fun photoPreviewRestoresAndDecoderRespectsExifWithoutChangingOriginal() {
        val file=File(app.cacheDir,"rotation-preview-10d.jpg");val bitmap=Bitmap.createBitmap(80,40,Bitmap.Config.ARGB_8888)
        file.outputStream().use {assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG,95,it))};bitmap.recycle()
        ExifInterface(file.path).apply {setAttribute(ExifInterface.TAG_ORIENTATION,"6");saveAttributes()}
        val bytes=file.readBytes();val decoded=com.tuempresa.inventariovial.camera.loadCorrectlyOrientedBitmap(file.path)!!
        assertEquals(40,decoded.width);assertEquals(80,decoded.height);assertArrayEquals(bytes,file.readBytes())
        val restore=StateRestorationTester(compose)
        restore.setContent {MaterialTheme {InventoryPhotoReview(listOf(file.path))}}
        compose.onNodeWithTag("review-photo-0").performClick()
        compose.waitUntil(10000){compose.onAllNodesWithText("Cerrar").fetchSemanticsNodes().size==1}
        restore.emulateSavedInstanceStateRestore()
        try {compose.waitUntil(10000){compose.onAllNodesWithText("Cerrar").fetchSemanticsNodes().size==1}}
        catch(e:Exception){println(compose.onAllNodes(isRoot(),useUnmergedTree=true).printToString());throw e}
        compose.onNodeWithText("Cerrar").performClick();assertArrayEquals(bytes,file.readBytes())
    }
}
