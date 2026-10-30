package com.tuempresa.inventariovial

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.room.Room
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapRepository
import com.tuempresa.inventariovial.scap.domain.ScapBridgeType
import com.tuempresa.inventariovial.scap.ui.*
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File
import java.util.zip.ZipFile

/** Real Compose components rendered locally with fictitious Room data; no ADB or Google. */
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class,qualifiers="w800dp-h1000dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FieldCorrectionsUi20261023Test {
    @get:Rule val ui=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Before fun workers(){initializeIdleUiWorkers()}
    private fun capture(name:String){ui.runOnIdle {
        val view=ui.activity.window.decorView;val b=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        view.draw(Canvas(b));val f=File("../docs/campo-20261023/previews/$name.png").apply{parentFile!!.mkdirs()}
        f.outputStream().use{assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,it))};b.recycle()
    }}
    @Test fun fixedTabsSwitchSingleFormAndPersistAnEditWithoutCreatingRecords()=runBlocking {
        val context=ui.activity
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use{db->
            val repo=ScapRepository(db,ScapCatalog.load(context));val id=repo.create("FICTICIO","FICTICIO",null,ScapBridgeType.MODULAR)
            val state=mutableStateOf(repo.dao.snapshot(id)!!)
            val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
            val controller=ScapController(context,db,scope)
            ui.setContent{InventarioVialTheme{Surface{Column(Modifier.fillMaxSize().padding(12.dp)) {
                ScapExportActions(true,false,{},{})
                ScapBridgeSicPanel(state.value,controller,true)
            }}}}
            ui.onNodeWithText("SIC-17A",useUnmergedTree=true).performClick()
            ui.onNodeWithTag("sic-input-SIC-17A-nearbyTown").performScrollTo().performTextInput("POBLADO FICTICIO")
            ui.waitUntil(20_000){controller.pending.value==0}
            state.value=repo.dao.snapshot(id)!!
            ui.onNodeWithText("SIC-17B",useUnmergedTree=true).performClick()
            ui.onNodeWithText("SIC-17B · CARACTERÍSTICAS DEL PUENTE").assertExists()
            ui.onNodeWithTag("sic-input-SIC-17A-nearbyTown").assertDoesNotExist()
            ui.onNodeWithText("SIC-17A",useUnmergedTree=true).performClick()
            ui.onNodeWithTag("sic-input-SIC-17A-nearbyTown").performScrollTo().assertTextContains("POBLADO FICTICIO")
            capture("01-sic17a-pestanas-reales-compose")
            controller.navigate(com.tuempresa.inventariovial.scap.domain.ScapFieldRoute("SIC",repeatableItemId="SIC-17B"))
            ui.onNodeWithText("SIC-17B · CARACTERÍSTICAS DEL PUENTE").assertExists()
            assertEquals(1,repo.dao.allSnapshots().size);assertEquals("POBLADO FICTICIO",repo.dao.snapshot(id)!!.sic17a?.nearbyTown)
            scope.cancel()
        }
    }
    @Test fun visibleActionsAreDisabledDuringAnExport() {
        var calls=0;val busy=mutableStateOf(false)
        ui.setContent{InventarioVialTheme{Surface{Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("SCAP · EJEMPLO FICTICIO")
            ScapExportActions(true,busy.value,{calls++;busy.value=true},{calls++;busy.value=true})
            if(busy.value){LinearProgressIndicator();Text("Preparando exportación…")}
        }}}}
        ui.onNodeWithText("EXPORTAR SCAP").performClick()
        ui.onNodeWithText("EXPORTAR SCAP").assertIsNotEnabled();ui.onNodeWithText("EXPORTAR SIC").assertIsNotEnabled()
        assertEquals(1,calls);capture("02-acciones-reales-compose")
    }
    @Test @Config(qualifiers="w360dp-h800dp-mdpi") fun phoneWithLargeFontKeepsBothActionsVisible() {
        ui.setContent{CompositionLocalProvider(LocalDensity provides Density(1f,1.4f)){InventarioVialTheme{Surface {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                ScapExportActions(true,false,{},{})
                BridgeSicTabs("SIC-17A"){}
            }
        }}}}
        ui.onNodeWithText("EXPORTAR SIC").assertIsDisplayed();ui.onNodeWithText("EXPORTAR SCAP").assertIsDisplayed()
        capture("03-telefono-fuente-grande-compose")
    }
    @Test fun visibleExportRequestQueuesDurablePackageContainingRealWorkbookWithoutClaimingPublication()=runBlocking {
        val context=ui.activity
        org.robolectric.shadows.ShadowStatFs.registerStats(context.filesDir.path,1_000_000,900_000,900_000)
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use{db->
            val repo=ScapRepository(db,ScapCatalog.load(context));val id=repo.create("FICTICIO","FICTICIO",null,ScapBridgeType.MODULAR)
            repo.setField(id,"inspection","route","PE-28H")
            val s=repo.dao.snapshot(id)!!;val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
            val controller=ScapController(context,db,scope)
            var status:String?=null
            ui.setContent{InventarioVialTheme{Surface{Column {
                ScapExportPanel(s,true,controller,requestedAction="SCAP:FICTICIO",onStatus={_,m->status=m})
            }}}}
            ui.onNodeWithText("Preparar copia local").performClick()
            ui.waitUntil(30_000){runBlocking{db.localFilesDao().jobs().any{it.recordId==id&&it.scope=="ZIP"}}}
            val job=db.localFilesDao().jobs().single{it.recordId==id&&it.scope=="ZIP"}
            assertEquals(s.inspection.updatedAt,job.revision);assertEquals("QUEUED",job.state)
            val plan=com.tuempresa.inventariovial.localfiles.LocalExportBuilder.build(context,db,job)
            val file=File(plan.files.single{it.mimeType=="application/zip"}.source)
            assertTrue(file.isFile);ZipFile(file).use {archive->
                val workbook=archive.entries().asSequence().single{it.name.endsWith("SCAP_PROVISIONAL.xlsx")||it.name.endsWith("SCAP.xlsx")}
                java.util.zip.ZipInputStream(archive.getInputStream(workbook)).use{excel->
                    var found=false
                    while(true){val e=excel.nextEntry ?: break;if(e.name=="xl/workbook.xml")found=true}
                    assertTrue(found)
                }
            }
            assertTrue(status?.contains("guardado",ignoreCase=true)!=true) // Picker launch is not verified publication.
            assertTrue(repo.dao.snapshot(id)!!.photos.isEmpty());assertTrue(db.automaticSyncDao().all().isEmpty())
            scope.cancel()
        }
    }
}
