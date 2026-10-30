package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.export.*
import com.tuempresa.inventariovial.supplementary.FieldKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class ScapProgressiveTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun repo(db:InventoryDatabase)=ScapRepository(db,ScapCatalog.load(context))
    private suspend fun create(r:ScapRepository):String {
        val id=r.create("FICTICIO","FICTICIO",null,ScapBridgeType.MODULAR)
        r.setField(id,"inspection","route","PE-28H");r.setField(id,"inspection","sic17.roadbedCode","CD")
        return id
    }
    private fun assertLocation(s:ScapInspectionSnapshot,pr:String,metres:Double) {
        assertEquals(pr,s.roadRecord!!.startPrCode);assertEquals(metres,s.roadRecord!!.startDistanceM,0.000001)
        val preview=ScapSicExporter.previews(s).single{it.format=="SIC-17"}
        assertEquals(pr,preview.values[2]);assertEquals(metres,preview.values[3] as Double,0.000001)
    }
    @Test fun allConventionalExamplesPersistThroughFullDatabaseCloseAndReopen()=runBlocking {
        val name="ficticio-progressive-${UUID.randomUUID()}.db"
        val cases=listOf(Triple("10+100","0010",100.0),Triple("10+010","0010",10.0),Triple("10+000","0010",0.0),Triple("1+250","0001",250.0),Triple("0+050","0000",50.0))
        val ids=mutableListOf<String>()
        Room.databaseBuilder(context,InventoryDatabase::class.java,name).build().use{db->
            val r=repo(db)
            for((raw,pr,metres) in cases){val id=create(r);ids+=id;r.setField(id,"inspection","progressive",raw);assertLocation(r.dao.snapshot(id)!!,pr,metres)}
        }
        Room.databaseBuilder(context,InventoryDatabase::class.java,name).build().use{db->
            val r=repo(db)
            cases.forEachIndexed{i,(raw,pr,metres)->val s=r.dao.snapshot(ids[i])!!;assertEquals(raw,s.values()["progressive"]);assertLocation(s,pr,metres)}
            assertEquals(ids,ids.map{r.dao.snapshot(it)!!.inspection.id});assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }
    @Test fun everySavedKeystrokeAndLaterCorrectionsUpdateOnlyInheritedStart()=runBlocking {
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use{db->
            val r=repo(db);val id=create(r)
            val trace=mutableListOf<String>()
            for(raw in listOf("1","10","10+","10+1","10+10","10+100")) {
                r.setField(id,"inspection","progressive",raw);val s=r.dao.snapshot(id)!!
                trace+="$raw -> ${s.roadRecord!!.startPrCode} + ${s.roadRecord!!.startDistanceM}"
            }
            assertLocation(r.dao.snapshot(id)!!,"0010",100.0)
            r.saveSicLocation(id,"PE-28H","CD","0010","100","0012","25")
            r.useScapProgressive(id)
            val original=r.dao.snapshot(id)!!.roadRecord!!
            r.setField(id,"inspection","progressive","10+010")
            val changed=r.dao.snapshot(id)!!;assertLocation(changed,"0010",10.0)
            assertEquals(original.endPrCode,changed.roadRecord!!.endPrCode);assertEquals(original.endDistanceM,changed.roadRecord!!.endDistanceM)
            r.setField(id,"inspection","bridgeName","FICTICIO")
            assertEquals(changed.roadRecord,r.dao.snapshot(id)!!.roadRecord)
            val evidence=File("../docs/hotfix-progresiva-scap-sic-20261023/evidencias").apply{mkdirs()}
            File(evidence,"RECORRIDO_TECLAS_FICTICIO.txt").writeText(trace.joinToString("\n"))
        }
    }
    @Test fun manualConfirmedLocationSurvivesScapEditsAndExplicitChoiceRestoresInheritance()=runBlocking {
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use{db->
            val r=repo(db);val id=create(r);r.setField(id,"inspection","progressive","10+100")
            r.saveSicLocation(id,"PE-28H","CD","0020","35","0021","12")
            val manual=r.dao.snapshot(id)!!.roadRecord!!
            r.setField(id,"inspection","progressive","10+010")
            var s=r.dao.snapshot(id)!!;assertEquals(manual,s.roadRecord)
            assertLocation(s,"0020",35.0);assertTrue(ScapSicLocation.discrepancy(s)!!.contains("confirmada manualmente"));assertFalse(ScapSicLocation.needsReview(s))
            r.useScapProgressive(id);s=r.dao.snapshot(id)!!;assertLocation(s,"0010",10.0)
            assertEquals("0021",s.roadRecord!!.endPrCode);assertEquals(ScapSicLocation.INHERITED,s.roadRecord!!.locationSource)
            r.setField(id,"inspection","progressive","1+250");assertLocation(r.dao.snapshot(id)!!,"0001",250.0)
        }
    }
    @Test fun historicalFirstDigitIsNotSilentlyMigratedOrExportedAndCanBeExplicitlyCorrected()=runBlocking {
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use{db->
            val r=repo(db);val id=create(r);r.setField(id,"inspection","progressive","10+100")
            val good=r.dao.snapshot(id)!!.roadRecord!!
            db.inventoryDao().updateRecord(good.copy(startPrCode="0000",startDistanceM=1.0))
            val s=r.dao.snapshot(id)!!
            assertTrue(ScapSicLocation.needsReview(s));assertNotNull(ScapSicLocation.discrepancy(s))
            val p=ScapSicExporter.previews(s).first();assertNull(p.values[2]);assertNull(p.values[3]);assertTrue(p.pending.isNotEmpty())
            assertTrue(runCatching{ScapSicExporter.write(ByteArrayOutputStream(),s,"SIC-17")}.isFailure)
            assertEquals("0000",r.dao.snapshot(id)!!.roadRecord!!.startPrCode) // Reading/export validation has no writes.
            r.useScapProgressive(id);assertLocation(r.dao.snapshot(id)!!,"0010",100.0)
        }
    }
    @Test fun independentContradictionRequiresReviewAndDifferentRoutesCannotBeEquated()=runBlocking {
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use{db->
            val r=repo(db);val id=create(r);r.setField(id,"inspection","progressive","10+100")
            val stored=r.dao.snapshot(id)!!.roadRecord!!.copy(startPrCode="0008",startDistanceM=30.0,locationSource="MANUAL")
            db.inventoryDao().updateRecord(stored)
            r.setField(id,"inspection","progressive","10+010")
            assertEquals(stored,r.dao.snapshot(id)!!.roadRecord);assertTrue(ScapSicLocation.needsReview(r.dao.snapshot(id)!!))
            db.inventoryDao().updateRecord(stored.copy(routeCode="PE-3N"))
            assertTrue(runCatching{r.useScapProgressive(id)}.isFailure)
            r.saveSicLocation(id,"PE-3N","CD","0008","30")
            assertLocation(r.dao.snapshot(id)!!,"0008",30.0)
        }
    }
    @Test fun incompleteInputKeepsLastDataButCannotExportItAsCurrentScapLocation()=runBlocking {
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use{db->
            val r=repo(db);val id=create(r);r.setField(id,"inspection","progressive","10+100")
            for(raw in listOf("10+","", "10+1000","10++100")){
                r.setField(id,"inspection","progressive",raw);val s=r.dao.snapshot(id)!!
                assertEquals("0010",s.roadRecord!!.startPrCode);assertEquals(100.0,s.roadRecord!!.startDistanceM,0.0)
                assertTrue(ScapSicLocation.needsReview(s));assertNull(ScapSicExporter.previews(s).first().values[2])
            }
            r.setField(id,"inspection","progressive","0+050");assertLocation(r.dao.snapshot(id)!!,"0000",50.0)
        }
    }
    @Test fun realSicWorkbookContainsCorrectTextPrNumericMetresAndPreservesOtherFormats()=runBlocking {
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use{db->
            val r=repo(db);val id=create(r);r.setField(id,"inspection","progressive","10+100")
            r.saveSicLocation(id,"PE-28H","CD","0010","100","0011","20");r.useScapProgressive(id)
            for(format in BridgeSicContract.formats) for(key in BridgeSicContract.fields(format).map{it.key}.filter{it !in BridgeSicContract.unresolved(format) && !(format=="SIC-17" && it in BridgeSicContract.locationKeys)}){
                val f=BridgeSicContract.fields(format,BridgeSicContract.manualValues(r.dao.snapshot(id)!!,format)).single{it.key==key}
                val v=when(f.kind){
                    FieldKind.CODE,FieldKind.DESCRIBED_CODE->f.options.first()
                    FieldKind.INTEGER->"2";FieldKind.YEAR->"2001"
                    FieldKind.DECIMAL,FieldKind.SIGNED_DECIMAL->"8.4"
                    FieldKind.LATITUDE->"-12.1";FieldKind.LONGITUDE->"-77.1"
                    else->if(key=="surveyDate")"23/10/2026" else "FICTICIO_$key"
                };r.setSicField(id,format,key,v)
            }
            val before=r.dao.snapshot(id)!!
            val a=ScapSicExporter.previews(before).single{it.format=="SIC-17A"}
            val b=ScapSicExporter.previews(before).single{it.format=="SIC-17B"}
            assertTrue(BridgeSicContract.fields("SIC-17A").none{it.key in BridgeSicContract.locationKeys})
            assertTrue(BridgeSicContract.fields("SIC-17B").none{it.key in BridgeSicContract.locationKeys})
            for(raw in listOf("10+100","10+010")){
                r.setField(id,"inspection","progressive",raw);val s=r.dao.snapshot(id)!!
                val bytes=ByteArrayOutputStream();ScapSicExporter.write(bytes,s,"SIC-17")
                val w=TemplateWorkbook(bytes.toByteArray().inputStream())
                assertEquals("0010",w.cell(1,"C14").textContent)
                assertEquals(if(raw=="10+100")100.0 else 10.0,w.cell(1,"D14").textContent.toDouble(),0.0)
                assertEquals("inlineStr",w.cell(1,"C14").getAttribute("t"));assertNotEquals("inlineStr",w.cell(1,"D14").getAttribute("t"))
                assertEquals(a.values,ScapSicExporter.previews(s).single{it.format=="SIC-17A"}.values)
                assertEquals(b.values,ScapSicExporter.previews(s).single{it.format=="SIC-17B"}.values)
                File("../docs/hotfix-progresiva-scap-sic-20261023/evidencias/SIC17_${raw.replace('+','_')}_FICTICIO.xlsx").writeBytes(bytes.toByteArray())
                assertEquals(before.roadRecord!!.endPrCode,s.roadRecord!!.endPrCode)
                assertEquals(before.inspection.id,s.inspection.id);assertEquals(before.inspection.roadRecordId,s.inspection.roadRecordId)
            }
            assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }
}
