package com.tuempresa.inventariovial

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.core.DriveSyncIntent
import com.tuempresa.inventariovial.dashboard.*
import com.tuempresa.inventariovial.data.entity.FieldSession
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/** Native Compose views with fictional data. No AuthGraph, database or network is used. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RutaClaraDashboardTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val day = LocalDate.of(2026, 10, 15)
    private val time = day.atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val session = FieldSession("fiction", "Proyecto ficticio · Ruta Clara", "Operador de prueba", "fixture", "PE-3N", null, "CD", "INCREASING", time)
    private val draft = testRecord("fictitious-draft", "0041", 325.5, time, "DRAFT", "PE-3N", sic = "SIC-18").copy(sessionId = "fiction", endPrCode = null, endDistanceM = null)
    private val clicks = mutableListOf<String>()
    private fun dashboard(font: Float = 1f) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, font)) {
                InventarioVialTheme {
                    DashboardScreen(session, listOf(session), listOf(draft), emptyList(), DashboardDeliveryCounts(7, 3, 2, 1), true, day,
                        { clicks += "session" }, { clicks += "new" }, { clicks += "records" }, { clicks += "pavement" },
                        { clicks += "drive" }, { clicks += "tools" }, { clicks += "all" }, { clicks += it.id }, { clicks += "map" })
                }
            }
        }
    }
    private fun screenshot(name: String) {
        // Settle the last interaction/frame before taking the native-view snapshot.
        compose.mainClock.advanceTimeBy(250)
        compose.waitForIdle()
        val bitmap = compose.runOnIdle {
            val view = compose.activity.window.decorView
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
        val file = File("build/outputs/ruta-clara-etapa-b/renderizados-locales/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }
    @Test @Config(qualifiers = "w800dp-h1280dp-port-mdpi")
    fun portraitKeepsAllEightAccessesAndOriginalDraftIdentity() {
        dashboard()
        compose.onNodeWithTag("ruta-clara-dashboard-header").assertIsDisplayed()
        screenshot("dashboard-vertical")
        compose.onNodeWithTag("session-action").performScrollTo().performClick()
        for (label in listOf("NUEVO REGISTRO", "MIS REGISTROS", "PAVIMENTOS", "ENTREGAS DRIVE", "MAPA")) {
            compose.onAllNodesWithTag(label).assertCountEquals(1)
            compose.onNodeWithTag(label).performScrollTo().assertIsDisplayed().performClick()
        }
        compose.onNodeWithTag("all-drafts").performScrollTo().performClick()
        compose.onNodeWithTag("tools").performScrollTo().performClick()
        compose.onNodeWithTag("continue-fictitious-draft").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("session", "new", "records", "pavement", "drive", "map", "all", "tools", draft.id), clicks) }
    }
    @Test @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    fun landscapeShowsPanoramicHeaderAndTabletActions() {
        dashboard()
        compose.onNodeWithText("GBO Inventario Vial").assertIsDisplayed()
        compose.onNodeWithTag("MAPA").assertIsDisplayed()
        compose.onNodeWithText("Con conexión · 4 entregas pendientes").assertIsDisplayed()
        screenshot("dashboard-horizontal")
    }
    @Test @Config(qualifiers = "w800dp-h1280dp-port-mdpi")
    fun twoHundredPercentTextStillReachesEveryActionAndAllDriveStates() {
        dashboard(2f)
        for (tag in listOf("session-action", "NUEVO REGISTRO", "MIS REGISTROS", "PAVIMENTOS", "ENTREGAS DRIVE", "MAPA", "all-drafts", "tools")) {
            compose.onNodeWithTag("dashboard").performScrollToNode(hasTestTag(tag))
            compose.onNodeWithTag(tag).assertIsDisplayed().assertHasClickAction()
        }
        for (text in listOf("Sincronizados: 7", "Pendientes: 3 · Actualizaciones pendientes: 1", "Errores: 2")) {
            compose.onNodeWithTag("dashboard").performScrollToNode(hasText(text))
            compose.onNodeWithText(text).assertIsDisplayed()
        }
        compose.onNodeWithTag("dashboard").performScrollToIndex(0)
        screenshot("dashboard-vertical-fuente-200")
    }
    @Test @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    fun driveSummaryUsesRealAckClassificationAndDoesNotTreatUploadAsConfirmed() {
        val intents = listOf(
            DriveSyncIntent("SIC/ack", "SIC", "ack", 4, 4, "SYNCED", receiptJson = "{\"ok\":true,\"key\":\"SIC/ack\",\"revision\":4}", lastSyncedAt = 1),
            DriveSyncIntent("SIC/noack", "SIC", "noack", 4, 4, "SYNCED"),
            DriveSyncIntent("SIC/upload", "SIC", "upload", 1, 0, "UPLOADING"),
            DriveSyncIntent("SIC/update", "SIC", "update", 5, 4, "UPDATE_PENDING"),
            DriveSyncIntent("SIC/error", "SIC", "error", 1, 0, "BLOCKED"),
            DriveSyncIntent("SIC/draft", "SIC", "draft", 1, 0, "DRAFT"))
        compose.setContent { InventarioVialTheme { DashboardScreen(null, emptyList(), emptyList(), emptyList(), DashboardDeliveryCounts.from(intents), false, day, {}, {}, {}, {}, {}, {}, {}, {}) } }
        for (text in listOf("Sincronizados: 1", "Pendientes: 1 · Actualizaciones pendientes: 1", "Errores: 2")) {
            compose.onNodeWithText(text).performScrollTo().assertIsDisplayed()
        }
        screenshot("dashboard-estados-drive")
    }
    private fun tools(font: Float = 1f) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, font)) {
                InventarioVialTheme { DashboardToolsScreen({ clicks += "back" }, { clicks += "map" }, { clicks += "export" }, { clicks += "bridge" },
                    { Text("Herramientas de campo · contenido de prueba") }, { Text("Ajustes · contenido de prueba") },
                    { Text("Cuenta · contenido de prueba") }, null) }
            }
        }
    }
    @Test @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    fun toolsLandscapePreservesCallbacksAndWhiteContent() {
        tools()
        screenshot("herramientas-horizontal")
        for (text in listOf("Mapa y orden de elementos", "Exportaciones avanzadas · Excel SIC", "SCAP y puentes", "VOLVER")) {
            compose.onNodeWithText(text).performScrollTo().performClick()
        }
        assertEquals(listOf("map", "export", "bridge", "back"), clicks)
    }
    @Test @Config(qualifiers = "w800dp-h1280dp-port-mdpi")
    fun toolsPortraitLargeTextRemainsScrollable() {
        tools(2f)
        compose.onNodeWithText("SCAP y puentes").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cuenta · contenido de prueba").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("VOLVER").performScrollTo()
        screenshot("herramientas-vertical-fuente-200")
    }
    private fun map() {
        val reference = RoadReferenceData(segments = listOf(RoadSegment("TEST_FICTICIO", "CD", "fixture-segment", listOf(
            RoadPolylinePoint("TEST_FICTICIO", "CD", "fixture-segment", 0, -12.00, -75.00, 0.0),
            RoadPolylinePoint("TEST_FICTICIO", "CD", "fixture-segment", 1, -12.01, -75.02, 2000.0)))))
        compose.setContent { InventarioVialTheme {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                RutaClaraMapHeader()
                Spacer(Modifier.height(16.dp))
                LiveRoadMap(reference, "TEST_FICTICIO", emptyList())
            }
        } }
        compose.onNodeWithText("MAPA OFFLINE · ORDEN EN VIVO").assertIsDisplayed()
    }
    @Test @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    fun realOfflineMapRendererKeepsZoomAndResetWithFictionalGeometry() {
        map()
        compose.onNodeWithText("+").performClick()
        compose.onNodeWithText("1.5x").assertIsDisplayed()
        compose.onNodeWithText("Restablecer").performClick()
        compose.onNodeWithText("1.0x").assertIsDisplayed()
        screenshot("mapa-horizontal-geometria-ficticia")
    }
    @Test @Config(qualifiers = "w800dp-h1280dp-port-mdpi")
    fun mapPortraitKeepsRealCanvasApartFromDecorativeHeader() {
        map()
        screenshot("mapa-vertical-geometria-ficticia")
    }
}
