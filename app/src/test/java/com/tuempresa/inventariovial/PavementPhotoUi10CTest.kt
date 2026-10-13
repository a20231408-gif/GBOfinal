package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.pavement.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class PavementPhotoUi10CTest {
    @get:Rule val compose=createComposeRule()
    @Before fun initializeSyntheticWorkManager() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        try {androidx.work.WorkManager.getInstance(context)}
        catch(_:IllegalStateException){androidx.work.WorkManager.initialize(context,androidx.work.Configuration.Builder().build())}
    }
    private fun sample()=PavementSampleEntity("s","T","R10",1,"Derecho","UC","00+000","00+040",5.0,40.0,"ASPHALT","2026-10-12","u","Inspector")
    private fun damage(id:String)=PavementDamageEntity(id,"s",if(id=="first")1 else 2,"ASPHALT",7,"Grieta","L","LINEAR",1.0,2.0,2.0,15.0,null,1.0,13.0,null,null,null,"")
    @Test fun eachDamageOffersCameraAndGalleryForItsOwnUuid() {
        val camera=mutableListOf<String>();val viewed=mutableListOf<String>()
        compose.setContent {MaterialTheme {PavementDamageTable(listOf(damage("first"),damage("second")),null,{},mapOf("first" to 2,"second" to 1),{camera+=it},{viewed+=it})}}
        compose.onNodeWithTag("damage-camera-first").performClick()
        compose.onNodeWithTag("damage-photos-second").performScrollTo().performClick()
        compose.onNodeWithText("Ver fotos (2)").assertExists()
        assertEquals(listOf("first"),camera);assertEquals(listOf("second"),viewed)
        compose.onAllNodes(hasText("VIDEO",substring=true)).assertCountEquals(0)
    }
    @Test fun closedSampleKeepsViewPhotosAndHasNoCaptureButtons() {
        compose.setContent {MaterialTheme {PavementDamageTable(listOf(damage("first")),null,{},mapOf("first" to 3),onViewPhotos={})}}
        compose.onNodeWithText("Ver fotos (3)").assertExists();compose.onAllNodes(hasText("Tomar foto")).assertCountEquals(0)
    }
    @Test fun generalPanelObservesTheSamePhotoRowsWithoutManualRefresh() {
        val context=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        val sessions=AuthSessionManager().apply {login(UserCacheEntity("u","u","Inspector","OPERATOR",true,"",0,0,0))}
        val repo=PavementRepository(db,sessions)
        val showing=androidx.compose.runtime.mutableStateOf(true)
        try {
            runBlocking {db.pavementDao().saveSample(sample());db.pavementDao().saveDamage(damage("first"));db.pavementDao().saveDamage(damage("second"))}
            compose.setContent {if(showing.value) MaterialTheme {Box(Modifier.fillMaxSize()) {PavementEditor(sample(),repo,"u",initialPage="PHOTOS",onBack={})}}}
            compose.onNodeWithText("Cerrar fotografías").assertExists()
            runBlocking {repo.savePhoto(PavementDamagePhotoEntity("one","first","/synthetic/one.jpg"),"s");repo.savePhoto(PavementDamagePhotoEntity("two","first","/synthetic/two.jpg"),"s");repo.savePhoto(PavementDamagePhotoEntity("three","second","/synthetic/three.jpg"),"s")}
            compose.waitUntil(10000) {compose.onAllNodes(hasText("1. Grieta · 2 fotos")).fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithText("2. Grieta · 1 fotos").assertExists()
            compose.onNodeWithText("Cerrar fotografías").performClick()
            compose.onNodeWithTag("damage-photos-second").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) {assertTrue(it())}
            compose.onNodeWithText("Muestra 1 · daño 2 · Grieta").assertExists()
            assertTrue(compose.onAllNodesWithText("Fotografías del daño · 1").fetchSemanticsNodes().isNotEmpty())
        }finally {compose.runOnIdle {showing.value=false};compose.waitForIdle();db.close()}
    }
}
