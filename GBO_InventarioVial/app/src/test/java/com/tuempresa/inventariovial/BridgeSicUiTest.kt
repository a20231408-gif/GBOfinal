package com.tuempresa.inventariovial

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapRepository
import com.tuempresa.inventariovial.scap.ui.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class BridgeSicUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun supplementsAreDiscoverableEditableAndContractPendingIsDiagnosticOnly() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        val repo=ScapRepository(db,ScapCatalog.load(context))
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
        val controller=ScapController(context,db,scope)
        val id=runBlocking{repo.create("Inspector","device",null)}
        val initial=runBlocking{repo.dao.snapshot(id)!!}
        try {
            compose.setContent { MaterialTheme {
                val s by repo.dao.observe(id).collectAsState(initial)
                Column(Modifier.verticalScroll(rememberScrollState())) {ScapSicSupplementPanel(s!!,controller,true)}
            } }
            compose.onNodeWithText("DATOS PARA SIC").assertExists()
            compose.onNodeWithText("Completar SIC-17A").performScrollTo().performClick()
            compose.onNodeWithText("Latitud WGS84").performScrollTo().performTextInput("-12.123456789")
            compose.onNodeWithText("Longitud WGS84").performScrollTo().performTextInput("-77.123456789")
            compose.onNodeWithText("Número de carriles").performScrollTo().performTextInput("2")
            compose.onNodeWithText("Ancho tablero (m)").performScrollTo().performTextInput("8.40")
            compose.onNodeWithText("Ancho superestructura (m)").performScrollTo().performTextInput("9.20")
            compose.onNodeWithText("Alineamiento").performScrollTo()
            // Select the actual catalog code, never the visual option index.
            compose.onNode(hasText("Sin completar") and hasAnyAncestor(hasTestTag("sic-field-alignmentCode"))).performScrollTo().performClick()
            compose.onNodeWithText("2 - Curvo").performClick()
            compose.waitUntil(15000) {runBlocking {repo.dao.snapshot(id)!!.sic17a?.alignmentCode=="2"}}
            val a=runBlocking{repo.dao.snapshot(id)!!.sic17a!!}
            assertEquals("-12.123456789",a.latitude);assertEquals("-77.123456789",a.longitude)
            assertEquals("2",a.lanes);assertEquals("8.40",a.deckWidthM);assertEquals("9.20",a.superstructureWidthM)
            compose.onNodeWithText("Completar SIC-17B").performScrollTo().performClick()
            compose.onNodeWithText("Comentarios").performScrollTo().performTextInput("Comentario SIC\nSegunda línea")
            compose.waitUntil(15000) {runBlocking {repo.dao.snapshot(id)!!.sic17b?.comments=="Comentario SIC\nSegunda línea"}}
            compose.onNodeWithText("Capacidad máxima",substring=true).assertDoesNotExist()
            compose.onNodeWithText("Diagnóstico técnico · SIC-17B").performScrollTo().performClick()
            compose.onNodeWithText("Capacidad máxima: pendiente de definición contractual",substring=true).assertExists()
            assertNull(controller.error.value)
        } finally {scope.cancel();db.close()}
    }
    @Test fun existingLocationDisplaysWithoutDuplicatePrAndDistanceInputs() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        val repo=ScapRepository(db,ScapCatalog.load(context))
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
        val controller=ScapController(context,db,scope)
        val s=runBlocking {
            val id=repo.create("Inspector","device",null)
            repo.saveSicLocation(id,"PE-3N","UC","0042","125.5","0043","10")
            repo.dao.snapshot(id)!!
        }
        try {
            compose.setContent {MaterialTheme {Column(Modifier.verticalScroll(rememberScrollState())) {ScapSicSupplementPanel(s,controller,true)}}}
            compose.onNodeWithText("Completar SIC-17").performScrollTo().performClick()
            compose.onNodeWithText("PE-3N · UC · PR 0042 + 125.5 m").assertExists()
            compose.onNodeWithText("Código PR").assertDoesNotExist()
            compose.onNodeWithText("Distancia desde PR (m)").assertDoesNotExist()
        } finally {scope.cancel();db.close()}
    }
}
