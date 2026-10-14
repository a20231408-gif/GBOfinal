package com.tuempresa.inventariovial

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.dashboard.*
import com.tuempresa.inventariovial.data.entity.FieldSession
import com.tuempresa.inventariovial.pavement.ui.PavementWorkspace
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class Navigation10KTest {
    @get:Rule val compose=createComposeRule()
    @Test fun map() {
        var opened=0
        compose.setContent{MaterialTheme{DashboardScreen(null,emptyList(),emptyList(),emptyList(),DashboardDeliveryCounts(0,0,0,0),false,LocalDate.of(2026,10,14),{},{},{},{},{},{},{},{},onMap={opened++})}}
        compose.onNodeWithTag("MAPA").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1,opened)
    }
    @Test fun pavement()=runBlocking {
        val app=ApplicationProvider.getApplicationContext<Application>();val graph=AuthGraph.get(app)
        graph.database.syncContext=null
        graph.sessions.login(UserCacheEntity("test10k","test10k","Ficticio","OPERATOR",true,"",1,1,1))
        val dao=graph.database.inventoryDao();dao.endSessions(1)
        dao.insertSession(FieldSession("test10k-session","TEST","Ficticio","test10k","PE-3N",null,"UC","INCREASING",2))
        try {
            compose.setContent{MaterialTheme{PavementWorkspace{}}}
            compose.waitUntil(10000){compose.onAllNodes(hasSetTextAction() and hasText("PE-3N")).fetchSemanticsNodes().isNotEmpty()}
            val filter=compose.onNode(hasSetTextAction() and hasText("Filtrar tramo / ruta"))
            filter.performTextReplacement("PE-22A");filter.assertTextContains("PE-22A")
            filter.performTextClearance();filter.assertTextContains("")
            assertEquals("PE-3N",dao.currentSession()!!.road)
        }finally{graph.sessions.logout();dao.endSessions(3)}
    }
}
