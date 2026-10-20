package com.tuempresa.inventariovial

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.sic.*
import com.tuempresa.inventariovial.catalog.EngineeringConditions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class SicNewFormatsTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    private fun assertSubmittedFields(expected:SicFieldState,actual:SicFieldState?) {
        requireNotNull(actual)
        assertEquals(expected.format,actual.format);assertEquals(expected.sourceRecordId,actual.sourceRecordId)
        expected.values.forEach {(key,value)->assertEquals(key,value,actual.text(key))}
    }
    private fun state(f:SicFieldFormat)=SicFieldState(f,mapOf("routeCode" to "PE-3N","roadbedCode" to "UC","faja" to "2",
        "startPrCode" to "0042","startDistanceM" to "12.3456","endPrCode" to "0043","endDistanceM" to "123.4567",
        "surveyDate" to "08/10/2026","typeCode" to "1","directionCode" to "C","sideCode" to "D","classCode" to "15",
        "usefulWidthM" to "3.456789","totalWidthM" to "4.987654","levelCode" to "2","description" to "Comentario libre\nSegunda línea"))
    private fun parent(id:String="parent")=InventoryRecordEntity(id,"SIC-18","CULVERT","PE-3N","UC","0042",12.5,null,null,null,
        -12.0,-77.0,null,2.0,"07/10/2026",null,"ACTIVE","PENDING","PENDING",10L,20L)
    private suspend fun culvert(db:InventoryDatabase):String {
        db.inventoryDao().insertRecord(parent())
        db.inventoryDao().insertSic18(Sic18Entity("parent","06","3",2,"2",1.25,null,"3","1"))
        return "parent"
    }
    private fun parts(bytes:ByteArray)=linkedMapOf<String,String>().also { result ->
        ZipInputStream(bytes.inputStream()).use {zip->while(true){val entry=zip.nextEntry ?: break;result[entry.name]=zip.readBytes().toString(Charsets.UTF_8)}}
    }
    @Test fun sic08AllCodesUseContractValuesAndRejectVisualIndexes() {
        val s=state(SicFieldFormat.SIC08)
        for(type in 1..6) for(direction in listOf("C","D","A")) {
            val row=s.set("typeCode","$type").set("directionCode",direction)
            assertTrue(row.completion().canFinalize);assertEquals("$type",row.exportValues()[7]);assertEquals(direction,row.exportValues()[8])
        }
        assertFalse(s.set("typeCode","0").completion().canFinalize)
        assertFalse(s.set("directionCode","0").completion().canFinalize)
        assertEquals(listOf("Ruta","Calzada","Faja","Código PR","Distancia","Código PR","Distancia","Tipo","Sentido del tránsito","Fecha"),s.format.columns.map {it.label})
    }
    @Test fun sic08MissingFieldsAndRawDraftSurviveReopen()=runBlocking {
        val name="sic08-${System.nanoTime()}"
        var db=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        try {
            val raw=SicFieldState(SicFieldFormat.SIC08,mapOf("startDistanceM" to "12,","surveyDate" to "08/10/2026"))
            assertTrue(raw.completion().canSaveDraft);assertFalse(raw.completion().canFinalize)
            SicFieldRepository(db).save("draft",raw,"operator",false)
            db.close();db=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
            assertSubmittedFields(raw,SicFieldRepository(db).load("draft",SicFieldFormat.SIC08))
            assertTrue(db.inventoryDao().recordsForExport(null,null).isEmpty())
        } finally {db.close();context.deleteDatabase(name)}
    }
    @Test fun sic09InheritsByUuidWithoutMutatingFaja()=runBlocking {
        val db=db();try {
            val repo=SicFieldRepository(db);repo.save("faja",state(SicFieldFormat.SIC08),"operator",true)
            val original=db.inventoryDao().sicExtension("faja")
            val inherited=repo.fromFaja("faja")
            assertEquals("faja",inherited.sourceRecordId);assertEquals("2",inherited.text("faja"));assertEquals("0043",inherited.text("endPrCode"))
            val width=inherited.set("usefulWidthM","3.456789").set("totalWidthM","4.987654")
            repo.save("width",width,"operator",true)
            assertSubmittedFields(width,repo.load("width",SicFieldFormat.SIC09));assertEquals(original,db.inventoryDao().sicExtension("faja"))
            assertEquals(3.456789,width.exportValues()[7]);assertEquals(4.987654,width.exportValues()[8])
        } finally {db.close()}
    }
    @Test fun sic09InvalidWidthsCanSaveDraftButCannotFinalizeOrExport()=runBlocking {
        val db=db();try {
            val invalid=state(SicFieldFormat.SIC09).set("totalWidthM","2.0")
            assertTrue(invalid.completion().canSaveDraft);assertFalse(invalid.completion().canFinalize)
            SicFieldRepository(db).save("width",invalid,"operator",false)
            assertTrue(runCatching {SicFieldRepository(db).save("width",invalid,"operator",true)}.isFailure)
            assertTrue(runCatching {invalid.exportValues()}.isFailure)
            assertFalse(invalid.set("usefulWidthM","NaN").completion().canFinalize)
        } finally {db.close()}
    }
    @Test fun sic13SidesClassesDescriptionsAndExactColumns() {
        val s=state(SicFieldFormat.SIC13)
        for(side in listOf("D","I")) for(cls in listOf("15","16","17","17A","17B","17C","17D")) {
            val row=s.set("sideCode",side).set("classCode",cls)
            assertTrue(row.completion().canFinalize);assertEquals(side,row.exportValues()[6]);assertEquals(cls,row.exportValues()[7])
            assertTrue(SicFieldFormat.criticalClasses.single {it.code==cls}.description.isNotBlank())
        }
        assertFalse(s.set("sideCode","S").completion().canFinalize)
        assertEquals("Zonas de Riesgo Probable",SicFieldFormat.criticalClasses.last().name)
        assertFalse(s.format.columns.any {it.label=="Faja" || it.label=="Tipo"})
    }
    @Test fun sic34NormalCodesLevelsAndFreeDescription() {
        val s=state(SicFieldFormat.SIC34)
        for(type in listOf("1","2","3","4","5","7")) for(level in listOf("1","2","3")) {
            val row=s.set("typeCode",type).set("levelCode",level)
            assertTrue(row.completion().canFinalize);assertEquals(type,row.exportValues()[7]);assertEquals(level,row.exportValues()[8]);assertEquals(s.text("description"),row.exportValues()[9])
        }
        assertTrue(s.set("description","").completion().canFinalize)
        assertFalse(s.set("levelCode","0").completion().canFinalize)
        assertEquals("Pérdidas humanas.",SicFieldFormat.levels.last().description)
    }
    @Test fun sic34SourceConflictIsNeverSilentlySixAndSurvivesDraft()=runBlocking {
        val db=db();try {
            val row=state(SicFieldFormat.SIC34).set("typeCode",ACCIDENT_TYPE_SOURCE_CONFLICT)
            assertTrue(row.completion().canSaveDraft);assertFalse(row.completion().canFinalize)
            val repo=SicFieldRepository(db);repo.save("acc",row,"operator",false)
            assertEquals(ACCIDENT_TYPE_SOURCE_CONFLICT,repo.load("acc",SicFieldFormat.SIC34)!!.text("typeCode"))
            assertTrue(runCatching {row.exportValues()}.isFailure)
            assertFalse(state(SicFieldFormat.SIC34).set("typeCode","6").completion().canFinalize)
        } finally {db.close()}
    }
    @Test fun sic18aParentLinkInheritedFieldsAndNoAutomaticCreation()=runBlocking {
        val db=db();try {
            val id=culvert(db);val dao=db.inventoryDao();val repo=SicFieldRepository(db)
            assertTrue(EngineeringConditions.badCulvert("3","1"));assertTrue(EngineeringConditions.badCulvert("1","3"));assertFalse(EngineeringConditions.badCulvert("2","2"))
            assertNull(dao.snapshot(id)!!.sic18a)
            val inherited=repo.load(id,SicFieldFormat.SIC18A)!!
            assertEquals(id,inherited.sourceRecordId);assertEquals("06",inherited.text("classCode"));assertEquals("3",inherited.text("typeCode"));assertEquals("2",inherited.text("spans"))
            assertEquals("07/10/2026",inherited.text("surveyDate"));assertFalse(inherited.format.hasEnd)
            repo.save(id,inherited,"operator",false)
            assertEquals("DRAFT",dao.sicExtension(id)!!.status);assertEquals("ACTIVE",dao.recordById(id)!!.status)
            assertTrue(runCatching {repo.save("orphan",SicFieldState(SicFieldFormat.SIC18A),"operator",false)}.isFailure)
        } finally {db.close()}
    }
    @Test fun sic18aAllMappingsAndSharedPhotosCompleteReopenExport()=runBlocking {
        val db=db();try {
            val id=culvert(db);val repo=SicFieldRepository(db)
            var s=repo.load(id,SicFieldFormat.SIC18A)!!
            for((key,max) in listOf("functionCode" to 4,"failureLocationCode" to 5,"failureTypeCode" to 7,"functionalStateCode" to 3,"probableCauseCode" to 4)) {
                val options=s.format.fields.single {it.key==key}.options
                assertEquals((1..max).map {"$it"},options.map {it.code})
                s=s.set(key,"$max")
            }
            val photo=PhotoEntity("photo",id,1,"/photos/original.jpg",true,null,null,null,"PENDING",1L,photoCategory="CULVERT_PANORAMIC")
            db.inventoryDao().insertPhoto(photo)
            repo.save(id,s,"operator",true,listOf(photo.localPath),mapOf(photo.localPath to "CULVERT_PANORAMIC"))
            val reopened=repo.load(id,SicFieldFormat.SIC18A)!!
            assertEquals("4",reopened.text("functionCode"));assertEquals(photo,db.inventoryDao().snapshot(id)!!.photos.single())
            val rows=db.inventoryDao().recordsForExport("SIC-18A",null)
            assertEquals(1,rows.size);assertEquals(13,SicExportFormat.SIC18A.values(rows.single()).size)
            assertEquals("06",SicExportFormat.SIC18A.values(rows.single())[4])
        } finally {db.close()}
    }
    @Test fun allNewWorkbooksShareHeaderExactColumnsAndNoInternalIds()=runBlocking {
        val db=db();try {
            val repo=SicFieldRepository(db)
            for(f in listOf(SicFieldFormat.SIC08,SicFieldFormat.SIC09,SicFieldFormat.SIC13,SicFieldFormat.SIC34)) {
                repo.save(f.code,state(f),"operator",true)
                val selected=SicExportFormat.entries.single {it.code==f.code}
                val out=ByteArrayOutputStream();SicExcelWriter.write(out,db.inventoryDao().recordsForExport(f.code,null),selected,ExportHeading())
                val p=parts(out.toByteArray());val sheet=p.getValue("xl/worksheets/sheet1.xml")
                assertTrue(sheet.contains(SicWorkbookHeaderWriter.CONTRACT_TITLE));assertTrue(sheet.contains(f.title))
                assertFalse(sheet.contains("operator"));assertFalse(sheet.contains("sourceRecordId"));assertFalse(sheet.contains("UUID"))
                assertEquals(f.columns.size,selected.values(db.inventoryDao().recordsForExport(f.code,null).single()).size)
                if(f==SicFieldFormat.SIC09) assertTrue(p.getValue("xl/styles.xml").contains("formatCode=\"0.0\""))
                assertTrue(sheet.contains(if(f.hasFaja) "D12:E12" else "C12:D12"))
            }
        } finally {db.close()}
    }
    @Test fun draftOwnerAndFinalRecordAreProtected()=runBlocking {
        val db=db();try {
            val repo=SicFieldRepository(db);repo.save("id",state(SicFieldFormat.SIC13),"owner",false)
            assertTrue(runCatching {repo.save("id",state(SicFieldFormat.SIC13),"other",false)}.isFailure)
            repo.save("id",state(SicFieldFormat.SIC13),"owner",true)
            // Owner editing is supported, retaining the UUID. Annulled rows remain protected.
            repo.save("id",state(SicFieldFormat.SIC13),"owner",false)
            assertEquals("id",db.inventoryDao().recordById("id")!!.id)
            assertEquals("owner",db.inventoryDao().sicExtension("id")!!.ownerUserId)
            db.inventoryDao().setRecordStatus("id","ANNULLED",1000)
            assertTrue(runCatching {repo.save("id",state(SicFieldFormat.SIC13),"owner",false)}.isFailure)
        } finally {db.close()}
    }
    @Test fun deliveryUsesNewFamiliesAndSupplementUsesParentFolderOnlyWhenComplete()=runBlocking {
        val db=db();try {
            val repo=SicFieldRepository(db)
            val parentId=culvert(db)
            var extra=repo.load(parentId,SicFieldFormat.SIC18A)!!
            listOf("functionCode","failureLocationCode","failureTypeCode","functionalStateCode","probableCauseCode").forEach {extra=extra.set(it,"1")}
            repo.save(parentId,extra,"operator",false)
            for(f in listOf(SicFieldFormat.SIC08,SicFieldFormat.SIC09,SicFieldFormat.SIC13,SicFieldFormat.SIC34)) repo.save(f.code,state(f),"operator",true)
            val delivery=com.tuempresa.inventariovial.delivery.RouteDelivery(context,db)
            val reference=com.tuempresa.inventariovial.road.RoadReferenceRepository(context).loadFromAssets()
            fun payloads(path:String):List<org.json.JSONObject> {
                val files=org.json.JSONObject(java.io.File(path).readText()).getJSONArray("files")
                return (0 until files.length()).map {files.getJSONObject(it).getJSONObject("payload")}.filter {it.getString("kind")=="sic"}
            }
            val draft=delivery.prepare("PE-3N",reference,com.tuempresa.inventariovial.road.MatchConfig())
            assertFalse(payloads(draft.batch.manifestPath).any {it.getString("sicCode")=="SIC-18A"})
            repo.save(parentId,extra,"operator",true)
            val ready=delivery.prepare("PE-3N",reference,com.tuempresa.inventariovial.road.MatchConfig())
            val payloads=payloads(ready.batch.manifestPath).associateBy {it.getString("sicCode")}
            for((code,family) in listOf("SIC-08" to "FAJAS_CALZADA","SIC-09" to "ANCHOS_FAJA","SIC-13" to "PUNTOS_CRITICOS","SIC-34" to "ACCIDENTES"))
                assertTrue(payloads.getValue(code).getString("logicalPath").startsWith("PE-3N/$family/"))
            assertEquals(payloads.getValue("SIC-18").getString("logicalPath"),payloads.getValue("SIC-18A").getString("logicalPath"))
            assertEquals(parentId,payloads.getValue("SIC-18A").getString("recordId"))
            val directory=java.io.File("build/outputs/sic-new/contractual-examples").apply {mkdirs()}
            val root=java.io.File(ready.batch.manifestPath).parentFile!!
            val files=org.json.JSONObject(java.io.File(ready.batch.manifestPath).readText()).getJSONArray("files")
            for(i in 0 until files.length()) {
                val e=files.getJSONObject(i);val p=e.getJSONObject("payload")
                if(p.getString("kind")=="sic" && p.getString("sicCode")!="SIC-18")
                    java.io.File(root,e.getString("localFile")).copyTo(java.io.File(directory,p.getString("fileName")),overwrite=true)
            }
        } finally {db.close()}
    }
    @Test fun dashboardSyncRequiresAcknowledgedBatchAndCurrentRecord() {
        val dir=java.io.File(context.cacheDir,"sic-status-test").apply {mkdirs()}
        val manifest=java.io.File(dir,"manifest.json").apply {writeText("""{"deliveryId":"batch","fingerprint":"hash","files":[{"payload":{"kind":"sic","recordId":"parent","sicCode":"SIC-18A"}}]}""")}
        val batch=DeliveryBatchEntity("batch","PE-3N","hash",manifest.absolutePath,"FOLDER_COMPACT","PREPARED",30)
        assertTrue(SicDeliveryStatus.acknowledged(listOf(batch),listOf(parent())).isEmpty())
        assertEquals(setOf("parent" to "SIC-18A"),SicDeliveryStatus.acknowledged(listOf(batch.copy(status="SYNCED")),listOf(parent())))
        assertTrue(SicDeliveryStatus.acknowledged(listOf(batch.copy(status="SYNCED")),listOf(parent().copy(updatedAt=31))).isEmpty())
        manifest.delete()
    }
    @Test fun sharedContextReusesConfirmedStartEndDateOnlyWithinSameSurvey() {
        val session=FieldSession("survey","project","operator","device","PE-3N","Tramo 1","UC",startTime=1)
        val contextStore=SicCaptureContext(context)
        val confirmed=state(SicFieldFormat.SIC08)
        contextStore.remember(session,confirmed)
        assertEquals(confirmed.values.filterKeys {it in SicFieldRepository.sharedKeys},contextStore.values(session))
        assertTrue(contextStore.values(session.copy(sessionId="other")).isEmpty())
        assertTrue(contextStore.values(session.copy(roadbed="CI")).isEmpty())
        assertTrue(contextStore.values(null).isEmpty())
        contextStore.remember(session,confirmed.set("startDistanceM","12,"))
        assertEquals("12.3456",contextStore.values(session)["startDistanceM"])
    }
    @Test fun supplementaryDraftWithoutPhotoEditsKeepsAllParentPhotos()=runBlocking {
        val db=db();try {
            val id=culvert(db);val photo=PhotoEntity("keep",id,1,"/photos/keep.jpg",true,null,null,null,"PENDING",1L,photoCategory="CULVERT_OUTLET")
            db.inventoryDao().insertPhoto(photo)
            val repo=SicFieldRepository(db);repo.save(id,repo.load(id,SicFieldFormat.SIC18A)!!,"operator",false)
            assertEquals(photo,db.inventoryDao().snapshot(id)!!.photos.single())
        } finally {db.close()}
    }
}
