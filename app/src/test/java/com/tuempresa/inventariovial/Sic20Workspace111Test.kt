package com.tuempresa.inventariovial

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.core.InventoryDraftStore
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.FieldSession
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.*
import java.io.File
import java.util.UUID

/** Real workspace/chooser/form/Room/raw draft/records/detail callbacks. Only gallery result is simulated. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,qualifiers="w1280dp-h800dp-land-mdpi",shadows=[Test111ImageFileProvider::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Sic20Workspace111Test {
    @get:Rule val ui=createAndroidComposeRule<ComponentActivity>()
    private val app get()=ApplicationProvider.getApplicationContext<Application>()
    private var models=androidx.lifecycle.ViewModelStore()
    private lateinit var vm:InventoryViewModel
    private lateinit var db:InventoryDatabase
    private val graph get()=AuthGraph.get(app)
    @Before fun setup() {
        initializeIdleUiWorkers()
        graph.sessions.login(UserCacheEntity("TEST111_NAV","test","FICTICIO","ENGINEER",true,"",1,1,1))
        runBlocking{com.tuempresa.inventariovial.localfiles.LocalCopySettings.enabled(app,false)}
        newModel()
        runBlocking{db.inventoryDao().insertSession(FieldSession("TEST111_SESSION_${UUID.randomUUID()}","TEST111","FICTICIO","FICTICIO","PE-28H",vm.field.projectRoutes.requireRoute("PE-28H").segmentCode,"CD",startTime=System.currentTimeMillis()))}
        mount();home()
    }
    @After fun close(){ui.runOnUiThread{ui.activity.setContent{}};ui.waitForIdle();ui.runOnIdle{models.clear()};graph.sessions.logout();db.close();InventoryDatabase::class.java.getDeclaredField("INSTANCE").apply{isAccessible=true}.set(null,null)}
    private fun newModel(){vm=InventoryViewModel(app);models.put("TEST111",vm);db=InventoryDatabase.getInstance(app);db.syncContext=null}
    private fun mount(){ui.activity.setContent{MaterialTheme{InventoryWorkspace(vm,graph)}}}
    private fun waitFor(tag:String){try{ui.waitUntil(25000){ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()}}catch(e:Throwable){println(ui.onRoot().printToString());throw e}}
    private fun home(){waitFor("dashboard")}
    private fun click(text:String){
        ui.waitUntil(20000){runCatching {
            ui.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToNode(hasText(text))
            ui.onNodeWithText(text).fetchSemanticsNode();true
        }.getOrDefault(false)}
        ui.onNodeWithText(text).performScrollTo().performClick()
    }
    private fun form(title:String){waitFor("legacy-inventory-form");ui.onNodeWithTag("legacy-inventory-form").performScrollToNode(hasText("Registrar $title"));ui.onNodeWithText("Registrar $title").assertExists()}
    private fun observed(id:String)=runBlocking{db.inventoryDao().recordById(id)!!}
    private fun create(choice:String,title:String,code:String):String {
        val old=runBlocking{db.inventoryDao().observeHistory().first().map{it.record.id}.toSet()}
        click("NUEVO REGISTRO");click(choice);click("Nuevo elemento");form(title)
        ui.onNodeWithTag("legacy-inventory-form").performScrollToNode(hasText("OBSERVACIONES"))
        ui.onNode(hasSetTextAction()and hasText("OBSERVACIONES")).performTextReplacement("TEST111 OBS $code")
        val image=File(app.filesDir,"TEST111_GALLERY_${UUID.randomUUID()}.jpg")
        val b=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888)
        image.outputStream().use{b.compress(Bitmap.CompressFormat.JPEG,95,it)};b.recycle()
        ui.onNodeWithTag("legacy-inventory-form").performScrollToNode(hasText("Incorporar fotos"))
        ui.onAllNodesWithText("Incorporar fotos").onFirst().performClick()
        ui.runOnUiThread{
            val request=Shadows.shadowOf(ui.activity).nextStartedActivityForResult
            assertNotNull(request)
            assertEquals(Intent.ACTION_GET_CONTENT,request.intent.action)
            val uri=Uri.parse("content://test111.gallery/${image.name}")
            Shadows.shadowOf(app.contentResolver).registerInputStream(uri,image.inputStream())
            assertTrue(ui.activity.activityResultRegistry.dispatchResult(request.requestCode,listOf(uri)))
        }
        try {ui.waitUntil(20000){ui.onAllNodes(hasText("— 1 fotos",substring=true)).fetchSemanticsNodes().isNotEmpty()}} catch(e:Throwable){println(ui.onRoot().printToString());throw e}
        click("Guardar borrador")
        var id:String?=null
        try {ui.waitUntil(25000){id=runBlocking{db.inventoryDao().observeHistory().first().map{it.record.id}.firstOrNull{it !in old}};id!=null&&InventoryDraftStore.read(app,id)!!.getJSONArray("photoPaths").length()==1}} catch(e:Throwable){println(ui.onRoot().printToString());throw AssertionError("Fictitious draft gallery/save: idExists=${id!=null}, photos=${id?.let{InventoryDraftStore.read(app,it)?.optJSONArray("photoPaths")?.length()}}",e)}
        click("Guardar borrador")
        ui.waitUntil(20000){observed(id!!).observations=="TEST111 OBS $code"}
        val raw=InventoryDraftStore.read(app,id)!!
        assertEquals(code,raw.getJSONObject("detail").getJSONObject("fields").getString("classCode"))
        assertNull(runBlocking{db.inventoryDao().snapshot(id!!)!!.sic20})
        click("← Volver");click("Volver");home()
        return id!!
    }
    private fun openRecords(id:String,title:String) {
        click("MIS REGISTROS");waitFor("mis-registros")
        ui.onNode(hasSetTextAction()and hasText("Buscar elemento o UUID")).performTextReplacement(id)
        click("Ver detalle");click("EDITAR");form(title)
        ui.onNodeWithTag("legacy-inventory-form").performScrollToNode(hasText("OBSERVACIONES"))
        ui.onNode(hasSetTextAction()and hasText("OBSERVACIONES")).assertTextContains("TEST111 OBS",substring=true)
        val raw=InventoryDraftStore.read(app,id)!!
        assertTrue(File(raw.getJSONArray("photoPaths").getString(0)).isFile)
    }
    private fun fromFormToHome(){click("← Volver");click("Volver");click("Volver");home()}
    private fun flow(choice:String,title:String,code:String) {
        val id=create(choice,title,code);openRecords(id,title)
        assertEquals(code,com.tuempresa.inventariovial.model.form.Sic20Identity.classOf(observed(id).assetType))
        click("Guardar borrador")
        assertEquals(id,observed(id).id)
        assertEquals(code,InventoryDraftStore.read(app,id)!!.getJSONObject("detail").getJSONObject("fields").getString("classCode"))
    }
    @Test fun homeContinueShortcutResolvesAllThreePersistedSubtypes() {
        for((choice,title,code)in listOf(Triple("Muros","Muro","14"),Triple("Túneles","Túnel","13"),Triple("Badenes","Badén","12"))) {
            val id=create(choice,title,code)
            ui.onNodeWithTag("dashboard").performScrollToNode(hasTestTag("daily-drafts"))
            ui.onNodeWithTag("daily-drafts").performScrollToNode(hasTestTag("continue-$id"))
            ui.onNodeWithTag("continue-$id").performScrollTo().assertIsDisplayed().performClick();form(title)
            assertEquals(code,InventoryDraftStore.read(app,id)!!.getJSONObject("detail").getJSONObject("fields").getString("classCode"))
            click("← Volver");home()
        }
    }
    @Test fun homeWallDraftHomeRecordsDetailEditKeepsWall(){flow("Muros","Muro","14")}
    @Test fun homeTunnelDraftHomeRecordsDetailEditKeepsTunnel(){flow("Túneles","Túnel","13")}
    @Test fun homeFordDraftHomeRecordsDetailEditKeepsFord(){flow("Badenes","Badén","12")}
    @Test fun multipleSubtypesAlternateAndSurviveActivitySavedState() {
        val initial=runBlocking{db.inventoryDao().observeHistory().first().size}
        val wall=create("Muros","Muro","14");val tunnel=create("Túneles","Túnel","13");val ford=create("Badenes","Badén","12")
        for((id,title)in listOf(wall to "Muro",tunnel to "Túnel",ford to "Badén",wall to "Muro")){openRecords(id,title);fromFormToHome()}
        openRecords(tunnel,"Túnel")
        ui.onNodeWithTag("legacy-inventory-form").performScrollToNode(hasText("OBSERVACIONES"))
        ui.onNode(hasSetTextAction()and hasText("OBSERVACIONES")).performTextReplacement("TEST111 OBS TRAS GIRO")
        ui.activityRule.scenario.recreate();mount();form("Túnel")
        ui.onNodeWithTag("legacy-inventory-form").performScrollToNode(hasText("OBSERVACIONES"))
        ui.onNode(hasSetTextAction()and hasText("OBSERVACIONES")).assertTextContains("TEST111 OBS TRAS GIRO")
        assertEquals(initial+3,runBlocking{db.inventoryDao().observeHistory().first().size})
    }
    @Test fun closingWorkspaceAndReopeningRoomKeepsHistoricalAliasesAndMedia() {
        val id=create("Muros","Muro","14")
        // Historical equivalent alias, never a contradictory class repair.
        runBlocking{db.inventoryDao().updateRecord(observed(id).copy(assetType="WALL"))}
        val raw=InventoryDraftStore.read(app,id)!!.toString()
        ui.runOnUiThread{ui.activity.setContent{}}
        ui.waitForIdle();ui.runOnIdle{models.clear()};db.close()
        InventoryDatabase::class.java.getDeclaredField("INSTANCE").apply{isAccessible=true}.set(null,null)
        models=androidx.lifecycle.ViewModelStore();newModel();mount();home();openRecords(id,"Muro")
        assertEquals(raw,InventoryDraftStore.read(app,id)!!.toString())
        assertEquals("WALL",observed(id).assetType)
    }
}

/** Windows host separators differ from Android FileProvider. Narrow test adapter, never shipped. */
@org.robolectric.annotation.Implements(androidx.core.content.FileProvider::class)
class Test111ImageFileProvider {
    companion object {
        @JvmStatic @org.robolectric.annotation.Implementation
        fun getUriForFile(context:android.content.Context,authority:String,file:File):Uri {
            val root=File(context.filesDir,"images").canonicalFile
            require(file.canonicalFile.toPath().startsWith(root.toPath()))
            require(authority==context.packageName+".fileprovider")
            return Uri.fromFile(file)
        }
    }
}
