package com.tuempresa.inventariovial

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.ScapBridgeType
import com.tuempresa.inventariovial.scap.export.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScapCanonicalExportTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun snapshot(type:ScapBridgeType):ScapInspectionSnapshot {
        val inspection=ScapInspectionEntity("12345678-1234-1234-1234-123456789010","road","Puente prueba ${type.label}","P-NEW",0,0,"Inspector","device","DRAFT","PENDING",-12.0,-77.0,null,null,"MANUAL")
        val values=mapOf("bridgeType" to type.name,"route" to "PE-28H","progressive" to "42+125.50",
            "totalLengthM" to "18.4","lastInspection" to "07/10/2026","latitude" to "-12.75","longitude" to "-77.5")
        val element=ScapElementEntity("manual",inspection.id,"X-01","Elemento manual campo",12.75,"ml",0.8,"ACCESOS",true)
        return ScapInspectionSnapshot(inspection,
            values.map {ScapFieldValueEntity(inspection.id,"inspection",it.key,it.value,"MANUAL")}+
                ScapFieldValueEntity(inspection.id,"manual","elementSource","MANUAL","MANUAL"),
            elements=listOf(ScapElementWithCondition(element,ScapElementConditionEntity("condition","manual",0.0,80.0,20.0,0.0,0.0,0.0))))
    }
    private fun export(s:ScapInspectionSnapshot,read:(String)->ByteArray={error("No media expected")}):TemplateWorkbook {
        val out=ByteArrayOutputStream();ScapDelivery.exporter(context,s).write(out,s,read)
        val dir=File("../tmp/parallel-b").apply {mkdirs()}
        File(dir,"SCAP_${ScapBridgeType.from(s)?.label}.xlsx").writeBytes(out.toByteArray())
        return TemplateWorkbook(out.toByteArray().inputStream())
    }
    @Test fun bothTypesUseExactSourceAndKeepCorporateHeaderSheetNamesStylesGeometryAndDerivedFormulas() {
        // Pin the existing repository templates, unchanged by Prompt 9B.
        val hashes=mapOf(ScapBridgeType.SLAB to "64feb2e7d34ce3504befee74c9f09ebf845c432cc05cf3fab70ecff7410944c3",
            ScapBridgeType.MODULAR to "ae6962149498506c8bf71f5b51d6f77231691161c1372132b82caefaf0058091")
        for(type in ScapBridgeType.entries) {
            val bytes=context.assets.open(type.templateAsset).use {it.readBytes()}
            assertEquals(hashes.getValue(type),ScapDelivery.sha256(bytes))
            val source=TemplateWorkbook(bytes.inputStream());val generated=export(snapshot(type))
            assertArrayEquals(source.parts.getValue("xl/styles.xml"),generated.parts.getValue("xl/styles.xml"))
            assertEquals(source.document("xl/workbook.xml").nodes("sheet").map{it.getAttribute("name")},generated.document("xl/workbook.xml").nodes("sheet").map{it.getAttribute("name")})
            for(sheet in 1..9) {
                for(node in listOf("mergeCell","col","row")) {
                    val attrs=when(node){"mergeCell"->listOf("ref");"col"->listOf("min","max","width","hidden");else->listOf("r","ht","hidden","customHeight")}
                    fun geometry(w:TemplateWorkbook)=w.sheet(sheet).nodes(node).map {e->attrs.map(e::getAttribute)}
                    assertEquals("$type sheet $sheet $node",geometry(source),geometry(generated))
                }
                source.sheet(sheet).nodes("c").filter{it.children().any {n->n.localName=="f"}}.forEach {cell->
                    val ref=cell.getAttribute("r")
                    val input=sheet==5 && ref.takeWhile(Char::isLetter) in setOf("A","B","D","E","F","G","H","I","J","K","L") && ref.filter(Char::isDigit).toInt() in 14..44
                    if(!input) assertEquals("$type $sheet!$ref",source.formula(sheet,ref),generated.formula(sheet,ref))
                }
            }
            assertEquals("Puente prueba ${type.label}",generated.cell(1,"J10").textContent)
            assertEquals("-12.75",generated.cell(1,"E16").textContent)
            assertEquals("-77.5",generated.cell(1,"E18").textContent)
            assertEquals("+E32",generated.formula(1,"E67"))
            assertEquals(source.cell(1,"F1").getAttribute("s"),generated.cell(1,"F1").getAttribute("s"))
            // The untouched first-sheet drawing contains both corporate logos.
            val originalDrawing=source.parts.keys.filter{it.startsWith("xl/drawings/drawing") && it.endsWith(".xml")}.first()
            assertArrayEquals(source.parts.getValue(originalDrawing),generated.parts.getValue(originalDrawing))
        }
    }
    @Test fun bothExportsKeepManualCodeMetradoContractualUnitAndActualImportanceValue() {
        for(type in ScapBridgeType.entries) {
            val w=export(snapshot(type))
            assertEquals("X-01",w.cell(5,"A14").textContent)
            assertEquals("Elemento manual campo",w.cell(5,"B14").textContent)
            assertEquals("12.75",w.cell(5,"D14").textContent)
            assertEquals("ml",w.cell(5,"E14").textContent)
            assertEquals("0.8",w.cell(5,"F14").textContent)
            assertEquals("80.0",w.cell(5,"H14").textContent)
            assertEquals("",w.cell(5,"A15").textContent)
            assertEquals("X-01",w.cell(3,if(type==ScapBridgeType.SLAB)"B41" else "B34").textContent)
        }
    }
    @Test fun canonicalBoundsRejectOverflowAndMismatchedTypeBeforeWriting() {
        val s=snapshot(ScapBridgeType.MODULAR)
        val overflow=s.copy(elements=(1..3).map {i->s.elements.first().let{it.copy(element=it.element.copy(id="e$i",elementCode="X$i",group="DETALLES"))}})
        assertTrue(ScapDelivery.exporter(context,overflow).review(overflow).errors.isEmpty())
        val extended=export(overflow)
        assertTrue(extended.sheet(3).nodes("c").any{it.textContent=="X3"})
        val tooMany=overflow.copy(elements=(1..32).map{i->overflow.elements.first().let{it.copy(element=it.element.copy(id="over$i",elementCode="OVER$i"))}})
        assertTrue(ScapDelivery.exporter(context,tooMany).review(tooMany).errors.any{"31" in it})
        val mismatched=ScapExcelExporter({context.assets.open(ScapBridgeType.SLAB.templateAsset)},context.assets.open("scap_slab_field_map.json").bufferedReader().use{it.readText()},ScapBridgeType.SLAB)
        assertTrue(mismatched.review(s).errors.any {"tipo" in it})
        val out=ByteArrayOutputStream()
        assertTrue(runCatching{mismatched.write(out,s){error("No media")}}.isFailure)
        assertEquals(0,out.size())
    }
    @Test fun observedNumericSourceLiteralsSurviveAndNumbersRemainTyped() {
        val base=snapshot(ScapBridgeType.MODULAR)
        val literals=mapOf("constructionYear" to "Sin Informacion","traffic" to "SIN DATOS",
            "truckBusPercent" to "SIN DATOS","sidewalkWidthM" to "N/A")
        val coords=mapOf("constructionYear" to "N30","traffic" to "N38","truckBusPercent" to "N42","sidewalkWidthM" to "E40")
        fun withValues(values:Map<String,String>)=base.copy(values=base.values+values.map{ScapFieldValueEntity(base.inspection.id,"inspection",it.key,it.value,"MANUAL")})
        val w=export(withValues(literals))
        literals.forEach {(key,value)->assertEquals(value,w.cell(1,coords.getValue(key)).textContent)}
        val numeric=mapOf("constructionYear" to "2005","traffic" to "81","truckBusPercent" to "9","sidewalkWidthM" to "0.6")
        val typed=export(withValues(numeric))
        numeric.forEach {(key,value)->
            val cell=typed.cell(1,coords.getValue(key))
            assertEquals("",cell.getAttribute("t"));assertEquals(value.toDouble(),cell.textContent.toDouble(),.000001)
        }
        val span=ScapSpanEntity("span",base.inspection.id,1,18.4,"PROVISIONALES","Modular","","","Acero Estructural")
        val repeated=withValues(literals).copy(spans=listOf(span),values=withValues(literals).values+
            ScapFieldValueEntity(base.inspection.id,"span","slabThicknessM","5 pulg","MANUAL")+
            ScapFieldValueEntity(base.inspection.id,"ACCESS_LEFT","shoulderWidthM","N/A","MANUAL"))
        val dimensions=export(repeated)
        assertEquals("5 pulg",dimensions.cell(1,"E101").textContent)
        assertEquals("N/A",dimensions.cell(1,"G275").textContent)
    }
    private fun image(width:Int,height:Int):ByteArray {
        val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        return try{ByteArrayOutputStream().use {bitmap.compress(Bitmap.CompressFormat.JPEG,95,it);it.toByteArray()}}finally{bitmap.recycle()}
    }
    @Test fun portraitLandscapeAndExifRotationStayCenteredInsideOriginalPhotoFrames() {
        for(type in ScapBridgeType.entries) for((width,height) in listOf(600 to 300,300 to 600)) {
            val s=snapshot(type);val p=PhotoEntity("photo","road",1,"synthetic.jpg",false,null,null,null,"PENDING",0,scapInspectionId=s.inspection.id,photoCategory="GENERAL")
            val w=export(s.copy(photos=listOf(p))){image(width,height)}
            val drawing=w.parts.keys.filter{it.startsWith("xl/drawings/drawing") && it.endsWith(".xml")}.map(w::document)
                .flatMap{it.nodes("oneCellAnchor")}.single {a->a.getElementsByTagNameNS("*","cNvPr").let{(it.item(0) as org.w3c.dom.Element).getAttribute("name").startsWith("scap_4_")}}
            val extent=drawing.children().single{it.localName=="ext"}
            val cx=extent.getAttribute("cx").toLong();val cy=extent.getAttribute("cy").toLong()
            assertEquals(width.toDouble()/height,cx.toDouble()/cy,.000001)
            val original=context.assets.open(type.templateAsset).use{TemplateWorkbook(it)}
            val photoDrawing=original.document("xl/worksheets/_rels/sheet4.xml.rels").nodes("Relationship").single{it.getAttribute("Type").endsWith("/drawing")}.getAttribute("Target").substringAfterLast('/')
            val frame=original.document("xl/drawings/$photoDrawing").documentElement.children().filter{a->a.children().firstOrNull{it.localName=="from"}?.children()?.firstOrNull{it.localName=="row"}?.textContent?.toInt()?.let{it>=8}==true}.minBy {a->a.children().first{it.localName=="from"}.children().first{it.localName=="row"}.textContent.toInt()}
            val frameExtent=(frame.getElementsByTagNameNS("*","xfrm").item(0) as org.w3c.dom.Element).children().single{it.localName=="ext"}
            val fw=frameExtent.getAttribute("cx").toLong();val fh=frameExtent.getAttribute("cy").toLong()
            assertTrue(cx<=fw);assertTrue(cy<=fh)
            val from=drawing.children().single{it.localName=="from"};val originalFrom=frame.children().single{it.localName=="from"}
            fun marker(e:org.w3c.dom.Element,k:String)=e.children().single{it.localName==k}.textContent.toLong()
            assertEquals((fw-cx)/2,marker(from,"colOff")-marker(originalFrom,"colOff"))
            assertEquals((fh-cy)/2,marker(from,"rowOff")-marker(originalFrom,"rowOff"))
        }
        val file=File(context.cacheDir,"export-exif.jpg").apply{writeBytes(image(600,300))}
        ExifInterface(file.path).apply{setAttribute(ExifInterface.TAG_ORIENTATION,"6");saveAttributes()}
        val normalized=ScapDelivery.readImage(context,file.path,photo=true)
        val bounds=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true}
        android.graphics.BitmapFactory.decodeByteArray(normalized,0,normalized.size,bounds)
        assertArrayEquals(file.readBytes(),normalized);assertEquals(600,bounds.outWidth);assertEquals(300,bounds.outHeight)
        val s=snapshot(ScapBridgeType.MODULAR)
        val photo=PhotoEntity("photo-landscape","road",1,"landscape.jpg",false,null,null,null,"PENDING",0,scapInspectionId=s.inspection.id,photoCategory="GENERAL")
        val output=ByteArrayOutputStream()
        ScapDelivery.exporter(context,s).write(output,s.copy(photos=listOf(photo,photo.copy(id="photo-portrait",photoIndex=2,localPath="portrait-exif.jpg")))) {path->
            if(path=="landscape.jpg") image(600,300) else file.readBytes()
        }
        File("../tmp/parallel-b/SCAP_PHOTOS_FIT.xlsx").writeBytes(output.toByteArray())
    }
}
