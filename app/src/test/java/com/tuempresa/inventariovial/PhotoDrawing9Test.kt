package com.tuempresa.inventariovial

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.field.LocalPhotoImage
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,qualifiers="w800dp-h1280dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoDrawing9Test {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private fun drawFrame(name: String? = null) = compose.runOnIdle {
        val view = compose.activity.window.decorView
        val frame = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        try {
            view.draw(android.graphics.Canvas(frame))
            if(name!=null) {
                val file=File("../docs/campo-9/capturas/$name.png")
                file.parentFile!!.mkdirs()
                file.outputStream().use { frame.compress(Bitmap.CompressFormat.PNG,100,it) }
            }
        } finally { frame.recycle() }
    }
    @Test fun decodedThumbnailCanBeDrawn() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val file = File(app.filesDir,"p9-drawing.jpg")
        val bitmap = Bitmap.createBitmap(64,48,Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.GREEN)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG,95,it) }
        bitmap.recycle()
        compose.setContent { MaterialTheme { LocalPhotoImage(file.path,480,Modifier.size(148.dp,112.dp)) } }
        compose.waitUntil(30_000) { compose.onAllNodesWithContentDescription("Fotografía del elemento").fetchSemanticsNodes().isNotEmpty() }
        drawFrame()
    }
    private fun seedAndModel(photoCount: Int = 1, missing: Boolean = false): Pair<InventoryViewModel,String> {
        val app = ApplicationProvider.getApplicationContext<Application>()
        try { androidx.work.WorkManager.getInstance(app) } catch (_:IllegalStateException) {
            androidx.work.WorkManager.initialize(app,androidx.work.Configuration.Builder().build())
        }
        val vm = InventoryViewModel(app)
        val db = InventoryDatabase.getInstance(app)
        val id = "p9-draw"
        val file = File(app.filesDir,"p9-integrated.jpg")
        val b = Bitmap.createBitmap(64,48,Bitmap.Config.ARGB_8888)
        file.outputStream().use { b.compress(Bitmap.CompressFormat.JPEG,95,it) }; b.recycle()
        runBlocking { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.clearAllTables() // Robolectric sandbox only, never device storage.
            db.inventoryDao().insertRecord(testRecord(id,sic="SIC-18"))
            db.inventoryDao().insertSic18(Sic18Entity(id,"06","1",1,"1",1.0,1.0,"1","1"))
            repeat(photoCount) { index ->
                db.inventoryDao().insertPhoto(PhotoEntity("p9-photo-$index",id,index+1,
                    if(missing) File(app.filesDir,"missing-$index.jpg").path else file.path,
                    index==0,null,null,null,"PENDING",1,photoCategory=if(index==0) null else "CULVERT_INLET"))
            }
        } }
        return vm to id
    }
    private fun checkDetail(photoCount: Int, missing: Boolean = false) {
        val (vm,id) = seedAndModel(photoCount,missing)
        val models = androidx.lifecycle.ViewModelStore().apply { put("p9",vm) }
        try {
            compose.setContent { MaterialTheme { Surface { RecordDetailScreen(vm,id) {} } } }
            compose.waitUntil(20_000) { compose.onAllNodesWithText("VER FOTOGRAFÍAS").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("VER FOTOGRAFÍAS").performScrollTo().performClick()
            compose.onNodeWithText("Fotografías ($photoCount)").assertExists()
            if(photoCount>0 && !missing) compose.waitUntil(20_000) {
                compose.onAllNodesWithContentDescription("Fotografía del elemento").fetchSemanticsNodes().size==photoCount
            }
            drawFrame("detalle-$photoCount-$missing")
            if(photoCount>0) {
                compose.onAllNodesWithTag("review-photo-0").onFirst().performClick()
                compose.onNodeWithText("Cerrar").assertExists()
                drawFrame()
                compose.onNodeWithText("Cerrar").performClick()
            }
            compose.onNodeWithText("Volver al detalle").performScrollTo().performClick()
            compose.onNodeWithText("VER FOTOGRAFÍAS").performScrollTo().performClick()
            if(photoCount>0 && !missing) compose.waitUntil(20_000) {
                compose.onAllNodesWithContentDescription("Fotografía del elemento").fetchSemanticsNodes().size==photoCount
            }
            drawFrame()
            val db=InventoryDatabase.getInstance(ApplicationProvider.getApplicationContext<Application>())
            runBlocking {
                assertEquals(id,db.inventoryDao().snapshot(id)!!.record.id)
                assertEquals(photoCount,db.inventoryDao().snapshot(id)!!.photos.size)
                assertNull(db.automaticSyncDao().get("SIC/$id"))
            }
        } finally { models.clear() }
    }
    @Test fun detailGalleryDrawsHistoricalPhoto() = checkDetail(1)
    @Test fun detailGalleryDrawsMultipleCategories() = checkDetail(3)
    @Test fun detailGalleryWithMissingFiles() = checkDetail(1,true)
    @Test fun detailGalleryWithoutPhotos() = checkDetail(0)
    @Test @Config(qualifiers="w1280dp-h800dp-mdpi") fun detailGalleryInLandscape() = checkDetail(1)
    @Test fun advancedExportsDrawsSelectedPhoto() {
        val (vm,_) = seedAndModel()
        val models = androidx.lifecycle.ViewModelStore().apply { put("p9",vm) }
        try {
            compose.setContent { MaterialTheme { Surface { SicExportScreen(vm) {} } } }
            compose.waitUntil(20_000) { vm.history.value.isNotEmpty() }
            compose.onNodeWithText("Los registros y borradores ya están guardados localmente.").performScrollTo()
            compose.waitUntil(20_000) { compose.onAllNodesWithContentDescription("Fotografía del elemento").fetchSemanticsNodes().isNotEmpty() }
            drawFrame("exportaciones-con-foto")
            compose.onNodeWithText("Proyecto o servicio (opcional)").performScrollTo().performTextInput("Recomposición")
            compose.onNodeWithText("Los registros y borradores ya están guardados localmente.").performScrollTo()
            drawFrame()
        } finally { models.clear() }
    }
    @Test fun advancedExportsWithoutRecords() {
        val (vm,_) = seedAndModel(0)
        val models = androidx.lifecycle.ViewModelStore().apply { put("p9",vm) }
        runBlocking { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            InventoryDatabase.getInstance(ApplicationProvider.getApplicationContext<Application>()).clearAllTables()
        } }
        try {
            compose.setContent { MaterialTheme { Surface { SicExportScreen(vm) {} } } }
            compose.onNodeWithText("Registros activos en la selección: 0").performScrollTo().assertExists()
            compose.onNodeWithText("Generar ZIP de Excel").performScrollTo().assertIsNotEnabled()
            drawFrame()
        } finally { models.clear() }
    }
}
