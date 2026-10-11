package com.tuempresa.inventariovial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.pavement.PavementSampleEntity
import com.tuempresa.inventariovial.pavement.ui.PavementScheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.min

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class   PavementTouchTest {
    @get:Rule val compose=createComposeRule()
    @Test fun homeDisplaysNewModuleAndOpensItWhenTapped() {
        var opened=false
        compose.setContent { androidx.compose.material3.MaterialTheme { HomeScreen(0,0,{},onPavement={opened=true}) } }
        compose.onNodeWithText("Fallas en pavimentos").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(opened) }
    }
    @Test fun movingGestureUpdatesGeometryWithoutCreatingDamage() {
        val s=PavementSampleEntity("s","A","A",1,"D","","0","10",10.0,10.0,"ASPHALT","2026-09-28","u","Inspector")
        val d=com.tuempresa.inventariovial.pavement.PavementDamageEntity("d","s",1,"ASPHALT",1,"Daño","B","AREA",1.0,1.0,2.0,2.0,null,1.0,1.0,1.0,null,null,"")
        var moved:com.tuempresa.inventariovial.pavement.PavementDamageEntity?=null
        compose.setContent { Box(Modifier.width(400.dp).height(400.dp)) { PavementScheme(s,listOf(d),emptySet(),null,true,moving=d,onMove={moved=it}){_,_->error("Must not capture while moving")} } }
        compose.onNodeWithTag("pavement-scheme").performTouchInput { swipe(Offset(width/3f,height/2f),Offset(width*2/3f,height/2f),500) }
        compose.runOnIdle { assertNotNull(moved);assertTrue(moved!!.x1!!>d.x1!!);assertEquals(d.areaM2,moved!!.areaM2) }
    }
    @Test fun eachPhotoHasDeleteAndConfirmation() {
        val context=androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val db=androidx.room.Room.inMemoryDatabaseBuilder(context,com.tuempresa.inventariovial.data.database.InventoryDatabase::class.java).build()
        val sample=PavementSampleEntity("s","A","A",1,"D","","0","40",5.0,40.0,"ASPHALT","2026-09-28","u","Inspector")
        val damage=com.tuempresa.inventariovial.pavement.PavementDamageEntity("d","s",1,"ASPHALT",1,"Daño","B","AREA",0.0,0.0,1.0,1.0,null,1.0,1.0,1.0,null,null,"")
        val sessions=com.tuempresa.inventariovial.auth.AuthSessionManager()
        try {
            kotlinx.coroutines.runBlocking {
                db.pavementDao().saveSample(sample);db.pavementDao().saveDamage(damage)
                for(i in 1..3) db.pavementDao().savePhoto(com.tuempresa.inventariovial.pavement.PavementDamagePhotoEntity("p$i","d","/missing$i.jpg"))
            }
            val repo=com.tuempresa.inventariovial.pavement.PavementRepository(db,sessions)
            compose.setContent { androidx.compose.material3.MaterialTheme {
                Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    com.tuempresa.inventariovial.pavement.ui.PhotoGallery("d",0,repo,true,{}, {error(it)})
                }
            } }
            compose.waitUntil(10000) { compose.onAllNodesWithText("Foto 3").fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodesWithText("Eliminar").assertCountEquals(3)
            compose.onAllNodesWithText("Eliminar")[1].performScrollTo().performClick()
            compose.onNodeWithText("¿Eliminar esta fotografía del daño?").assertIsDisplayed()
            compose.onNodeWithText("Cancelar").performClick()
            compose.onNodeWithText("¿Eliminar esta fotografía del daño?").assertDoesNotExist()
            kotlinx.coroutines.runBlocking { assertEquals(3,repo.photos("d").size) }
        } finally { db.close() }
    }

    @Test fun changingGridKeepsMetricPosition() {
        val sample=PavementSampleEntity("s","A","A",1,"D","","0","40",5.0,40.0,"ASPHALT","2026-09-28","u","Inspector")
        val points=mutableListOf<Pair<Double,Double>>()
        compose.setContent { Box(Modifier.width(400.dp).height(420.dp)) { PavementScheme(sample,emptyList(),emptySet(),null,true){x,y->points+=x to y} } }
        fun tapAtTwenty() { compose.onNodeWithTag("pavement-scheme").performTouchInput {
            val gutter=64*compose.density.density
            val scale=min((width-gutter-15)/5f,(height-30)/40f)
            click(Offset(gutter+scale,15+20*scale))
        } }
        tapAtTwenty()
        compose.onNodeWithText("Malla 1.0 m").performClick()
        compose.onNodeWithText("Malla 0.5 m").assertIsDisplayed()
        tapAtTwenty()
        compose.runOnIdle { assertEquals(2,points.size);assertEquals(points[0],points[1]);assertEquals(20.0,points[1].second,1e-4) }
    }

    @Test fun tapsUseBottomLeftOriginAndEqualAxisScale() {
        val sample=PavementSampleEntity("s","A","A",1,"D","","0","46",5.0,46.0,"ASPHALT","2026-09-28","u","Inspector")
        var point:Pair<Double,Double>?=null
        compose.setContent { Box(Modifier.width(400.dp).height(400.dp)) { PavementScheme(sample,emptyList(),emptySet(),null,true){x,y->point=x to y} } }
        compose.onNodeWithTag("pavement-scheme").performTouchInput {
            val gutter=64*compose.density.density
            val scale=min((width-gutter-15)/5f,(height-30)/46f)
            click(Offset(gutter+1.25f*scale,15+26f*scale))
        }
        compose.runOnIdle { assertNotNull(point);assertEquals(1.25,point!!.first,1e-4);assertEquals(20.0,point!!.second,1e-4) }
    }
}
