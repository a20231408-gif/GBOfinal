package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.pavement.ui.PavementDrawing
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementObservations10ITest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun sample()=PavementSampleEntity("sample10i","TRAMO FICTICIO","TEST10I",1,"Derecho","UC","00+000","00+010",6.0,10.0,"RIGID","2026-10-13","test10i","Inspector de prueba",slabRows=2,slabColumns=2)
    private fun damage(id:String="damage10i",code:Int=29)=PavementDamageEntity(id,"sample10i",1,"RIGID",code,"Prueba ficticia",if(code==32)null else "L","SLABS",.5,1.0,1.5,2.0,null,null,null,null,null,null,"TEST_AUTO_10I",slabs="1")
    @Test fun partialSlabPreservesContractualQuantityAndDrawsOnlyItsBounds() {
        val s=sample();val d=damage();PavementRules.validate(s,d,emptyList());assertEquals(1.0,PavementRules.quantity(d),0.0)
        fun render(items:List<PavementDamageEntity>)=Bitmap.createBitmap(300,500,Bitmap.Config.ARGB_8888).also {PavementDrawing.draw(Canvas(it),s,items,50f,labels=false)}
        val base=render(emptyList());val marked=render(listOf(d));var changed=0
        for(y in 0 until 500)for(x in 0 until 300)if(base.getPixel(x,y)!=marked.getPixel(x,y)) {
            changed++;assertTrue("Drawing escaped selected zone at $x,$y",x in 24..76 && y in 399..451)
        }
        assertTrue(changed>0);base.recycle();marked.recycle()
    }
    @Test fun twoPositionsAndTwoDamageTypesRemainIndependentWithoutDoubleCountingSameCodeSlab() {
        val a=damage();val b=damage("second").copy(x1=1.8,x2=2.5,y1=3.0,y2=4.0,severity="H")
        val line=damage("line",28).copy(x1=.4,y1=2.0,x2=2.5,y2=2.3,severity="M")
        for(d in listOf(a,b,line))PavementRules.validate(sample(),d,listOf(a,b,line))
        assertEquals(3,listOf(a,b,line).map {it.id}.distinct().size)
        assertEquals(2,PavementSlabTotals.records(listOf(a,b,line)).size)
        assertEquals("H",PavementSlabTotals.records(listOf(a,b,line)).first{it.code==29}.severity)
        assertEquals(SlabMark.LINE,PavementSlabGeometry.mark(28));assertEquals(SlabMark.POINT,PavementSlabGeometry.mark(32))
        PavementRules.validate(sample(),damage("point",32).copy(x2=null,y2=null),emptyList())
    }
    @Test fun editingLocationPersistsOfflineOnReopenAndLeavesHistoricalRowUntouched()=runBlocking {
        val name="10i-${UUID.randomUUID()}";fun open()=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        val session=AuthSessionManager().apply {login(UserCacheEntity("test10i","test10i","Inspector de prueba","OPERATOR",true,"",0,0,0))}
        var db=open()
        try {
            db.pavementDao().saveSample(sample());val repo=PavementRepository(db,session)
            val old=damage("historical").copy(x1=null,y1=null,x2=null,y2=null,crackWidthMm=8.0)
            db.pavementDao().saveDamage(old);repo.saveDamage(damage())
            val saved=db.pavementDao().damage("damage10i")!!
            repo.saveDamage(saved.copy(x1=1.0,y1=2.0,x2=2.0,y2=3.0,severity="M"))
            db.close();db=open();val actual=db.pavementDao().damage(saved.id)!!
            assertEquals(1.0,actual.x1!!,0.0);assertEquals(saved.correlativo,actual.correlativo)
            assertEquals("M",actual.severity);assertEquals(old,db.pavementDao().damage(old.id))
            assertFalse(PavementSlabGeometry.localized(old));assertTrue(db.automaticSyncDao().all().isEmpty())
        }finally {db.close();context.deleteDatabase(name)}
    }
    @Test fun invalidOrUnselectedSlabGeometryCannotBeSaved() {
        for(d in listOf(damage().copy(x2=4.0),damage().copy(y2=1.0),damage().copy(x1=Double.NaN),damage().copy(y1=null)))
            assertTrue(runCatching {PavementRules.validate(sample(),d,emptyList())}.isFailure)
        PavementRules.validate(sample(),damage().copy(x1=null,y1=null,x2=null,y2=null),emptyList())
    }
    @Test fun exportedWorkbookAndPdfUseSavedCoordinatesAndLocalizedRenderer() {
        val s=sample();val d=damage();val result=PavementResult(emptyList(),emptyList(),null,null,listOf("Prueba de geometría"))
        val data=PavementExportSample(s,listOf(d),result);val excel=ByteArrayOutputStream();PavementExcel.write(excel,listOf(data))
        val entries=mutableMapOf<String,String>();ZipInputStream(excel.toByteArray().inputStream()).use {z->while(true){val e=z.nextEntry?:break;entries[e.name]=z.readBytes().toString(Charsets.UTF_8)}}
        assertTrue(entries["xl/worksheets/sheet1.xml"]!!.contains("0.5"));assertTrue(entries["xl/worksheets/sheet3.xml"]!!.contains("1.5"))
        // Robolectric does not implement PdfDocument's native file handle here.
        // Exercise the exact production PDF page renderer on real native canvases.
        val pages=mutableListOf<Bitmap>()
        PavementPdf.render(data,emptyMap(),{Bitmap.createBitmap(595,842,Bitmap.Config.ARGB_8888).also {pages+=it}.let(::Canvas)},{})
        assertTrue(pages.isNotEmpty())
        val out=File("build/campo-10i").apply {mkdirs()}
        File(out,"croquis-localizado.png").outputStream().use {assertTrue(pages.first().compress(Bitmap.CompressFormat.PNG,100,it))}
        pages.forEach {it.recycle()}
    }
    @Test fun crackFieldsMatchEveryRealCatalogEntryAndRuttingPreservesHistoricalUnusedValue() {
        val root=JSONObject(context.assets.open("pavement/catalogo_danos.json").bufferedReader().readText())
        fun visit(o:Any) {when(o){is JSONObject->if(o.has("ancho_fisura_aplicable"))assertEquals(o.getInt("codigo").toString(),o.getBoolean("ancho_fisura_aplicable"),PavementDamageFields.crackWidth(o.getInt("codigo"))) else o.keys().forEach{visit(o.get(it))};is org.json.JSONArray->for(i in 0 until o.length())visit(o.get(i))}}
        visit(root);assertFalse(PavementDamageFields.crackWidth(15));assertTrue(PavementDamageFields.depth(15))
        val s=sample().copy(surface="ASPHALT");val d=damage().copy(surface="ASPHALT",code=15,geometry="AREA",areaM2=2.0,slabs="",crackWidthMm=-1.0,depthMm=14.0)
        PavementRules.validate(s,d,emptyList());assertEquals(-1.0,d.crackWidthMm!!,0.0);assertEquals(2.0,PavementRules.quantity(d),0.0)
    }
}
