package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.compose.runtime.*
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
    @Test fun threeJointsAreCreatedThroughRealComposeAndRoomAndRemainIndependent() {
        initializeIdleUiWorkers()
        val context=ApplicationProvider.getApplicationContext<Context>()
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        try {
            val s=visualSample();runBlocking {db.pavementDao().saveSample(s)}
            val sessions=AuthSessionManager();sessions.login(UserCacheEntity("test","test","FICTICIO","OPERATOR",true,"",0,0,0))
            val repo=PavementRepository(db,sessions)
            compose.setContent {val saved by repo.observeSample(s.id).collectAsState(initial=s);InventarioVialTheme {PavementEditor(saved?:s,repo,"test",onBack={})}}
            compose.onNodeWithText("Daño: 21 · Rotura por pandeo").performScrollTo().performClick()
            compose.onNodeWithText("26 · Daño del sello de junta").performScrollTo().performClick()
            val points=listOf(1.0 to 5.0,1.0 to 10.0,3.5 to 2.5)
            for(n in 0..2){
                if(n>0){
                    compose.onNodeWithText("Agregar otro daño de junta").performScrollTo().performClick()
                    compose.onNodeWithText("Severidad: ${PavementSeverity.label(listOf("L","M","H")[n-1])}").performScrollTo().performClick()
                    compose.onNodeWithText(PavementSeverity.label(listOf("L","M","H")[n]),substring=false).performClick()
                }
                val canvas=compose.onNodeWithTag("pavement-scheme");val size=canvas.fetchSemanticsNode().size
                val v=PavementViewport.fit(s.anchoM,s.largoM,size.width.toFloat(),size.height.toFloat(),64f,1f,0f,0f)
                canvas.performTouchInput{click(Offset(v.originX+(points[n].first*v.scale).toFloat(),v.canvasY(points[n].second)))}
                compose.onNodeWithText("Confirmar juntas y severidad").performScrollTo().performClick()
                compose.onNodeWithText("Aceptar",substring=false).performClick()
                compose.waitUntil(15000){runBlocking{db.pavementDao().damages(s.id).size==n+1}}
            }
            runBlocking {
                val all=db.pavementDao().damages(s.id);assertEquals(3,all.size);assertEquals(3,all.map{it.id}.distinct().size)
                assertEquals(listOf("L","M","H"),all.map{it.severity})
                val saved=db.pavementDao().sample(s.id)!!
                assertEquals(3,all.map{PavementJointAnnotations.marks(saved,it.id)}.distinct().size)
                assertTrue(all.all{PavementJointOccurrences.individual(saved,it.id)})
            }
            compose.onNodeWithText("Calificación global PCI: Pendiente de evaluación global").performScrollTo().assertExists()
        } finally {db.close()}
    }

}
