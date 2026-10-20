package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.core.InventoryDraftStore
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.model.RoadAssetType
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
class Sic20FormRecoveryUiTest {
    @get:Rule val ui=createAndroidComposeRule<ComponentActivity>()
    private val models=androidx.lifecycle.ViewModelStore()
    private lateinit var vm:InventoryViewModel
    private lateinit var db:InventoryDatabase
    private var draft by mutableStateOf<InventoryRecordEntity?>(null)
    private var asset by mutableStateOf(RoadAssetType.WALL)
    @Before fun setup() {
        initializeIdleUiWorkers();val app=ApplicationProvider.getApplicationContext<Application>()
        android.provider.Settings.Secure.putString(app.contentResolver,android.provider.Settings.Secure.ANDROID_ID,"TEST_SIC20_UI")
        AuthGraph.get(app).sessions.login(UserCacheEntity("test","test","FICTICIO","OPERATOR",true,"",1,1,1))
        vm=InventoryViewModel(app);models.put("sic20-ui",vm);db=InventoryDatabase.getInstance(app);db.syncContext=null
    }
    @After fun close(){models.clear();AuthGraph.get(ApplicationProvider.getApplicationContext()).sessions.logout()}
    private fun prepare(code:String):InventoryRecordEntity {
        val c=ApplicationProvider.getApplicationContext<Context>();val id="TEST_UI_${UUID.randomUUID()}"
        val photo=File(c.filesDir,"$id.jpg").apply{writeBytes(byteArrayOf(1,2,3))}
        val request=InventorySaveRequest("SIC-20",if(code=="14")"WALL"else"FORD","PE-3N","CD","1","10","1","20","D",Double.NaN,Double.NaN,null,"19/10/2026","OBS $code",photo.path,
            SicFormDetail.Sic20(Sic20FormState(classCode=code,dimension1M="2",dimension2M=if(code=="12")"3"else"")),recordId=id)
        runBlocking{InventoryDraftStore.save(c,db,request)}
        return runBlocking{db.inventoryDao().recordById(id)!!}
    }
    private fun content(){ui.activity.setContent{MaterialTheme{AssetFormScreen(asset,vm,{},{},draft)}}}
    private fun header(title:String){ui.waitUntil(20000){ui.onAllNodesWithTag("legacy-inventory-form").fetchSemanticsNodes().isNotEmpty()};ui.onNodeWithTag("legacy-inventory-form").performScrollToNode(hasText(title));ui.onNodeWithText(title).assertExists()}
    @Test fun wallDraftKeepsCorrectFormUnsavedObservationAndUuidAfterActivityRecreation() {
        draft=prepare("14");asset=RoadAssetType.WALL;content();header("Registrar Muro")
        val observations=hasSetTextAction() and hasText("OBSERVACIONES")
        ui.onNodeWithTag("legacy-inventory-form").performScrollToNode(observations)
        ui.onNode(observations).performScrollTo().performTextReplacement("OBS TRAS GIRO")
        ui.activityRule.scenario.recreate();content();header("Registrar Muro")
        ui.onNodeWithTag("legacy-inventory-form").performScrollToNode(observations)
        ui.onNode(observations).performScrollTo().assertTextContains("OBS TRAS GIRO")
        assertEquals("WALL",runBlocking{db.inventoryDao().recordById(draft!!.id)!!.assetType})
    }
    @Test fun switchingMultipleWallAndFordDraftsDoesNotReuseTheOtherFormState() {
        val wall=prepare("14");val ford=prepare("12")
        draft=wall;asset=RoadAssetType.WALL;content();header("Registrar Muro")
        ui.onNodeWithTag("legacy-inventory-form").performScrollToNode(hasText("OBSERVACIONES"))
        ui.onNode(hasSetTextAction() and hasText("OBSERVACIONES")).performScrollTo().assertTextContains("OBS 14")
        ui.runOnIdle{draft=ford;asset=RoadAssetType.FORD};header("Registrar Badén")
        ui.onNodeWithTag("legacy-inventory-form").performScrollToNode(hasText("OBSERVACIONES"))
        ui.onNode(hasSetTextAction() and hasText("OBSERVACIONES")).performScrollTo().assertTextContains("OBS 12")
        ui.runOnIdle{draft=wall;asset=RoadAssetType.WALL};header("Registrar Muro")
        ui.onNodeWithTag("legacy-inventory-form").performScrollToNode(hasText("OBSERVACIONES"))
        ui.onNode(hasSetTextAction() and hasText("OBSERVACIONES")).performScrollTo().assertTextContains("OBS 14")
        assertEquals("WALL",runBlocking{db.inventoryDao().recordById(wall.id)!!.assetType})
        assertEquals("FORD",runBlocking{db.inventoryDao().recordById(ford.id)!!.assetType})
    }
}
