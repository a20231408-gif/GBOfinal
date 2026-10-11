package com.tuempresa.inventariovial
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.validation.*
import com.tuempresa.inventariovial.camera.loadCorrectlyOrientedBitmap
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.*
import java.time.LocalDate
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class TabletUxDataTest {
 private fun sample()=PavementSampleEntity("s","Ruta A","Tramo 1",1,"D","UC","0001","0002",4.0,50.0,"ASPHALT","2026-10-06","u","Inspector")
 private fun damage()=PavementDamageEntity("real-uuid","s",3,"ASPHALT",7,"Grieta de borde","B","LINEAR",2.34958372918374,7.123456789,2.0,10.0,null,null,3.0,null,null,null,"")
 @Test fun systemDatePersistsAndHistoricalResumeKeepsAssociationsAndCalculation()=runBlocking {
  val context=ApplicationProvider.getApplicationContext<Context>();val name="tablet-history-${System.nanoTime()}"
  fun open()=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
  var db=open();val sessions=AuthSessionManager();sessions.login(UserCacheEntity("u","u","Inspector","OPERATOR",true,"",0,0,0))
  try {
   val repo=PavementRepository(db,sessions);val today=LocalDate.now().toString();val id=repo.newSample(PavementSurface.ASPHALT)
   assertEquals(today,db.pavementDao().sample(id)!!.fecha)
   val persistedDamage=damage();db.pavementDao().saveSample(sample());db.pavementDao().saveDamage(persistedDamage);db.pavementDao().savePhoto(PavementDamagePhotoEntity("p","real-uuid","/original.jpg"))
   val before=PavementCalculator(context).calculate(sample(),listOf(damage()));db.close();db=open()
   val resumed=PavementRepository(db,sessions);val historic=db.pavementDao().sample("s")!!
   assertEquals("06/10/2026",PavementHistory.dateLabel(historic.fecha));assertTrue(PavementHistory.canContinue(historic,"u"))
   assertEquals("s",PavementHistory.visible(resumed.samples().first(),PavementSurface.ASPHALT,"Tramo 1","").single().id)
   assertEquals(persistedDamage,resumed.damages("s").first().single());assertEquals("p",resumed.photos("real-uuid").single().id)
   resumed.saveSample(historic.copy(fecha=LocalDate.now().plusDays(1).toString()));assertEquals(historic.fecha,db.pavementDao().sample("s")!!.fecha)
   assertEquals(before,PavementCalculator(context).calculate(db.pavementDao().sample("s")!!,resumed.damages("s").first()))
   assertEquals(today,db.pavementDao().sample(id)!!.fecha)
  } finally {db.close();context.deleteDatabase(name)}
 }
 @Test fun historyOrdersCreationAndRespectsFiltersAndClosedRights() {
  val old=sample();val recent=old.copy(id="new",fecha="2026-10-07",updatedAt=0)
  assertEquals(listOf("new","s"),PavementHistory.visible(listOf(old.copy(updatedAt=99999),recent),PavementSurface.ASPHALT,"ruta a","u").map{it.id})
  assertFalse(PavementHistory.canContinue(old.copy(status="CLOSED"),"u"));assertFalse(PavementHistory.canContinue(old,"another"))
  assertTrue(PavementHistory.visible(listOf(old),PavementSurface.RIGID,"","").isEmpty())
 }
 @Test fun rulerUsesOneTransformAtEveryZoomPanAndAdaptsToDensity() {
  for(zoom in listOf(1f,3f,8f)) for(pan in listOf(0f,-125f,400f)) {
   val view=PavementViewport.fit(4.0,50.0,800f,500f,64f,zoom,80f,pan)
   for(y in listOf(0.0,5.0,7.0,10.0,50.0)) {
    assertEquals(y,view.metresY(view.canvasY(y)),0.00001)
    assertEquals(view.canvasY(y),view.originY+PavementScale(50.0,view.scale).canvasY(y),0f)
   }
   assertEquals(2.35,view.metresX(view.originX+(2.35*view.scale).toFloat()),0.00001)
   assertTrue(view.canvasY(0.0)>view.canvasY(10.0))
  }
  assertEquals(5,PavementRuler.labelStep(50.0,500f,30f));assertEquals(2,PavementRuler.labelStep(50.0,1000f,30f));assertEquals(1,PavementRuler.labelStep(50.0,2000f,30f))
  assertTrue(7.0 in PavementRuler.ticks(50.0))
 }
 @Test fun formattingPreservesPrecisionQuantityAndCalculator() {
  val d=damage();val context=ApplicationProvider.getApplicationContext<Context>();val before=PavementCalculator(context).calculate(sample(),listOf(d))
  assertEquals("2.35",PavementDisplay.number(d.x1));assertEquals("7.12",PavementDisplay.number(d.y1));assertEquals("3.00",PavementDisplay.number(PavementRules.quantity(d)))
  assertEquals(2.34958372918374,d.x1!!,0.0);assertEquals(3.0,PavementRules.quantity(d),0.0);assertEquals(before,PavementCalculator(context).calculate(sample(),listOf(d)))
 }
 @Test fun verticalIgnoresLegacyEndValidationWithoutChangingHorizontalPolicy() {
  val r=InventorySaveRequest("SIC-22","VERTICAL","PE-3N","CD","12","350",null,null,null,-12.0,-77.0,1f,"07/10/2026","","",SicFormDetail.Sic22(Sic22FormState()),photoPaths=emptyList(),segment="Tramo 1")
  assertFalse(requiresEndLocation(r));assertFalse(requiresEndLocation("SIC-22","MURO","13"));assertTrue(requiresEndLocation("SIC-21"))
  val legacy=r.copy(endPrCode="invalid",endDistanceM="-99",endLatitude=90.0,endLongitude=null)
  assertEquals(CaptureValidation.errors(r),CaptureValidation.errors(legacy));assertEquals(CaptureValidation.warnings(r,com.tuempresa.inventariovial.road.RoadReferenceData(),0L),CaptureValidation.warnings(legacy,com.tuempresa.inventariovial.road.RoadReferenceData(),0L))
  assertNull(pointCapture(legacy).endPrCode);assertEquals("invalid",legacy.endPrCode)
 }
 @Test fun sic22WorkbookHasExactlyTwelveColumnsOneLocationAndNoLegacyEnd() {
  val r=testRecord().copy(sicCode="SIC-22",endPrCode="invalid-legacy",endDistanceM=-99.0)
  val item=InventoryRecordForExport(r,sic22=Sic22Entity(r.id,"20","2","2","P-1A",null,"2",null,null,null))
  assertEquals(listOf("Carretera","Calzada","Código PR","Distancia (m)","Lado","Clase","Tipo","Material","Código de señal","Número del poste kilométrico","Condición","Fecha"),SicExportFormat.SIC22.columns.map{it.label})
  assertEquals(12,SicExportFormat.SIC22.values(item).size)
  val bytes=ByteArrayOutputStream().also{SicExcelWriter.write(it,listOf(item),SicExportFormat.SIC22,ExportHeading())}.toByteArray()
  val xml=ZipInputStream(ByteArrayInputStream(bytes)).use { zip -> var found="";while(true) {val entry=zip.nextEntry?:break;if(entry.name=="xl/worksheets/sheet1.xml") found=zip.readBytes().toString(Charsets.UTF_8)};found }
  assertFalse(xml.contains("Ubicación fin"));assertFalse(xml.contains("invalid-legacy"));assertTrue(xml.contains("Ubicación del elemento"));assertFalse(xml.contains("E12:F12"))
  val doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray()))
  val rows=doc.getElementsByTagName("row");for(i in 0 until rows.length) {val row=rows.item(i) as org.w3c.dom.Element;if(row.getAttribute("r") in listOf("13","14")) assertEquals(12,row.getElementsByTagName("c").length)}
  assertEquals(2,SicExportFormat.SIC21.columns.count{it.label=="Código PR"})
 }
 @Test fun singleLocationSic22AndLegacyEndFieldsPhotosSurviveRoomReopen()=runBlocking {
  val context=ApplicationProvider.getApplicationContext<Context>();val name="sic22-point-${System.nanoTime()}"
  fun open()=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
  var db=open()
  try {
   for(id in listOf("point","legacy")) {
    val r=testRecord(id,sic="SIC-22").copy(endPrCode=if(id=="point") null else "not-a-pr",endDistanceM=if(id=="point") null else -99.0)
    val photos=(1..2).map{n->PhotoEntity("$id-$n",id,n,"/external-$n.jpg",n==1,null,null,null,"PENDING",1L)}
    com.tuempresa.inventariovial.repository.InventoryRepository(db).saveSic22(r,Sic22Entity(id,"20","2","2","P-1A",null,"2",null,null,null),photos)
   }
   db.close();db=open()
   assertNull(db.inventoryDao().snapshot("point")!!.record.endPrCode)
   val legacy=db.inventoryDao().snapshot("legacy")!!;assertEquals("not-a-pr",legacy.record.endPrCode);assertEquals(2,legacy.photos.size)
   com.tuempresa.inventariovial.repository.EngineeringEdits.save(db,"legacy",SicFormDetail.Sic22(Sic22FormState()),legacy.photos.map{it.localPath},emptyMap())
   assertEquals("not-a-pr",db.inventoryDao().snapshot("legacy")!!.record.endPrCode)
  } finally {db.close();context.deleteDatabase(name)}
 }
 @GraphicsMode(GraphicsMode.Mode.NATIVE) @Test fun thumbnailHonorsExifBoundsWithoutRewritingOriginal() {
  val context=ApplicationProvider.getApplicationContext<Context>();val file=File(context.cacheDir,"exif-${System.nanoTime()}.jpg")
  try {
   val image=android.graphics.Bitmap.createBitmap(640,320,android.graphics.Bitmap.Config.ARGB_8888);file.outputStream().use{image.compress(android.graphics.Bitmap.CompressFormat.JPEG,90,it)};image.recycle()
   android.media.ExifInterface(file.path).apply{setAttribute(android.media.ExifInterface.TAG_ORIENTATION,"6");saveAttributes()}
   val before=file.readBytes();val thumb=loadCorrectlyOrientedBitmap(file.path,160)!!;assertEquals(80,thumb.width);assertEquals(160,thumb.height);thumb.recycle();assertArrayEquals(before,file.readBytes())
  } finally {file.delete()}
 }
}
