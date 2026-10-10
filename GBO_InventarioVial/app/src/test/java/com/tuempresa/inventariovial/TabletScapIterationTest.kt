package com.tuempresa.inventariovial

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.calculator.*
import com.tuempresa.inventariovial.scap.export.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TabletScapIterationTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private val catalog get()=ScapCatalog.load(context)
    @Test fun bothContractsSeedAllUnsetElementsAndTwoSupportsAndRetainManualAndHiddenValuesAfterReopen()=runBlocking {
        for(type in ScapBridgeType.entries) {
            val name="iteration.db"
            context.getDatabasePath(name).parentFile!!.mkdirs()
            var db=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
            try {
                val repo=ScapRepository(db,catalog);val id=repo.create("Inspector","device",null,type)
                val initial=repo.dao.snapshot(id)!!;val expected=if(type==ScapBridgeType.SLAB)17 else 10
                assertEquals(expected,initial.elements.size);assertEquals(expected,ScapElementView.applicable(initial).size)
                assertTrue(initial.elements.all{ScapElementView.state(initial,it.element)==ScapElementState.UNSET&&it.condition==null&&it.element.quantity==null})
                assertEquals(listOf(1,2),initial.supports.map{it.supportIndex}.sorted())
                initial.supports.sortedBy{it.supportIndex}.forEachIndexed{i,b->repo.setField(id,b.id,"location",if(i==0)"LEFT" else "RIGHT")}
                repo.addRow(id,"BEARING")
                val e=initial.elements.first().element
                repo.setField(id,e.id,"importanceFactor","")
                assertNull(ScapElementView.factor(repo.dao.snapshot(id)!!,e))
                repo.setField(id,e.id,"importanceFactor",e.importanceFactor.toString())
                repo.setField(id,e.id,"quantity","20.25");repo.setField(id,e.id,"percent1","90");repo.setField(id,e.id,"percent2","10")
                repo.setField(id,e.id,"customName","Nombre observado");repo.setField(id,e.id,"globalDescription","Descripción real")
                repo.selectElement(id,e.elementCode,false)
                assertEquals(expected-1,ScapElementView.applicable(repo.dao.snapshot(id)!!).size)
                assertTrue(ScapCompletion.assess(repo.dao.snapshot(id)!!,catalog.forType(type)).pending.none{it.ownerId==e.id})
                repo.selectElement(id,e.elementCode,true)
                repo.addManualElement(id,"X-CAMPO","Elemento de campo","","ml","")
                val manual=repo.dao.snapshot(id)!!.elements.single{it.element.elementCode=="X-CAMPO"}.element
                assertEquals(expected+1,ScapElementView.applicable(repo.dao.snapshot(id)!!).size)
                repo.selectElement(id,manual.elementCode,false);assertEquals(expected,ScapElementView.applicable(repo.dao.snapshot(id)!!).size)
                repo.selectElement(id,manual.elementCode,true);repo.saveDraft(id)
                db.close();db=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
                val restored=db.scapDao().snapshot(id)!!;val changed=restored.elements.single{it.element.id==e.id}
                assertEquals(expected+1,ScapElementView.applicable(restored).size)
                assertEquals(20.25,changed.element.quantity!!,0.0)
                assertEquals(listOf(null,90.0,10.0,null,null,null),ScapPercentages.values(changed.condition!!))
                assertEquals("Nombre observado",ScapElementView.name(restored,changed.element));assertEquals("Descripción real",ScapElementView.description(restored,changed.element))
                assertEquals(listOf("LEFT","RIGHT",""),restored.supports.sortedBy{it.supportIndex}.map{it.location})
                assertEquals("INCOMPLETE",restored.values("completion")["status"])
            }finally{db.close();context.deleteDatabase(name)}
        }
    }
    @Test fun incompleteExportsPreserveSourceGeometryAndBlanksAndSupportThreeAndCustomGlobalDescription()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        try {for(type in ScapBridgeType.entries) {
            val repo=ScapRepository(db,catalog);val id=repo.create("Inspector","device",null,type)
            val s0=repo.dao.snapshot(id)!!;val e=s0.elements.sortedWith(compareBy({ScapPresentation.groups.indexOf(it.element.group)},{it.element.elementCode})).first().element
            repo.setField(id,e.id,"percent1","90");repo.setField(id,e.id,"percent2","10")
            repo.setField(id,e.id,"globalDescription","Texto de campo");repo.setField(id,e.id,"customName","Nombre editado")
            repo.setField(id,e.id,"customCode","X-F1");repo.setField(id,e.id,"exportCode","X-G1")
            repo.addManualElement(id,"X-NEW","Elemento nuevo","","ml","");repo.addRow(id,"BEARING")
            val support=repo.dao.snapshot(id)!!.supports.single{it.supportIndex==3};repo.setField(id,support.id,"location","LEFT")
            repo.saveDraft(id);val s=repo.dao.snapshot(id)!!
            assertTrue(ScapCompletion.assess(s,catalog.forType(type)).pending.size>=10)
            assertTrue(ScapDelivery.exporter(context,s).review(s).errors.isEmpty())
            val output=ByteArrayOutputStream();ScapDelivery.exporter(context,s).write(output,s){error("No image expected")}
            val result=TemplateWorkbook(output.toByteArray().inputStream());val source=TemplateWorkbook(context.assets.open(type.templateAsset))
            assertArrayEquals(source.parts.getValue("xl/styles.xml"),result.parts.getValue("xl/styles.xml"))
            assertEquals(source.document("xl/workbook.xml").nodes("sheet").map{it.getAttribute("name")},result.document("xl/workbook.xml").nodes("sheet").map{it.getAttribute("name")})
            val names=result.document("xl/workbook.xml").nodes("definedName")
            source.document("xl/workbook.xml").nodes("definedName").forEach{before->
                val after=names.single{it.getAttribute("name")==before.getAttribute("name")&&it.getAttribute("localSheetId")==before.getAttribute("localSheetId")}
                if(before.getAttribute("name")=="_xlnm.Print_Area")assertTrue(after.textContent.startsWith(before.textContent)) else assertEquals(before.textContent,after.textContent)
            }
            assertEquals("",result.cell(5,"D14").textContent);assertEquals("",result.cell(5,"G14").textContent)
            assertEquals("90.0",result.cell(5,"H14").textContent);assertEquals("10.0",result.cell(5,"I14").textContent)
            for(ref in listOf("J14","K14","L14"))assertEquals("",result.cell(5,ref).textContent)
            assertEquals("Nombre editado",result.cell(3,"C22").textContent);assertEquals("Texto de campo",result.cell(3,"F22").textContent)
            assertEquals("X-F1",result.cell(3,"B22").textContent);assertEquals("X-G1",result.cell(5,"A14").textContent)
            assertTrue(result.sheet(3).nodes("c").any{it.textContent=="Elemento nuevo"})
            assertTrue(result.sheet(1).nodes("c").any{it.textContent=="Apoyo 3"})
            for(sheet in 1..9) {
                val before=source.sheet(sheet).nodes("mergeCell").map{it.getAttribute("ref")}
                assertTrue(result.sheet(sheet).nodes("mergeCell").map{it.getAttribute("ref")}.containsAll(before))
                assertEquals(source.sheet(sheet).nodes("col").map{it.getAttribute("width")},result.sheet(sheet).nodes("col").map{it.getAttribute("width")})
                source.sheet(sheet).nodes("row").filter{it.namespaceURI==TemplateWorkbook.MAIN}.forEach{row->assertEquals(row.getAttribute("ht"),result.sheet(sheet).nodes("row").single{it.namespaceURI==TemplateWorkbook.MAIN&&it.getAttribute("r")==row.getAttribute("r")}.getAttribute("ht"))}
            }
        }}finally{db.close()}
    }
    @Test fun photoDefaultsCoverEverySourceCaptionAndEditableRecordsExportAspectRatioAndOriginalJpegUnchanged()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        val image=File(context.cacheDir,"iteration-photo.jpg")
        val bmp=Bitmap.createBitmap(120,240,Bitmap.Config.ARGB_8888);image.outputStream().use{bmp.compress(Bitmap.CompressFormat.JPEG,90,it)};bmp.recycle()
        try {for(type in ScapBridgeType.entries) {
            val source=TemplateWorkbook(context.assets.open(type.templateAsset))
            val strings=source.document("xl/sharedStrings.xml").nodes("si").map{it.textContent}
            val names=(11..42).map{row->source.cell(4,"D$row").let{cell->if(cell.getAttribute("t")=="s")strings[cell.textContent.toInt()] else cell.textContent}.substringBefore(':').trim()}.filter{it.isNotBlank()}.distinct()
            assertEquals(names,catalog.forType(type).photoNames)
            assertEquals(31,names.size)
            val repo=ScapRepository(db,catalog);val id=repo.create("Inspector","device",null,type)
            val photoId=repo.addPhotoRecord(id,names.first(),"")
            repo.setField(id,photoId,"photoName","AGUAS ABAJO EDITADA");repo.describePhoto(id,photoId,"Descripción editable")
            repo.setField(id,photoId,"photoDate","09/10/2026");repo.replacePhoto(id,photoId,image.absolutePath)
            val s=repo.dao.snapshot(id)!!;val out=ByteArrayOutputStream()
            ScapDelivery.exporter(context,s).write(out,s){ScapDelivery.readImage(context,it,photo=true,workbook=true)}
            val excel=TemplateWorkbook(out.toByteArray().inputStream())
            assertEquals("AGUAS ABAJO EDITADA: Descripción editable",excel.cell(4,"D11").textContent)
            assertTrue(excel.cell(4,"C11").textContent.isNotBlank())
            val drawingName=excel.document("xl/worksheets/_rels/sheet4.xml.rels").nodes("Relationship").single{it.getAttribute("Type").endsWith("/drawing")}.getAttribute("Target").substringAfterLast('/')
            val drawing=excel.document("xl/drawings/$drawingName")
            val picture=drawing.nodes("cNvPr").single{it.getAttribute("name")=="scap_4_1.jpg"}
            val anchor=picture.parentNode.parentNode.parentNode as org.w3c.dom.Element
            val extent=anchor.children().single{it.localName=="ext"}
            assertEquals(0.5,extent.getAttribute("cx").toDouble()/extent.getAttribute("cy").toDouble(),0.001)
            assertEquals(0,anchor.getElementsByTagNameNS("*","srcRect").length)
            val original=image.readBytes();val bundle=ScapDelivery.bundle(context,s)
            val entries=mutableMapOf<String,ByteArray>();ZipInputStream(bundle.inputStream()).use{zip->while(true){val entry=zip.nextEntry?:break;entries[entry.name]=zip.readBytes()}}
            assertArrayEquals(original,entries.getValue("fotos/$photoId.jpg"));assertArrayEquals(original,image.readBytes())
            repo.clearPhotoImage(id,photoId);assertEquals(photoId,repo.dao.snapshot(id)!!.photos.single().id)
            assertTrue(image.isFile);assertEquals("",repo.dao.snapshot(id)!!.photos.single().localPath)
        }}finally{db.close();image.delete()}
    }
    @Test fun missingValuesRequireUploadConfirmationWhileMalformedValuesBlockAndAckDoesNotFreezeDraft()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        try {
            val repo=ScapRepository(db,catalog);val id=repo.create("Inspector","device",null,ScapBridgeType.SLAB)
            repo.saveDraft(id);var s=repo.dao.snapshot(id)!!;val contract=catalog.forSnapshot(s)
            assertTrue(runCatching{ScapDriveSync.reviewForUpload(s,contract,false)}.isFailure)
            assertTrue(ScapDriveSync.reviewForUpload(s,contract,true).pending.size>=10)
            val e=s.elements.first().element;repo.setField(id,e.id,"quantity","abc");s=repo.dao.snapshot(id)!!
            assertTrue(ScapCompletion.assess(s,contract).pending.any{it.code=="INVALID_VALUE"&&it.ownerId==e.id})
            assertTrue(runCatching{ScapDriveSync.reviewForUpload(s,contract,true)}.isFailure)
            repo.setField(id,e.id,"quantity","");s=repo.dao.snapshot(id)!!
            for((key,value) in mapOf("reviewed" to "true","revision" to s.inspection.updatedAt.toString()))repo.dao.putValue(ScapFieldValueEntity(id,"delivery",key,value,"REVIEWED"))
            repo.dao.putValue(ScapFieldValueEntity(id,"completion","uploadedWithWarnings","true","REVIEWED"))
            s=repo.dao.snapshot(id)!!;assertTrue(ScapDriveSync.authorized(db,s))
            val files=JSONArray(listOf("SCAP.xlsx","manifest.json").map{JSONObject().put("path",it).put("fileId","f").put("folderId","d")})
            val ack=JSONObject().put("ok",true).put("protocol",1).put("recordKind","SCAP").put("scapInspectionId",id).put("revision",s.inspection.updatedAt).put("sha256","h").put("fileId","f").put("folderId","d").put("files",files)
            assertTrue(ScapDriveSync.acknowledge(db,s,ack,"h"));assertEquals("SYNCED",repo.dao.inspection(id)!!.syncStatus)
            repo.setField(id,"inspection","bridgeName","Completar en gabinete")
            assertEquals(id,repo.dao.snapshot(id)!!.inspection.id);assertEquals("PENDING",repo.dao.inspection(id)!!.syncStatus)
            assertEquals("f",repo.dao.snapshot(id)!!.values("drive")["fileId"])
        }finally{db.close()}
    }
    @Test fun nullRatingsCalculateExactlyAsExplicitZerosAndAllBlankNeverInventsClassification() {
        val partial=listOf(null,90.0,10.0,null,null,null)
        assertNull(ScapPercentages.error(partial))
        fun calculate(p:List<Double?>)=ScapConditionCalculator.calculate(listOf(ScapCalculationInput("a",0.8,p),ScapCalculationInput("b",0.9,p)))
        assertEquals(calculate(listOf(0.0,90.0,10.0,0.0,0.0,0.0)),calculate(partial))
        assertNull(calculate(List(6){null}).value);assertNull(calculate(List(6){null}).classification)
        assertNotNull(ScapPercentages.error(listOf(null,89.0,10.0,null,null,null)))
        val missingFactor=ScapConditionCalculator.calculate(listOf(ScapCalculationInput("a",null,partial),ScapCalculationInput("b",0.9,partial)))
        assertNull(missingFactor.value);assertNull(missingFactor.classification)
    }
}
