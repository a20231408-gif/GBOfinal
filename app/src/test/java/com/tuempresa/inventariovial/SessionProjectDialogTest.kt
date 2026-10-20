package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.FieldSession
import com.tuempresa.inventariovial.field.FieldController
import com.tuempresa.inventariovial.field.SessionDialog
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w800dp-h1280dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SessionProjectDialogTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var db: InventoryDatabase
    private lateinit var field: FieldController
    private lateinit var graph: AuthGraph
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var day by mutableStateOf(LocalDate.of(2026, 10, 15))
    private var show by mutableStateOf(true)
    private var generation by mutableIntStateOf(0)
    private val closed = AtomicInteger()
    @Before fun setup() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        Settings.Secure.putString(app.contentResolver, Settings.Secure.ANDROID_ID, "session-project-fixture")
        app.getSharedPreferences("user_service", Context.MODE_PRIVATE).edit().clear().commit()
        AuthGraph::class.java.getDeclaredField("instance").apply { isAccessible = true }.set(null, null)
        graph = AuthGraph.get(app)
        graph.sessions.login(UserCacheEntity("fixture-operator", "fixture-operator", "Operador ficticio", "OPERATOR", true, "opaque", 1, 1, 1))
        db = Room.inMemoryDatabaseBuilder(app, InventoryDatabase::class.java).build()
        db.syncContext = null
        // Safe for ordinary Debug too: no test in this class executes a queued worker.
        try { androidx.work.WorkManager.getInstance(app) }
        catch (_: IllegalStateException) {
            androidx.work.WorkManager.initialize(app, androidx.work.Configuration.Builder().setExecutor {}.setTaskExecutor {}.build())
        }
        field = FieldController(app, db, scope, graph.sessions)
    }
    @After fun close() {
        runBlocking { scope.coroutineContext[Job]!!.cancelAndJoin() }
        graph.sessions.logout()
        db.close()
    }
    private fun content(initial: FieldSession? = null, font: Float = 1f) {
        compose.activity.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, font)) {
                InventarioVialTheme {
                    Text("Fixture $generation")
                    if (show) SessionDialog(field, { show = false; closed.incrementAndGet() }, initial, today = { day })
                }
            }
        }
    }
    private fun project() = compose.onNode(hasSetTextAction() and hasText("Proyecto"))
    private fun active() = runBlocking { db.inventoryDao().currentSession() }
    private fun waitClosed() { compose.waitUntil(15_000) { closed.get() > 0 } }
    @Test fun defaultIsEditableAndRecompositionDoesNotReplaceItAndNextOpeningUsesNextDay() {
        content()
        project().assertTextContains("Inventario Vial - 15/10/2026")
        project().performTextReplacement("Mi proyecto completamente editable")
        compose.runOnIdle { day = day.plusDays(1); generation++ }
        project().assertTextContains("Mi proyecto completamente editable")
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle { show = true }
        project().assertTextContains("Inventario Vial - 16/10/2026")
        assertNull(active())
    }
    @Test fun savingNewSessionPersistsExactlyTheDisplayedDefaultAndAuthenticatedOperator() {
        content()
        project().assertTextContains("Inventario Vial - 15/10/2026")
        compose.onNodeWithText("Sin ruta").performScrollTo().performClick()
        compose.onNodeWithText("PE-22A").performClick()
        compose.onNodeWithText("Seleccionar").performScrollTo().performClick()
        compose.onNodeWithText("CD").performClick()
        compose.onNodeWithText("Recorrido decreciente").performScrollTo()
        compose.onNode(isToggleable()).performClick()
        val before = System.currentTimeMillis()
        compose.onNodeWithText("Iniciar").performClick(); waitClosed()
        val saved = active()!!
        assertEquals("Inventario Vial - 15/10/2026", saved.project)
        assertEquals("Operador ficticio", saved.operator)
        assertEquals("PE-22A", saved.road); assertEquals("CD", saved.roadbed)
        assertEquals("DECREASING", saved.direction)
        assertEquals(saved.sessionId, UUID.fromString(saved.sessionId).toString())
        assertTrue(saved.startTime >= before && saved.startTime <= System.currentTimeMillis())
    }
    @Test fun configuringKeepsSavedNameIdentityOperatorTimeAndHistoricalRecords() = runBlocking {
        val initial = FieldSession("historical-session", "Proyecto histórico de 2020", "Ingeniero histórico", "fixture", "PE-22A", field.projectRoutes.segment("PE-22A"), "CD", "DECREASING", 1000)
        db.inventoryDao().insertSession(initial)
        val record = testRecord("old-record").copy(sessionId = initial.sessionId, observations = "No modificar")
        db.inventoryDao().insertRecord(record)
        content(initial)
        project().assertTextContains(initial.project)
        project().performTextReplacement("  Nombre editado literalmente  ")
        compose.onNodeWithText("Guardar configuración").performClick(); waitClosed()
        val saved = active()!!
        assertEquals("  Nombre editado literalmente  ", saved.project)
        assertEquals(initial.sessionId, saved.sessionId); assertEquals(initial.startTime, saved.startTime)
        assertEquals(initial.operator, saved.operator); assertEquals(initial.device, saved.device)
        assertEquals(initial.road, saved.road); assertEquals(initial.roadbed, saved.roadbed); assertEquals(initial.direction, saved.direction)
        assertEquals(record, db.inventoryDao().recordById(record.id))
        var error: String? = null
        field.endSession { error = it }
        compose.waitUntil(15_000) { active() == null }
        assertNull(error)
        val ended = db.inventoryDao().sessionById(initial.sessionId)!!
        assertEquals(saved.copy(endTime = ended.endTime), ended)
        assertNotNull(ended.endTime)
        val afterEnd = db.inventoryDao().recordById(record.id)!!
        // Existing closeSessions marks attached records pending for the closing-session sync.
        assertTrue(afterEnd.updatedAt > record.updatedAt)
        assertEquals(record.copy(updatedAt = afterEnd.updatedAt, serverSyncStatus = "PENDING", serverSyncError = null), afterEnd)
    }
    @Test @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    fun rotationRetainsManualNameRouteRoadbedAndDirection() {
        content()
        project().performTextReplacement("Operación escrita antes de girar")
        compose.onNodeWithText("Sin ruta").performScrollTo().performClick()
        compose.onNodeWithText("PE-28H").performClick()
        compose.onNodeWithText("Seleccionar").performScrollTo().performClick()
        compose.onNodeWithText("A2").performClick()
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.activityRule.scenario.recreate()
        content()
        project().assertTextContains("Operación escrita antes de girar")
        compose.onNodeWithText("PE-28H").performScrollTo().assertExists()
        compose.onNodeWithText("A2").performScrollTo().assertExists()
        compose.onNode(isToggleable()).assertIsOn()
        assertNull(active())
    }
    @Test fun blankEditedProjectIsRejectedWithoutCreatingSession() {
        content()
        project().performTextClearance()
        compose.onNodeWithText("Iniciar").performClick()
        compose.onNodeWithText("Completa proyecto y operador.").performScrollTo().assertIsDisplayed()
        assertNull(active()); assertEquals(0, closed.get())
    }
    @Test @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    fun largeTextKeepsProjectEditableAndConfirmAndCancelAccessible() {
        content(font = 2f)
        project().performScrollTo().performTextReplacement("Proyecto legible")
        project().assertTextContains("Proyecto legible")
        compose.onNodeWithText("Iniciar").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").assertIsDisplayed()
    }
}
