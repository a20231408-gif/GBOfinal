package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.repository.*
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.sic.*
import com.tuempresa.inventariovial.supplementary.*
import com.tuempresa.inventariovial.validation.*
import com.tuempresa.inventariovial.catalog.SicCatalogRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class TechnicalRules4Test {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    private fun endpoint(pr:String="0010",offset:String="100")=LengthEndpoint("R","CD",pr,offset)
    private val prs=listOf(RoadPr("R","CD","0010",-12.0,-76.0,400.0),RoadPr("R","CD","0011",-12.0,-76.0,1750.0))
    @Test fun samePrUsesCompleteOffsetsAndKeepsDecimalPrecision(){
        val r=ContractualLength.calculate(endpoint(offset="100,125"),endpoint(offset="200.375"),emptyList(),"INCREASING")
        assertEquals(100.25,r.metres!!,0.000001);assertEquals("SAME_PR_OFFSETS",r.source)
    }
    @Test fun differentPrUsesActualNonKilometricCatalogSpacing(){
        assertEquals(1450.25,ContractualLength.calculate(endpoint(offset="100.125"),endpoint("0011","200.375"),prs,"INCREASING").metres!!,0.000001)
    }
    @Test fun decreasingDirectionHasPositivePhysicalLength(){
        assertEquals(1450.0,ContractualLength.calculate(endpoint("0011","200"),endpoint("0010","100"),prs,"DECREASING").metres!!,0.0)
        assertNull(ContractualLength.calculate(endpoint("0011","200"),endpoint("0010","100"),prs,"INCREASING").metres)
    }
    @Test fun missingAndAmbiguousPrNeverInventKilometers(){
        assertNull(ContractualLength.calculate(endpoint(),endpoint("0011"),emptyList(),"INCREASING").metres)
        assertNull(ContractualLength.calculate(endpoint(),endpoint("0011"),prs+prs.first(),"INCREASING").metres)
        assertNull(ContractualLength.calculate(endpoint(offset=""),endpoint(),prs,"INCREASING").metres)
        assertNull(ContractualLength.calculate(endpoint(),endpoint(pr=""),prs,"INCREASING").metres)
    }
    @Test fun distinctRoutesAndRoadbedsAreRejected(){
        assertNull(ContractualLength.calculate(endpoint(),endpoint().copy(route="OTHER"),prs,"INCREASING").metres)
        assertNull(ContractualLength.calculate(endpoint(),endpoint().copy(bed="UD"),prs,"INCREASING").metres)
    }
    @Test fun newMaterialsUseExistingOfficialCodesAndPreserveHistoricalSelections(){
        val guard=Sic21FormState("19","1","1")
        assertTrue(SicCatalogRepository.options("sic21.material.0").any {it=="1 - Acero"})
        assertEquals("2",TechnicalElementRules.selectSafetyType(guard,"4",true).materialCode)
        assertEquals("1",TechnicalElementRules.selectSafetyType(guard.copy(typeCode="4"),"1",true).materialCode)
        assertEquals("4",TechnicalElementRules.selectSafetyType(guard.copy(materialCode="4"),"4",false).materialCode)
        assertEquals("4",TechnicalElementRules.selectSafetyType(guard.copy(materialCode="4"),"1",true).materialCode)
    }
    @Test fun tunnelAndWallSideRulesDoNotAffectFord(){
        fun request(cls:String,side:String)=InventorySaveRequest("SIC-20","X","R","CD","0010","100","0010","200",side,Double.NaN,Double.NaN,null,"11/10/2026","","",SicFormDetail.Sic20(Sic20FormState(classCode=cls)))
        for(side in listOf("I","D")) assertTrue(CaptureValidation.errors(request("13",side)).any {it.field=="side"})
        assertFalse(CaptureValidation.errors(request("13","S")).any {it.field=="side"})
        assertTrue(CaptureValidation.errors(request("14","S")).any {it.field=="side"})
        for(side in listOf("I","D")) assertFalse(CaptureValidation.errors(request("14",side)).any {it.field=="side"})
        assertFalse(CaptureValidation.errors(request("12","S")).any {it.field=="side"})
        assertFalse(CaptureValidation.errors(request("12","S").copy(sideCode=null)).any {it.field=="side"})
    }
    private fun extra(condition:String)=SicFieldState(SicFieldFormat.SIC18A,mapOf(
        "routeCode" to "R","roadbedCode" to "CD","startPrCode" to "0010","startDistanceM" to "100","surveyDate" to "11/10/2026",
        "classCode" to "06","typeCode" to "1","spans" to "1","functionCode" to "3",
        "structuralConditionCode" to condition,"functionalConditionCode" to condition),"culvert")
    @Test fun goodRegularAndBadRequireIndependentFunctionAndAppropriateFailures(){
        assertTrue(extra("1").completion().canFinalize)
        assertTrue(SupplementaryFormState(SupplementaryFormat.SIC18A,extra("1").values).validate().isEmpty())
        assertFalse(extra("1").set("functionCode","").completion().canFinalize)
        for(c in listOf("2","3")) {
            assertTrue(InventoryElements.requiresCulvertDetail(c,"1"))
            assertFalse(extra(c).completion().canFinalize)
            assertTrue(withFailures(extra(c)).completion().canFinalize)
        }
    }
    private fun withFailures(s:SicFieldState)=s.copy(values=s.values+mapOf("failureLocationCode" to "1","failureTypeCode" to "2","functionalStateCode" to "2","probableCauseCode" to "3"))
    @Test fun culvertConditionChangesPersistReopenAndExportSameUuidWithoutLosingFailures()=runBlocking {
        db().use {d->
            val dao=d.inventoryDao();val record=testRecord("culvert",sic="SIC-18",route="R").copy(latitude=null,longitude=null)
            dao.insertRecord(record);dao.insertSic18(Sic18Entity("culvert","06","1",1,"1",1.0,1.0,"2","2"))
            val repo=SicFieldRepository(d);repo.save("culvert",withFailures(extra("2")),"engineer",true)
            val loaded=repo.load("culvert",SicFieldFormat.SIC18A)!!
            assertEquals("3",loaded.text("functionCode"));assertEquals("2",loaded.text("failureTypeCode"))
            val old=dao.snapshot("culvert")!!
            EngineeringEdits.save(d,"culvert",SicFormDetail.Sic18((EngineeringEdits.form(old) as SicFormDetail.Sic18).state.copy(structuralConditionCode="1",functionalConditionCode="1")),emptyList(),emptyMap())
            repo.save("culvert",repo.load("culvert",SicFieldFormat.SIC18A)!!.set("functionCode","4"),"engineer",true)
            val row=dao.recordsForExport(null,null).single()
            assertEquals("culvert",row.sic18a!!.recordId);assertEquals("4",row.sic18a.functionCode);assertEquals("2",row.sic18a.failureTypeCode)
            assertTrue(InventoryElements.exportFormats(listOf(row)).any {it.first==SicExportFormat.SIC18A})
            val bytes=ByteArrayOutputStream();SicExcelWriter.write(bytes,listOf(row),SicExportFormat.SIC18A,ExportHeading())
            assertTrue(bytes.size()>0);assertEquals("Función",SicFieldFormat.SIC18A.fields[7].label)
            assertTrue(dao.recordById("culvert")!!.updatedAt>record.updatedAt)
        }
    }
    @Test fun wallAutomaticModeUpdatesAfterProgressiveEditButHistoricalAndMeasuredRemain()=runBlocking {
        db().use {d->
            val r=testRecord("wall",sic="SIC-20",route="R").copy(assetType="WALL",startPrCode="0010",startDistanceM=100.0,endPrCode="0010",endDistanceM=200.0,surveyDirection="INCREASING",sideCode="D")
            val repo=InventoryRepository(d);repo.saveSic20(r,Sic20Entity("wall","14","2",3.0,null,"1","1",100.0),emptyList(),"AUTO")
            val edited=r.copy(endDistanceM=250.0);d.inventoryDao().updateRecord(edited);TechnicalLengthStore.refresh(d,edited,emptyList())
            assertEquals(150.0,d.inventoryDao().snapshot("wall")!!.sic20!!.wallLengthMeters!!,0.0)
            TechnicalLengthStore.remember(d,edited,"MEASURED");TechnicalLengthStore.refresh(d,edited.copy(endDistanceM=300.0),emptyList())
            assertEquals(150.0,d.inventoryDao().snapshot("wall")!!.sic20!!.wallLengthMeters!!,0.0)
            val s=d.inventoryDao().snapshot("wall")!!;assertEquals("MEASURED",(EngineeringEdits.form(s) as SicFormDetail.Sic20).state.lengthMode)
            d.inventoryDao().saveSicExtension(s.sicExtension!!.copy(valuesJson="{}"));TechnicalLengthStore.refresh(d,edited,emptyList())
            assertEquals(150.0,d.inventoryDao().snapshot("wall")!!.sic20!!.wallLengthMeters!!,0.0)
        }
    }
    @Test fun lengthCompanionContainsMeasuredAndCalculatedValuesWithoutChangingSicColumns(){
        val r=testRecord("wall",sic="SIC-20",route="R").copy(assetType="WALL",startPrCode="0010",startDistanceM=100.0,endPrCode="0010",endDistanceM=200.0,surveyDirection="INCREASING")
        val item=InventoryRecordForExport(r,sic20=Sic20Entity("wall","14","2",3.0,null,"1","1",95.25))
        val m=TechnicalLengthExport.metadata(item,emptyList());assertEquals(100.0,m.getDouble("calculatedM"),0.0);assertEquals(95.25,m.getDouble("applicableM"),0.0)
        val out=ByteArrayOutputStream();TechnicalLengthExport.write(out,item,emptyList())
        val sheet=ZipInputStream(out.toByteArray().inputStream()).use {zip->var xml="";while(true){val e=zip.nextEntry ?: break;if(e.name=="xl/worksheets/sheet1.xml")xml=zip.readBytes().toString(Charsets.UTF_8)};xml}
        assertTrue(sheet.contains("95.25"));assertTrue(sheet.contains("100.0"));assertTrue(sheet.contains("wall"))
        assertFalse(SicExportFormat.SIC20.columns.any {it.label.contains("Longitud",true)})
    }
    @Test fun linearFamiliesAreScopedToTheirActualModels(){
        assertTrue(TechnicalElementRules.linear(SicFormDetail.Sic19(Sic19FormState())))
        assertTrue(TechnicalElementRules.linear(SicFormDetail.Sic21(Sic21FormState("20"))))
        assertTrue(TechnicalElementRules.linear(SicFormDetail.Sic21(Sic21FormState("19","1"))))
        assertFalse(TechnicalElementRules.linear(SicFormDetail.Sic21(Sic21FormState("19","4"))))
        assertFalse(TechnicalElementRules.linear(SicFormDetail.Sic20(Sic20FormState("13"))))
    }
    @Test fun databaseReopenKeepsFunctionFailuresMaterialAndMeasuredWallLength()=runBlocking {
        val name="technical4-${java.util.UUID.randomUUID()}.db"
        var d=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        try {
            val dao=d.inventoryDao()
            dao.insertRecord(testRecord("culvert",sic="SIC-18",route="R"))
            dao.insertSic18(Sic18Entity("culvert","06","1",1,"1",1.0,1.0,"3","3"))
            SicFieldRepository(d).save("culvert",withFailures(extra("3")),"engineer",true)
            InventoryRepository(d).saveSic20(testRecord("wall",sic="SIC-20").copy(assetType="WALL"),Sic20Entity("wall","14","2",3.0,null,"1","1",87.75),emptyList(),"MEASURED")
            InventoryRepository(d).saveSic21(testRecord("resalto",sic="SIC-21"),Sic21Entity("resalto","19","4","2","1"),emptyList())
            d.close();d=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
            val loaded=SicFieldRepository(d).load("culvert",SicFieldFormat.SIC18A)!!
            assertEquals("3",loaded.text("functionCode"));assertEquals("2",loaded.text("failureTypeCode"));assertTrue(loaded.completion().canFinalize)
            assertEquals(87.75,d.inventoryDao().snapshot("wall")!!.sic20!!.wallLengthMeters!!,0.0)
            val row=d.inventoryDao().recordsForExport(null,null).single {it.record.id=="resalto"}
            assertEquals("2",row.sic21!!.materialCode);assertTrue(SicExportFormat.SIC21.values(row).contains("2"))
        } finally {d.close()}
    }
    @Test fun tunnelSidePersistsAndHistoricalSideIsNeverMigratedSilently()=runBlocking {
        db().use {d->
            val repo=InventoryRepository(d);val r=testRecord("tunnel",sic="SIC-20").copy(assetType="TUNNEL")
            val detail=Sic20Entity("tunnel","13","1",4.0,5.0,"1","1")
            assertTrue(runCatching {repo.saveSic20(r,detail,emptyList())}.isFailure)
            repo.saveSic20(r.copy(sideCode="S"),detail,emptyList())
            val row=d.inventoryDao().recordsForExport(null,null).single()
            assertEquals("S",row.record.sideCode);assertTrue(SicExportFormat.SIC20.values(row).contains("S"))
            d.inventoryDao().updateRecord(row.record.copy(sideCode="D"))
            val s=d.inventoryDao().snapshot("tunnel")!!
            EngineeringEdits.save(d,"tunnel",SicFormDetail.Sic20((EngineeringEdits.form(s) as SicFormDetail.Sic20).state.copy(structuralConditionCode="2")),emptyList(),emptyMap())
            assertEquals("D",d.inventoryDao().recordById("tunnel")!!.sideCode)
        }
    }
    @Test fun v3LengthCompanionReusesStableKeyAndReflectsProgressiveEditWithoutGps()=runBlocking {
        db().use {d->
            val r=testRecord("auto-wall",sic="SIC-20",route="PE-22A").copy(assetType="WALL",latitude=null,longitude=null,
                startPrCode="0010",startDistanceM=100.0,endPrCode="0010",endDistanceM=200.0,surveyDirection="INCREASING")
            InventoryRepository(d).saveSic20(r,Sic20Entity(r.id,"14","2",3.0,null,"1","1",100.0),emptyList(),"AUTO")
            val intent=d.automaticSyncDao().get("SIC/${r.id}")!!
            val first=com.tuempresa.inventariovial.core.AutomaticDelivery.prepare(context,d,intent)
            val repeated=com.tuempresa.inventariovial.core.AutomaticDelivery.prepare(context,d,intent)
            assertEquals(first.getString("contentHash"),repeated.getString("contentHash"))
            fun file(p:org.json.JSONObject,key:String):org.json.JSONObject {
                val a=p.getJSONArray("files");return (0 until a.length()).map {a.getJSONObject(it)}.single {it.getString("key")==key}
            }
            val old=file(first,"technical-length");assertEquals("longitud.xlsx",old.getString("path"))
            val meta=org.json.JSONObject(java.io.File(file(first,"metadata").getString("localFile")).readText())
            assertTrue(meta.isNull("latitude"));assertEquals(100.0,meta.getJSONObject("technicalLength").getDouble("applicableM"),0.0)
            val next=r.copy(endDistanceM=250.0,updatedAt=r.updatedAt+100)
            d.inventoryDao().updateRecord(next);TechnicalLengthStore.refresh(d,next,emptyList())
            com.tuempresa.inventariovial.core.AutomaticSync.request(d,"SIC",r.id,next.updatedAt)
            val changed=com.tuempresa.inventariovial.core.AutomaticDelivery.prepare(context,d,d.automaticSyncDao().get(intent.key)!!)
            assertEquals(3,changed.getInt("syncProtocol"));assertEquals(4,changed.getInt("pathVersion"));assertEquals(r.id,changed.getString("recordId"))
            assertEquals(old.getString("key"),file(changed,"technical-length").getString("key"))
            assertNotEquals(old.getString("sha256"),file(changed,"technical-length").getString("sha256"))
        }
    }
}
