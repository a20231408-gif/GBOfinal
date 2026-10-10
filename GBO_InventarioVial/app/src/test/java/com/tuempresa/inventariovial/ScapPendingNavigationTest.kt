package com.tuempresa.inventariovial

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapRepository
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.ui.*
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w800dp-h1200dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScapPendingNavigationTest {
    private var testDb:InventoryDatabase?=null
    private var testScope:CoroutineScope?=null
    private val models=androidx.lifecycle.ViewModelStore()
    @get:Rule(order=0) val resources=object:org.junit.rules.TestWatcher(){override fun finished(description:org.junit.runner.Description){testScope?.cancel();models.clear();testDb?.close()}}
    @get:Rule(order=1) val compose=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun tappingRealPendingButtonsReachesIdentificationSupportTwoGlobalPhotoAndMetrado() {
        val app=ApplicationProvider.getApplicationContext<Application>()
        val db=Room.inMemoryDatabaseBuilder(app,InventoryDatabase::class.java).build()
        testDb=db
        val catalog=ScapCatalog.load(app);val repo=ScapRepository(db,catalog)
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
        testScope=scope
        val c=ScapController(app,db,scope);val vm=InventoryViewModel(app)
        models.put("inventory",vm)
        val initial=runBlocking {
            val id=repo.create("Inspector","device",null,ScapBridgeType.SLAB)
            val support=repo.dao.snapshot(id)!!.supports.single{it.supportIndex==2}
            val type=catalog.forType(ScapBridgeType.SLAB).fields("C5","bearing").single{it.key=="type"}
            repo.setField(id,support.id,"type",catalog.forType(ScapBridgeType.SLAB).options(type.catalog).first())
            repo.addPhotoRecord(id,"AGUAS ABAJO","")
            repo.dao.snapshot(id)!!
        }
        try {
            val issues=ScapCompletion.assess(initial,catalog.forSnapshot(initial)).pending
            val support=initial.supports.single{it.supportIndex==2}
            val e=initial.elements.last().element
            val photo=initial.photos.single()
            val targets=listOf(
                issues.first{it.section=="A"&&it.key=="bridgeName"},
                issues.first{it.ownerId==support.id&&it.key=="material"},
                issues.first{it.ownerId==e.id&&it.key=="globalDescription"},
                issues.first{it.ownerId==photo.id&&it.key=="photo"},
                issues.first{it.ownerId==e.id&&it.key=="quantity"})
            compose.setContent {MaterialTheme {
                val s by repo.dao.observe(initial.inspection.id).collectAsState(initial)
                val pending by c.pending.collectAsState();val error by c.error.collectAsState()
                ScapEditor(s!!,c,catalog.forSnapshot(s!!),vm,"Guardado local",pending,error)
            }}
            compose.onNodeWithText("Guardar borrador").performClick()
            compose.waitUntil(15000){compose.waitForIdle();assertNull(c.error.value);runBlocking{repo.dao.snapshot(initial.inspection.id)!!.inspection.status=="DRAFT"}}
            for(issue in targets) {
                compose.onNodeWithText("Ver pendientes").performClick()
                compose.onNodeWithText("${issue.section} · ${issue.label}").performScrollTo().performClick()
                compose.mainClock.advanceTimeBy(700)
                compose.waitForIdle()
                compose.onNodeWithTag("scap:${issue.ownerId}:${issue.key}",useUnmergedTree=true).assertIsDisplayed()
                val directory=java.io.File("../docs/tablet-admin-stability-scap-evidence/screenshots").apply{mkdirs()}
                lateinit var bitmap:android.graphics.Bitmap
                compose.runOnIdle {
                    val view=compose.activity.window.decorView
                    bitmap=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
                    view.draw(android.graphics.Canvas(bitmap))
                }
                java.io.File(directory,"pending_${issue.section}.png").outputStream().use{assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it))}
                bitmap.recycle()
                assertEquals(issue.route,c.destination.value!!.second)
                if(issue.key in setOf("bridgeName","globalDescription","quantity")) {
                    compose.onNode(isFocused() and hasAnyAncestor(hasTestTag("scap:${issue.ownerId}:${issue.key}")),useUnmergedTree=true).assertExists()
                }
            }
            assertNull(c.error.value)
        }finally{scope.cancel()}
    }
}
