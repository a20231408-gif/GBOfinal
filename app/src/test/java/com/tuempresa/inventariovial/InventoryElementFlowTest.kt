package com.tuempresa.inventariovial

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.field.*
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.repository.EngineeringEdits
import com.tuempresa.inventariovial.sic.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28],application=android.app.Application::class)
class InventoryElementFlowTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    private fun faja()=SicFieldState(SicFieldFormat.SIC08,mapOf("routeCode" to "PE-3N","roadbedCode" to "UC","faja" to "1","startPrCode" to "0010","startDistanceM" to "0","endPrCode" to "0012","endDistanceM" to "500","surveyDate" to "08/10/2026","typeCode" to "1","directionCode" to "C"))
    private suspend fun culvert(db:InventoryDatabase,bad:Boolean=false):String {
        val id="culvert"
        db.inventoryDao().insertRecord(InventoryRecordEntity(id,"SIC-18","CULVERT","PE-3N","UC","0010",125.0,null,null,null,-12.0,-77.0,null,2.0,"08/10/2026",null,"ACTIVE","PENDING","PENDING",1,2))
        db.inventoryDao().insertSic18(Sic18Entity(id,"06","3",2,"2",1.25,null,if(bad)"3" else "2","1",sectionShape="CIRCULAR"))
        return id
    }
    private suspend fun fill(db:InventoryDatabase,id:String) {
        val repo=SicFieldRepository(db);var state=repo.load(id,SicFieldFormat.SIC18A)!!
        culvertAdditionalKeys.forEach {state=state.set(it,"1")};repo.save(id,state,"operator",true)
    }
    @Test fun fajaCharacteristicsSaveReopenThenWidthsFinalizeAsOneElement()=runBlocking {
        val name="ui-faja-${System.nanoTime()}";var db=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        try {
            SicFieldRepository(db).saveFaja("faja",faja(),"width",emptyMap(),"operator",true)
            assertNull(db.inventoryDao().recordById("width"))
            db.close();db=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
            val repo=SicFieldRepository(db);val reopened=repo.load("faja",SicFieldFormat.SIC08)!!
            repo.saveFaja("faja",reopened,"width",mapOf("usefulWidthM" to "3.25","totalWidthM" to "4.50"),"operator",true)
            val records=db.inventoryDao().observeAllRecords().first();val extensions=db.inventoryDao().observeSicExtensions().first()
            val grouped=InventoryElements.group(records,extensions)
            assertEquals(1,grouped.size);assertEquals("faja",grouped.single().record.id);assertEquals("width",grouped.single().children.single().id)
            assertEquals("faja",repo.load("width",SicFieldFormat.SIC09)!!.sourceRecordId)
            val files=ElementExports.prepare(context,db,"faja",ExportHeading())
            assertEquals(setOf("SIC-08"),files.map {it.fileType}.toSet());assertTrue(files.all {it.recordId=="faja"})
        } finally {db.close();context.deleteDatabase(name)}
    }
    @Test fun rawWidthsAndPartialCharacteristicsRemainDraftsWithoutDuplicatingTheirParent()=runBlocking {
        val db=db();try {
            val repo=SicFieldRepository(db)
            repo.saveFaja("draft",faja().set("startDistanceM","12,"),"width",mapOf("usefulWidthM" to "3,"),"operator",false)
            assertEquals("DRAFT",db.inventoryDao().recordById("draft")!!.status)
            assertEquals("3,",repo.load("width",SicFieldFormat.SIC09)!!.text("usefulWidthM"))
            assertEquals(1,InventoryElements.group(db.inventoryDao().observeAllRecords().first(),db.inventoryDao().observeSicExtensions().first()).size)
            assertTrue(db.inventoryDao().recordsForExport(null,null).isEmpty())
        } finally {db.close()}
    }
    @Test fun inheritedCommonFieldsFollowCurrentFajaWhilePreservingWidthUuidAndRawValues()=runBlocking {
        val db=db();try {
            val repo=SicFieldRepository(db)
            repo.saveFaja("faja",faja(),"width",mapOf("usefulWidthM" to "3,"),"operator",false)
            repo.saveFaja("faja",faja().set("startPrCode","0011"),"width",mapOf("usefulWidthM" to "3,"),"operator",false)
            val width=repo.load("width",SicFieldFormat.SIC09)!!
            assertEquals("0011",width.text("startPrCode"));assertEquals("3,",width.text("usefulWidthM"));assertEquals("faja",width.sourceRecordId)
        } finally {db.close()}
    }
    @Test fun historicalUnlinkedWidthsAndMultipleLinkedMeasurementsAreNeverLostOrGuessed()=runBlocking {
        val db=db();try {
            val repo=SicFieldRepository(db);repo.save("faja",faja(),"operator",true)
            for(id in listOf("width1","width2")) repo.save(id,repo.fromFaja("faja").set("usefulWidthM","3").set("totalWidthM","4"),"operator",true)
            repo.save("legacy",SicFieldState(SicFieldFormat.SIC09,faja().values+mapOf("usefulWidthM" to "3","totalWidthM" to "4")),"operator",true)
            val original=db.inventoryDao().observeSicExtensions().first()
            val groups=InventoryElements.group(db.inventoryDao().observeAllRecords().first(),original)
            assertEquals(2,groups.size);assertEquals(2,groups.single {it.record.id=="faja"}.children.size)
            assertEquals("legacy",groups.single {it.record.id=="legacy"}.record.id)
            assertEquals(original,db.inventoryDao().observeSicExtensions().first())
            assertEquals(1,ElementExports.prepare(context,db,"faja",ExportHeading()).size)
        } finally {db.close()}
    }
    @Test fun legacyCulvertRegularRequiresFailuresAndKeepsOneIdentity()=runBlocking {
        val db=db();try {
            val id=culvert(db)
            assertTrue(InventoryElements.requiresCulvertDetail("2","1"))
            assertEquals("Pendiente",InventoryElements.culvertDetailStatus(db.inventoryDao().snapshot(id)!!.sic18!!,null,null))
            assertEquals(listOf("SIC-18"),ElementExports.prepare(context,db,id,ExportHeading()).map {it.fileType})
            val base=(EngineeringEdits.form(db.inventoryDao().snapshot(id)!!) as SicFormDetail.Sic18).state
            EngineeringEdits.save(db,id,SicFormDetail.Sic18(base.copy(structuralConditionCode="3")),emptyList(),emptyMap())
            assertTrue(InventoryElements.requiresCulvertDetail("3","1"));fill(db,id)
            val reopened=db.inventoryDao().snapshot(id)!!
            assertEquals("1",reopened.sic18a!!.functionCode);assertEquals(id,reopened.sicExtension!!.sourceRecordId)
            assertEquals(1,db.inventoryDao().observeAllRecords().first().size)
            assertEquals(setOf("SIC-18","SIC-18A"),ElementExports.prepare(context,db,id,ExportHeading()).map {it.fileType}.toSet())
        } finally {db.close()}
    }
    @Test fun goodConditionKeepsFunctionAndSupplementHistoryExportable()=runBlocking {
        val db=db();try {
            val id=culvert(db,true);fill(db,id)
            val saved=db.inventoryDao().snapshot(id)!!.sic18a
            val base=(EngineeringEdits.form(db.inventoryDao().snapshot(id)!!) as SicFormDetail.Sic18).state
            EngineeringEdits.save(db,id,SicFormDetail.Sic18(base.copy(structuralConditionCode="1")),emptyList(),emptyMap())
            val current=db.inventoryDao().snapshot(id)!!
            assertEquals(saved,current.sic18a)
            assertEquals("No requerido · antecedente conservado",InventoryElements.culvertDetailStatus(current.sic18!!,current.sic18a,current.sicExtension))
            assertEquals(setOf("SIC-18","SIC-18A"),ElementExports.prepare(context,db,id,ExportHeading()).map {it.fileType}.toSet())
        } finally {db.close()}
    }
    @Test fun culvertRawEditDraftPersistsInvalidNumbersAndIsIsolatedByOperator() {
        val id="draft-${System.nanoTime()}";val metadata=com.tuempresa.inventariovial.camera.PhotoCaptureMetadata(123L,com.tuempresa.inventariovial.location.GeoLocation(-12.0,-77.0,2.0f,timestamp=123L))
        val d=CulvertEditingDraft(Sic18FormState(dimension1M="12,"),mapOf("functionCode" to "2"),listOf("/original/photo.jpg"),mapOf("/original/photo.jpg" to "CULVERT_INLET"),mapOf("/original/photo.jpg" to metadata))
        try {
            CulvertEditDraft.save(context,id,"operator",d)
            assertEquals(d,CulvertEditDraft.read(context,id,"operator"));assertNull(CulvertEditDraft.read(context,id,"another"))
        } finally {CulvertEditDraft.clear(context,id,"operator")}
    }
    @Test fun culvertSupplementEnteredBeforeBaseFinalizationKeepsItsParentForeignKey()=runBlocking {
        val db=db();try {
            val id=culvert(db);val record=db.inventoryDao().recordById(id)!!
            db.inventoryDao().updateRecord(record.copy(status="DRAFT"))
            SicFieldRepository(db).saveCulvertDraft(id,mapOf("functionCode" to "2"),"operator")
            assertEquals(id,db.inventoryDao().sicExtension(id)!!.sourceRecordId)
            db.inventoryDao().updateRecord(record)
            assertEquals("2",SicFieldRepository(db).load(id,SicFieldFormat.SIC18A)!!.text("functionCode"))
        } finally {db.close()}
    }
    @Test fun oneDriveFolderForLinkedFajaFormatsKeepsChildAckIdAndDoesNotMarkPreparedAsSynced()=runBlocking {
        val db=db();try {
            val repo=SicFieldRepository(db);repo.saveFaja("faja",faja(),"width",mapOf("usefulWidthM" to "3","totalWidthM" to "4"),"operator",true)
            val reference=com.tuempresa.inventariovial.road.RoadReferenceRepository(context).loadFromAssets()
            val delivery=com.tuempresa.inventariovial.delivery.RouteDelivery(context,db).prepare("PE-3N",reference,com.tuempresa.inventariovial.road.MatchConfig())
            assertEquals(1,delivery.plan.folders.size)
            val entries=JSONObject(java.io.File(delivery.batch.manifestPath).readText()).getJSONArray("files")
            val sic=(0 until entries.length()).map {entries.getJSONObject(it).getJSONObject("payload")}.filter {it.getString("kind")=="sic"}
            assertEquals(setOf("SIC-08","SIC-09"),sic.map {it.getString("sicCode")}.toSet())
            assertEquals(1,sic.map {it.getString("logicalPath")}.distinct().size)
            assertEquals("width",sic.single {it.getString("sicCode")=="SIC-09"}.getString("recordId"))
            assertTrue(SicDeliveryStatus.acknowledged(listOf(delivery.batch),db.inventoryDao().observeAllRecords().first()).isEmpty())
        } finally {db.close()}
    }
    @Test fun generatedFileDiscoveryAndGroupingRetainElementContextForOldAndNewFilenameOrder()=runBlocking {
        val db=db();try {
            val repo=SicFieldRepository(db);repo.saveFaja("faja",faja(),"width",mapOf("usefulWidthM" to "3","totalWidthM" to "4"),"operator",true)
            assertEquals("faja",GeneratedFileIndex.recordIdFromName("SIC-08_faja.xlsx"));assertEquals("width",GeneratedFileIndex.recordIdFromName("width_SIC-09.xlsx"))
            assertNull(GeneratedFileIndex.recordIdFromName("SIC-09.xlsx"))
            val files=listOf(GeneratedFile("a","faja","SIC","SIC-08","/a",SicExcelWriter.XLSX_MIME,1,"LOCAL"),GeneratedFile("b","width","SIC","SIC-09","/b",SicExcelWriter.XLSX_MIME,2,"LOCAL"))
            val grouped=GeneratedFileIndex.groupByElement(files,db.inventoryDao().observeAllRecords().first(),db.inventoryDao().observeSicExtensions().first())
            assertEquals(setOf("faja"),grouped.keys);assertEquals(2,grouped.getValue("faja").size)
        } finally {db.close()}
    }
    @Test fun coordinatesAndCleanPhotographsOfNewElementsSurviveSaveAndReopen()=runBlocking {
        val db=db();try {
            val state=faja().copy(format=SicFieldFormat.SIC13,values=faja().values+mapOf("sideCode" to "D","classCode" to "15","_latitude" to "-12.2","_longitude" to "-77.3","_accuracy" to "2.5","_gpsExplicit" to "true","_coordinateSource" to "TABLET_GNSS"))
            SicFieldRepository(db).save("critical",state,"operator",true,listOf("/original/clean.jpg"))
            val s=db.inventoryDao().snapshot("critical")!!
            assertEquals(-12.2,requireNotNull(s.record.latitude),0.0);assertEquals("TABLET_GNSS",s.record.locationSource)
            assertEquals("/original/clean.jpg",s.photos.single().localPath);assertEquals("GENERAL",s.photos.single().photoCategory)
        } finally {db.close()}
    }
}
