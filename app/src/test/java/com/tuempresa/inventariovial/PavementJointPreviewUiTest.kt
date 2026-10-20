package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.pavement.ui.PavementEditor
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,qualifiers="w1280dp-h800dp-land")
class PavementJointPreviewUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun selecting26StartsPreviewDirectlyAndUndoCancelNeverCreateARecord() {
        initializeIdleUiWorkers()
        val context=ApplicationProvider.getApplicationContext<Context>()
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        try {
            val s=visualSample();runBlocking {db.pavementDao().saveSample(s)}
            val sessions=AuthSessionManager();sessions.login(UserCacheEntity("test","test","FICTICIO","OPERATOR",true,"",0,0,0))
            val repo=PavementRepository(db,sessions)
            compose.setContent {InventarioVialTheme {PavementEditor(s,repo,"test",onBack={})}}
            compose.onNodeWithText("Daño: 21 · Rotura por pandeo").performScrollTo().performClick()
            compose.onNodeWithText("26 · Daño del sello de junta").performScrollTo().performClick()
            compose.onNodeWithText("Confirmar juntas y severidad").performScrollTo().assertIsEnabled()
            val canvas=compose.onNodeWithTag("pavement-scheme");val size=canvas.fetchSemanticsNode().size
            val v=PavementViewport.fit(s.anchoM,s.largoM,size.width.toFloat(),size.height.toFloat(),64f,1f,0f,0f)
            canvas.performTouchInput {click(Offset(v.originX+(3.5*v.scale).toFloat(),v.canvasY(2.5)))}
            compose.onNodeWithText("Juntas/cruces seleccionados: 1").assertExists()
            compose.onNodeWithText("Deshacer última marca").performScrollTo().performClick()
            compose.onNodeWithText("Juntas/cruces seleccionados: 0").assertExists()
            compose.onNodeWithText("Cancelar selección").performScrollTo().performClick()
            compose.onNodeWithText("Editar juntas").assertExists()
            runBlocking {assertTrue(db.pavementDao().damages(s.id).isEmpty());assertNull(db.pavementDao().sample(s.id)!!.sicDataJson)}
        }finally {db.close()}
    }
}
