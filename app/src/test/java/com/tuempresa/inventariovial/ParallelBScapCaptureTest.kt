package com.tuempresa.inventariovial

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.export.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class ParallelBScapCaptureTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun catalog(type:ScapBridgeType)=ScapCatalog.parseCanonical(File("src/main/assets/${type.assetName}").readText(),type)
    private fun db()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()

    @Test fun selectionCreatesDifferentCaptureModelsAndPersistsType()=runBlocking {
        ScapBridgeType.entries.forEach {type->
            val database=db()
            try {
                val repo=ScapRepository(database,catalog(type));val id=repo.create("Inspector","device",null,type)
                repo.addRow(id,"SPAN")
                val snapshot=repo.dao.snapshot(id)!!;val model=ScapCaptureModels.forType(type)
                assertEquals(type,ScapBridgeType.from(snapshot))
                assertEquals(model.category,snapshot.spans.single().category)
                assertEquals(model.structuralType,snapshot.spans.single().type)
                assertTrue(model.sections.containsAll(listOf("D4","G1","SIC")))
            } finally {database.close()}
        }
        assertNotEquals(ModularScapCapture::class,SlabScapCapture::class)
        assertNotEquals(ScapBridgeType.MODULAR.templateAsset,ScapBridgeType.SLAB.templateAsset)
    }

    @Test fun canonicalCatalogContainsEverySourceOptionAndGUnitsFactors() {
        ScapBridgeType.entries.forEach {type->
            val cat=catalog(type)
            // Read the actual approved workbook instead of an absent, untracked extraction JSON.
            val workbook=TemplateWorkbook(context.assets.open(type.templateAsset))
            val strings=if(workbook.parts.containsKey("xl/sharedStrings.xml")) workbook.document("xl/sharedStrings.xml").nodes("si").map {it.textContent} else emptyList()
            val map=JSONObject().put("sheets",org.json.JSONArray().apply {
                workbook.document("xl/workbook.xml").nodes("sheet").forEachIndexed {index,sheet->
                    put(JSONObject().put("name",sheet.getAttribute("name")).put("cells",org.json.JSONArray().apply {
                        workbook.sheet(index+1).nodes("c").forEach {cell->
                            val raw=cell.children().firstOrNull {it.localName=="v"}?.textContent.orEmpty()
                            val value:Any=when(cell.getAttribute("t")) {
                                "s"->strings[raw.toInt()]
                                "inlineStr"->cell.textContent
                                else->raw.toDoubleOrNull() ?: raw
                            }
                            put(JSONObject().put("cell",cell.getAttribute("r")).put("value",value))
                        }
                    }))
                }
            })
            val sheets=map.getJSONArray("sheets")
            val aux=(0 until sheets.length()).map{sheets.getJSONObject(it)}.first{it.getString("name")=="AUXILIAR"}.getJSONArray("cells")
            val sourceCodes=(0 until aux.length()).map{aux.getJSONObject(it)}.filter{it.getString("cell").matches(Regex("C[0-9]+")) && it.get("value") is Number && it.getDouble("value")>=100}.map{it.getInt("value").toString()}.toSet()
            assertEquals(sourceCodes,cat.elements.map{it.code}.toSet())
            assertEquals(112,cat.elements.size)
            assertTrue(cat.elements.all{it.unit in cat.options("units")})
            assertEquals(setOf(.2,.4,.6,.8,1.0),cat.options("importanceFactors").map{it.toDouble()}.toSet())
            val g=(0 until sheets.length()).map{sheets.getJSONObject(it)}.first{it.getString("name")=="G.- CONDICION ESTADISTICA"}.getJSONArray("cells")
            val cells=(0 until g.length()).map{g.getJSONObject(it)}.associate{it.getString("cell") to it.get("value")}
            (14..44).filter{cells["A$it"] is Number}.forEach {row->
                val code=(cells["A$row"] as Number).toInt().toString()
                val canonical=cat.canonicalElements.single{it.exportCode==code}
                val element=cat.element(canonical.templateCode)!!
                assertTrue(canonical.templateCode in cat.defaultElements)
                assertEquals(cells["E$row"].toString().trim(),element.unit)
                assertEquals((cells["F$row"] as Number).toDouble(),element.importanceFactor,0.0)
            }
            assertTrue(cat.generalHeader.startsWith("SERVICIO DE CONSULTORIA"))
            assertEquals("E16",cat.fields.first{it.key=="latitude"}.valueCell)
            assertEquals("E18",cat.fields.first{it.key=="longitude"}.valueCell)
            assertFalse(cat.fields.any{it.key in listOf("segment","utmEasting","utmNorthing")})
        }
        assertTrue("168" in catalog(ScapBridgeType.MODULAR).defaultElements)
        assertTrue("104" in catalog(ScapBridgeType.SLAB).defaultElements)
        assertTrue("und" in catalog(ScapBridgeType.MODULAR).options("units"))
    }

    @Test fun incompleteDraftAndRawTextSurviveDatabaseReopen()=runBlocking {
        val name="parallel-b-${UUID.randomUUID()}.db";var database=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        try {
            val cat=catalog(ScapBridgeType.SLAB);var repo=ScapRepository(database,cat)
            val id=repo.create("","device",null,ScapBridgeType.SLAB)
            repo.setField(id,"inspection","totalLengthM","-")
            repo.setSicLocationDraft(id,"startPrCode","12")
            repo.saveDraft(id)
            assertEquals("DRAFT",repo.dao.inspection(id)!!.status)
            database.close();database=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build();repo=ScapRepository(database,cat)
            val restored=repo.dao.snapshot(id)!!
            assertEquals("-",restored.values()["totalLengthM"])
            assertEquals("12",restored.values()["sicLocation.startPrCode"])
            assertEquals("MANUAL_PENDING_SIC3",restored.values.first{it.key=="sicLocation.startPrCode"}.source)
            assertEquals(ScapBridgeType.SLAB,ScapBridgeType.from(restored))
            assertTrue(runCatching{repo.complete(id)}.isFailure)
            assertEquals("DRAFT",repo.dao.inspection(id)!!.status)
            repo.reopen(id);repo.setField(id,"inspection","bridgeName","Continuado")
            assertEquals("Continuado",repo.dao.snapshot(id)!!.inspection.bridgeName)
        } finally {database.close();context.deleteDatabase(name)}
    }

    @Test fun manualGPreservesContractualMetradoUnitFactorAndFlagsMissingCondition()=runBlocking {
        val database=db()
        try {
            val cat=catalog(ScapBridgeType.MODULAR);val repo=ScapRepository(database,cat)
            val id=repo.create("Inspector","device",null,ScapBridgeType.MODULAR)
            repo.addManualElement(id,"X-42","Elemento observado","12,75","ml","0.8")
            val snapshot=repo.dao.snapshot(id)!!;val element=snapshot.elements.single{it.element.elementCode=="X-42"}.element
            assertEquals("MANUAL",snapshot.values(element.id)["elementSource"])
            assertEquals(12.75,element.quantity!!,0.0);assertEquals("ml",element.unit);assertEquals(.8,element.importanceFactor,0.0)
            val review=ScapValidation.review(snapshot,cat)
            assertFalse(review.errors.any{it.contains("catálogo o factor")})
            assertTrue(review.pending.any{it.contains("evaluación") || it.contains("incompletos")})
            assertTrue(ScapCompletion.assess(snapshot,cat).pending.any{it.section=="G1" && it.ownerId==element.id})
            assertTrue(runCatching{repo.addManualElement(id,"168","No mezclar catálogo","1","und","1")}.isFailure)
            assertTrue(runCatching{repo.addManualElement(id," 168 ","No mezclar catálogo","1","und","1")}.isFailure)
            repo.saveDraft(id)
            assertEquals("DRAFT",repo.dao.inspection(id)!!.status)
            repo.setField(id,element.id,"importanceFactor","0.4")
            assertEquals(.4,repo.dao.snapshot(id)!!.elements.single{it.element.elementCode=="X-42"}.element.importanceFactor,0.0)
            repo.addPhoto(id,"/images/manual.jpg","ELEMENT","X-42")
            val photo=repo.dao.snapshot(id)!!.photos.single()
            repo.setPhotoMetadata(id,photo.id,"DEFECT","X-42")
            assertEquals("X-42",repo.dao.snapshot(id)!!.photos.single().scapElementCode)
            repo.selectElement(id,"X-42",false)
            val retained=repo.dao.snapshot(id)!!
            assertFalse(retained.elements.single{it.element.elementCode=="X-42"}.element.isPresent)
            assertEquals(12.75,retained.elements.single{it.element.elementCode=="X-42"}.element.quantity!!,0.0)
            assertEquals(1,retained.photos.size)
            repo.selectElement(id,"X-42",true)
            assertTrue(repo.dao.snapshot(id)!!.elements.single{it.element.elementCode=="X-42"}.element.isPresent)
        } finally {database.close()}
    }

    @Test fun canonicalManualCoordinatesAreReusedForSic17AWithoutGnssRequirement()=runBlocking {
        val database=db()
        try {
            val cat=catalog(ScapBridgeType.SLAB);val repo=ScapRepository(database,cat);val id=repo.create("Inspector","device",null,ScapBridgeType.SLAB)
            repo.setField(id,"inspection","latitude","-16.2");repo.setField(id,"inspection","longitude","-70.3")
            val snapshot=repo.dao.snapshot(id)!!;val fields=ScapToSicMapper.mapToSic17A(snapshot).fields.associate{it.sicField to it.value}
            assertEquals("-16.2",fields["latitude"]);assertEquals("-70.3",fields["longitude"])
            assertFalse(ScapValidation.review(snapshot,cat).pending.any{it.contains("GNSS")})
            BridgeSicContract.formats.forEach {format->
                val mapping=when(format){"SIC-17"->ScapToSicMapper.mapToSic17(snapshot);"SIC-17A"->ScapToSicMapper.mapToSic17A(snapshot);else->ScapToSicMapper.mapToSic17B(snapshot)}
                assertFalse(BridgeSicContract.assess(mapping).isComplete)
                assertTrue(BridgeSicContract.assess(mapping).requiredMissing.isNotEmpty())
            }
        } finally {database.close()}
    }

    @Test fun finalizationValidatesPresentGAndDoesNotInventMandatoryGnssOrMedia()=runBlocking {
        val database=db()
        try {
            val cat=catalog(ScapBridgeType.SLAB);val repo=ScapRepository(database,cat)
            val id=repo.create("Inspector","device",null,ScapBridgeType.SLAB)
            mapOf("bridgeName" to "Puente","bridgeCode" to "P-1","route" to "MO-104","progressive" to "12+500").forEach{(key,value)->repo.setField(id,"inspection",key,value)}
            repo.addRow(id,"SPAN");repo.selectElement(id,"104",true)
            val element=repo.dao.snapshot(id)!!.elements.single{it.element.elementCode=="104"}.element
            (0..5).forEach{repo.setField(id,element.id,"percent$it",if(it==1) "100" else "0")}
            repo.complete(id,warningsConfirmed=true)
            assertEquals("INCOMPLETE",repo.dao.snapshot(id)!!.values("completion")["status"])
            repo.reopen(id)
            repo.setField(id,element.id,"quantity","0")
            val snapshot=repo.dao.snapshot(id)!!
            assertTrue(ScapValidation.review(snapshot,cat).errors.isEmpty())
            assertTrue(ScapCompletion.assess(snapshot,cat).pending.any{it.section=="B"})
            repo.complete(id,warningsConfirmed=true)
            assertEquals("COMPLETE",repo.dao.inspection(id)!!.status)
            repo.reopen(id);repo.saveDraft(id)
            assertEquals(17,repo.dao.snapshot(id)!!.elements.size)
        } finally {database.close()}
    }

    @Test fun observedNumericLiteralsRemainExactAndNumericValidationRemainsBounded() {
        val cat=catalog(ScapBridgeType.MODULAR)
        val expected=mapOf("constructionYear" to "Sin Informacion","traffic" to "SIN DATOS","truckBusPercent" to "SIN DATOS","sidewalkWidthM" to "N/A","slabThicknessM" to "5 pulg","shoulderWidthM" to "N/A")
        expected.forEach {(key,status)->
            val field=cat.fields.first{it.key==key && it.kind=="NUMERIC_OR_SOURCE_STATUS"}
            assertEquals(listOf(status),cat.options(field.catalog))
            assertNull(ScapValidation.fieldError(field,status,emptyMap(),cat))
            assertNotNull(ScapValidation.fieldError(field,"Texto inventado",emptyMap(),cat))
        }
        val percent=cat.fields.first{it.key=="truckBusPercent"}
        assertNotNull(ScapValidation.fieldError(percent,"101",emptyMap(),cat))
        assertNull(ScapValidation.fieldError(percent,"95",emptyMap(),cat))
        val year=cat.fields.first{it.key=="constructionYear"}
        assertNotNull(ScapValidation.fieldError(year,"2",emptyMap(),cat))
        assertNull(ScapValidation.fieldError(year,"2026",emptyMap(),cat))
        assertTrue(ScapFieldPolicy.enabled(cat.fields.first{it.key=="sidewalkWidthM" && it.section=="B"},emptyMap()))
    }
}
