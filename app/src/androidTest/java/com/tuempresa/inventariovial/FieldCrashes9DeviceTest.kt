package com.tuempresa.inventariovial

import android.app.Application
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Run only with the credential-free validation APK on a disposable emulator. No table wipe. */
@RunWith(AndroidJUnit4::class)
class FieldCrashes9DeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val app get() = ApplicationProvider.getApplicationContext<Application>()
    private val db get() = InventoryDatabase.getInstance(app)
    private val models = ViewModelStore()
    private lateinit var vm: InventoryViewModel
    private val route = "P9-${UUID.randomUUID()}"

    @Before fun prepare() {
        check(BuildConfig.DRIVE_WEB_APP_URL.isBlank() && BuildConfig.SERVER_BASE_URL.isBlank()) {
            "These tests require -PgboValidationBuild=true; never run against field data."
        }
        vm = InventoryViewModel(app)
        models.put("prompt9", vm)
    }
    @After fun cleanup() { models.clear() }

    private fun seed(photoCount: Int, missing: Boolean = false): String = runBlocking {
        val id = UUID.randomUUID().toString()
        db.inventoryDao().insertRecord(InventoryRecordEntity(id,"SIC-18","ALCANTARILLA",route,"CD","0010",125.0,
            null,null,"D",null,null,null,null,"12/10/2026","Fixture Prompt 9","ACTIVE","PENDING","PENDING",1,1))
        db.inventoryDao().insertSic18(Sic18Entity(id,"06","1",1,"1",1.0,1.0,"1","1"))
        repeat(photoCount) { index ->
            val file = File(app.filesDir,"p9-$id-$index.jpg")
            if (!missing) {
                val bitmap = Bitmap.createBitmap(64,48,Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.GREEN)
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG,95,it) }
                bitmap.recycle()
            }
            db.inventoryDao().insertPhoto(PhotoEntity(UUID.randomUUID().toString(),id,index+1,file.path,index==0,
                null,null,null,"PENDING",1,photoCategory=if(index==0) null else "CULVERT_INLET"))
        }
        id
    }
    private fun waitFor(text: String) {
        compose.waitUntil(20_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val bitmap = requireNotNull(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        File(app.getExternalFilesDir(null),"p9-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
    }
    private fun openGallery(count: Int, missing: Boolean = false) {
        val id = seed(count,missing)
        compose.setContent { InventarioVialTheme { Surface { RecordHistoryScreen(vm) {} } } }
        waitFor("Registros locales")
        compose.waitUntil(20_000) { vm.sequencedHistory.value.any { it.item.record.id==id } }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(route,substring=true))
        compose.onNode(hasClickAction() and hasAnyDescendant(hasText(route,substring=true)),useUnmergedTree=true).performClick()
        waitFor("VER FOTOGRAFÍAS")
        compose.onNodeWithText("VER FOTOGRAFÍAS").performScrollTo().performClick()
        waitFor("Fotografías ($count)")
        // Allow decoding and hardware drawing to complete: semantics alone does not exercise Canvas.
        compose.waitUntil(10_000) {
            count==0 || compose.onAllNodesWithContentDescription("Fotografía del elemento").fetchSemanticsNodes().size==count || missing
        }
        screenshot("gallery-$count-$missing")
        if (count>0) {
            compose.onAllNodesWithTag("review-photo-0").onFirst().performClick()
            waitFor("Cerrar")
            screenshot("preview-$count-$missing")
            compose.onNodeWithText("Cerrar").performClick()
        }
        compose.onNodeWithText("Volver al detalle").performScrollTo().performClick()
        waitFor("VER FOTOGRAFÍAS")
        compose.onNodeWithText("VER FOTOGRAFÍAS").performScrollTo().performClick()
        screenshot("gallery-reopened-$count-$missing")
        runBlocking { assertEquals(count,db.inventoryDao().snapshot(id)!!.photos.size) }
    }

    @Test fun galleryFromHistoryWithOneHistoricalPhoto() = openGallery(1)
    @Test fun galleryFromHistoryWithMultiplePhotos() = openGallery(3)
    @Test fun galleryFromHistoryWithNoPhotos() = openGallery(0)
    @Test fun galleryWithMissingOriginal() = openGallery(1,true)
    @Test fun advancedExportsWithPhotos() {
        seed(2)
        compose.setContent { InventarioVialTheme { Surface { SicExportScreen(vm) {} } } }
        waitFor("Exportar SIC a Excel")
        compose.onNodeWithText("Los registros y borradores ya están guardados localmente.").performScrollTo()
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Fotografía del elemento").fetchSemanticsNodes().isNotEmpty() }
        screenshot("advanced-exports")
        compose.onNodeWithText("Proyecto o servicio (opcional)").performScrollTo().performTextInput("Prueba offline")
        screenshot("advanced-exports-recompose")
    }
    @Test fun advancedExportsWithoutRecords() {
        // Fresh emulator/process required for the empty-database scenario.
        Assume.assumeTrue(runBlocking { db.inventoryDao().recordsForExport(null,null).isEmpty() })
        compose.setContent { InventarioVialTheme { Surface { SicExportScreen(vm) {} } } }
        waitFor("Exportar SIC a Excel")
        compose.onNodeWithText("Registros activos en la selección: 0").performScrollTo().assertExists()
        compose.onNodeWithText("Generar ZIP de Excel").performScrollTo().assertIsNotEnabled()
        screenshot("advanced-exports-empty")
    }
}
