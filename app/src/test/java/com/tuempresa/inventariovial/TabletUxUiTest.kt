package com.tuempresa.inventariovial
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.field.InventoryPhotoGallery
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.pavement.ui.PavementDamageTable
import com.tuempresa.inventariovial.pavement.ui.PavementDamageActions
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class TabletUxUiTest {
 @org.junit.Before fun initializeWorkers() = initializeIdleUiWorkers()
 @get:Rule val compose=createComposeRule()
 @Test fun tableHasOneRowPerDamageAndSelectionKeepsRealId() {
  val damages=(1..5).map{n->PavementDamageEntity("uuid-$n","s",n,"ASPHALT",7,"Grieta de borde","B","LINEAR",2.35,7.1,2.0,10.0,null,null,3.0,null,null,null,"")}
  var selected:String?=null;var modifying:String?=null;var moving:String?=null
  compose.setContent{MaterialTheme{var selection by remember{mutableStateOf<String?>(null)};Column(Modifier.width(600.dp).verticalScroll(rememberScrollState())){PavementDamageTable(damages,selection){selection=it;selected=it};damages.firstOrNull{it.id==selection}?.let{d->PavementDamageActions(d,false,{modifying=it.id},{moving=it.id},{},{},{})}}}}
  for(header in listOf("Ítem","X","Y","Falla","Severidad")) compose.onNodeWithText(header).assertExists()
  compose.onAllNodesWithText("2.35").assertCountEquals(5);compose.onAllNodesWithText("7.10").assertCountEquals(5)
  compose.onNodeWithTag("damage-row-uuid-3").performClick().assertIsSelected()
  compose.onNodeWithText("Modificar").performClick();compose.onNodeWithText("Mover en esquema").performScrollTo().performClick()
  compose.runOnIdle{assertEquals("uuid-3",modifying);assertEquals("uuid-3",moving);assertEquals("uuid-3",selected);assertEquals("Grieta de borde",damages.single{it.id==selected}.name);assertEquals("B",damages.single{it.id==selected}.severity)}
 }
 @Test fun historicCardContinuesExactIdAndClosedCardUsesView() {
  val old=PavementSampleEntity("historic","R","Tramo 1",1,"D","","0","50",4.0,50.0,"ASPHALT","2026-10-06","u","Inspector")
  var opened:String?=null
  compose.setContent{MaterialTheme{Column(Modifier.verticalScroll(rememberScrollState())){
   com.tuempresa.inventariovial.pavement.ui.PavementSampleCard(old,"u"){opened=it}
   com.tuempresa.inventariovial.pavement.ui.PavementSampleCard(old.copy(id="closed",status="CLOSED"),"u"){opened=it}
  }}}
  compose.onAllNodesWithText("06/10/2026 · Inspector").assertCountEquals(2)
  compose.onNodeWithText("Continuar").performClick();compose.runOnIdle{assertEquals("historic",opened)}
  compose.onNodeWithText("Ver").performScrollTo().performClick();compose.runOnIdle{assertEquals("closed",opened)}
 }
 @Test fun verticalFormUsesElementLocationAndDoesNotRenderEndBlock() {
  val app=androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
  try {androidx.work.WorkManager.getInstance(app)} catch(_:IllegalStateException) {
   androidx.work.WorkManager.initialize(app,androidx.work.Configuration.Builder().build())
  }
  val vm=com.tuempresa.inventariovial.viewmodel.InventoryViewModel(app)
  compose.setContent{MaterialTheme{SignalizationFormScreen(com.tuempresa.inventariovial.model.SignalizationType.VERTICAL,vm,{},{},testRecord(sic="SIC-22"))}}
  compose.waitUntil(10000) {
   compose.waitForIdle()
   org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
   compose.onAllNodesWithTag("legacy-inventory-form").fetchSemanticsNodes().isNotEmpty()
  }
  compose.onAllNodes(hasScrollToNodeAction())[0].performScrollToNode(hasText("Ubicación del elemento"))
  compose.onNodeWithText("Ubicación del elemento").assertExists()
  compose.onAllNodes(hasScrollToNodeAction())[0].performScrollToNode(hasText("Lado"))
  compose.onNodeWithText("Ubicación fin").assertDoesNotExist()
  compose.onNodeWithText("Kilómetro de progresiva final (PR)").assertDoesNotExist()
  compose.onNodeWithText("Metros desde el kilómetro final").assertDoesNotExist()
 }
 @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
 @Test fun twoPhotosPreviewCloseRemovalPreserveOtherAndForm() {
  val context=androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
  val files=(1..2).map{n->java.io.File(context.cacheDir,"thumb-$n-${System.nanoTime()}.jpg").also{file->val image=android.graphics.Bitmap.createBitmap(32,16,android.graphics.Bitmap.Config.ARGB_8888);file.outputStream().use{image.compress(android.graphics.Bitmap.CompressFormat.JPEG,90,it)};image.recycle()}}
  var paths by mutableStateOf(files.map{it.path})
  compose.setContent{MaterialTheme{Column(Modifier.verticalScroll(rememberScrollState())){androidx.compose.material3.Text("Observación conservada");InventoryPhotoGallery(paths){p->paths=paths.filterNot{it==p}}}}}
  compose.onNodeWithTag("inventory-photo-0").assertExists();compose.onNodeWithTag("inventory-photo-1").assertExists()
  compose.waitUntil(10000){compose.onAllNodesWithContentDescription("Fotografía del elemento").fetchSemanticsNodes().size==2}
  compose.onAllNodesWithText("Ver")[0].performClick();compose.onNodeWithText("Cerrar").performClick()
  compose.onNodeWithText("Observación conservada").assertExists();compose.onAllNodesWithText("Eliminar")[1].performScrollTo().performClick()
  compose.runOnIdle{assertEquals(listOf(files[0].path),paths);assertTrue(files.all{it.exists()})}
  compose.onNodeWithTag("inventory-photo-0").assertExists();compose.onNodeWithTag("inventory-photo-1").assertDoesNotExist()
  files.forEach{it.delete()}
 }
}
