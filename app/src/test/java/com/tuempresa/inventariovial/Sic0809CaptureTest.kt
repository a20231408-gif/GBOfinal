package com.tuempresa.inventariovial

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.delivery.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.gis.KmlExporter
import com.tuempresa.inventariovial.sic.*
import com.tuempresa.inventariovial.server.ServerPayload
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File

internal fun fajaState(format:SicFieldFormat)=SicFieldState(format,mapOf(
    "routeCode" to "PE-22A","roadbedCode" to "CD","faja" to "2","startPrCode" to "0010","startDistanceM" to "12.3456",
    "endPrCode" to "0011","endDistanceM" to "20","surveyDate" to "11/10/2026")+
    if(format==SicFieldFormat.SIC08) mapOf("typeCode" to "1","directionCode" to "C")
    else mapOf("usefulWidthM" to "3.2567","totalWidthM" to "4.9876"))

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class Sic0809CaptureTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun database()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    @Test fun offlineRecordsWithNullCoordinatesAndLongObservationsPersistReopenAndEditIndependently()=runBlocking {
        val name="sic0809-${System.nanoTime()}"
        var db=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        val notes="Primera línea\n"+"Inspección de faja: áéíóú <&>\n".repeat(2000)+"Última línea"
        try {
            for(format in listOf(SicFieldFormat.SIC08,SicFieldFormat.SIC09))
                SicFieldRepository(db).save(format.code,fajaState(format).set("observations",notes),"engineer",true,sessionId="session")
            assertEquals(2,db.inventoryDao().observeAllRecords().first().size)
            db.close();db=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
            for(format in listOf(SicFieldFormat.SIC08,SicFieldFormat.SIC09)) {
                val repo=SicFieldRepository(db);val saved=repo.load(format.code,format)!!
                val record=db.inventoryDao().recordById(format.code)!!
                assertEquals(notes,saved.text("observations"));assertEquals(notes,record.observations)
                assertNull(record.latitude);assertNull(record.longitude);assertNull(record.endLatitude);assertNull(record.endLongitude)
                assertEquals("ACTIVE",record.status);assertEquals("session",record.sessionId)
                assertTrue(db.inventoryDao().track(record.id).isEmpty())
                assertEquals("PENDING_VALIDATION",db.automaticSyncDao().get("SIC/${record.id}")!!.state)
                assertTrue(ServerPayload.encode(db.inventoryDao().snapshot(record.id)!!).getJSONObject("location").isNull("latitude"))
                repo.save(record.id,saved.set("observations",notes+"\nCorrección"),"engineer",true)
                assertEquals(notes+"\nCorrección",repo.load(record.id,format)!!.text("observations"))
                assertEquals(record.createdAt,db.inventoryDao().recordById(record.id)!!.createdAt)
            }
            assertFalse(KmlExporter.render(db.inventoryDao().activeSnapshots()).contains("<Placemark>"))
        } finally {db.close();context.deleteDatabase(name)}
    }
    @Test fun technicalValidationIsIndependentAndObservationsAreOptional()=runBlocking {
        val db=database();try {
            for(f in listOf(SicFieldFormat.SIC08,SicFieldFormat.SIC09)) {
                val state=fajaState(f)
                assertTrue(state.completion().canFinalize)
                val incomplete=state.set("endPrCode","")
                assertFalse(incomplete.completion().canFinalize)
                assertTrue(runCatching {SicFieldRepository(db).save(f.code,incomplete,"engineer",true)}.isFailure)
                SicFieldRepository(db).save(f.code,incomplete,"engineer",false)
                assertEquals("DRAFT",db.inventoryDao().recordById(f.code)!!.status)
            }
            assertFalse(fajaState(SicFieldFormat.SIC09).set("totalWidthM","1").completion().canFinalize)
            assertTrue(db.inventoryDao().recordsForExport(null,null).isEmpty())
        } finally {db.close()}
    }
    @Test fun historicalLinkedWidthsKeepTheirOwnValuesAndCanFinalizeWithDraftParent()=runBlocking {
        val db=database();try {
            val repo=SicFieldRepository(db)
            repo.save("parent",fajaState(SicFieldFormat.SIC08),"engineer",false)
            val child=fajaState(SicFieldFormat.SIC09).copy(sourceRecordId="parent").set("routeCode","PE-28H").set("roadbedCode","A2")
            repo.save("child",child,"engineer",true)
            repo.save("parent",fajaState(SicFieldFormat.SIC08).set("faja","7"),"engineer",false)
            val loaded=repo.load("child",SicFieldFormat.SIC09)!!
            assertEquals("PE-28H",loaded.text("routeCode"));assertEquals("A2",loaded.text("roadbedCode"));assertEquals("2",loaded.text("faja"))
            assertEquals("parent",loaded.sourceRecordId)
            assertEquals(listOf("SIC-09"),ElementExports.prepare(context,db,"child",ExportHeading()).map {it.fileType})
            assertEquals("DRAFT",db.inventoryDao().recordById("parent")!!.status)
            assertTrue(runCatching {repo.save("child",fajaState(SicFieldFormat.SIC08),"engineer",true)}.isFailure)
        } finally {db.close()}
    }
    @Test fun editingObservationsPreservesHistoricalGnssMetadataTrackAndStableIdentity()=runBlocking {
        val db=database();try {
            val repo=SicFieldRepository(db)
            for(f in listOf(SicFieldFormat.SIC08,SicFieldFormat.SIC09)) {
                repo.save(f.code,fajaState(f),"engineer",true)
                val old=db.inventoryDao().recordById(f.code)!!.copy(latitude=-12.123456,longitude=-77.654321,altitudeM=235.0,
                    endLatitude=-12.2,endLongitude=-77.7,gpsAccuracyM=0.4,endGpsAccuracyM=0.5,locationSource="EXTERNAL_GNSS",
                    gnssProvider="EXTERNAL",gpsTimestamp=123,endGpsTimestamp=456,satellites=18,hdop=0.8,isRtkFixed=true,
                    sideSource="MANUAL_OVERRIDE",routeSource="GNSS_CONFIRMED",axisMeasureM=12650.0,sessionId="original")
                db.inventoryDao().updateRecord(old)
                db.inventoryDao().insertTrackPoint(TrackPointEntity("point-${f.code}",f.code,0,-12.1,-77.1,235.0,0.4,123))
                repo.save(f.code,repo.load(f.code,f)!!.set("observations","Texto nuevo"),"engineer",true,sessionId="different")
                val updated=db.inventoryDao().recordById(f.code)!!
                assertEquals(old.copy(observations="Texto nuevo",updatedAt=updated.updatedAt),updated)
                assertEquals(1,db.inventoryDao().track(f.code).size)
                assertEquals("Texto nuevo",repo.load(f.code,f)!!.text("observations"))
            }
        } finally {db.close()}
    }
    @Test fun officialWorkbooksKeepExactColumnsAndObservationsUseCompanionWithNullMetadata()=runBlocking {
        val db=database();try {
            val root=File(System.getProperty("user.dir"));val out=File(if(File(root,"app").exists())root else root.parentFile,"docs/sic08-sic09-fix-2026-10-11/exports").apply {mkdirs()}
            val comparison=JSONObject()
            for(f in listOf(SicFieldFormat.SIC08,SicFieldFormat.SIC09)) {
                val repo=SicFieldRepository(db);repo.save(f.code,fajaState(f),"engineer",true)
                val format=SicExportFormat.entries.single {it.code==f.code}
                val before=ByteArrayOutputStream().also {SicExcelWriter.write(it,db.inventoryDao().recordsForExport(f.code,null),format,ExportHeading())}.toByteArray()
                repo.save(f.code,repo.load(f.code,f)!!.set("observations","Línea 1\nLínea 2 <&>"),"engineer",true)
                val after=ByteArrayOutputStream().also {SicExcelWriter.write(it,db.inventoryDao().recordsForExport(f.code,null),format,ExportHeading())}.toByteArray()
                // ZIP timestamp may differ; compare every actual workbook part.
                fun parts(bytes:ByteArray)=java.util.zip.ZipInputStream(bytes.inputStream()).use {zip->buildMap<String,String> {while(true) {val e=zip.nextEntry ?: break;put(e.name,zip.readBytes().toString(Charsets.UTF_8))}}}
                assertEquals(parts(before),parts(after));assertEquals(10,format.columns.size)
                File(out,"${f.code}_antes.xlsx").writeBytes(before);File(out,"${f.code}_despues.xlsx").writeBytes(after)
                val generated=ElementExports.prepare(context,db,f.code,ExportHeading())
                assertEquals(setOf(f.code,"OBSERVACIONES"),generated.map {it.fileType}.toSet())
                assertTrue(generated.all {it.recordId==f.code})
                val html=generated.single {it.fileType=="OBSERVACIONES"}
                assertTrue(File(html.localPath).readText().contains("Línea 1\nLínea 2 &lt;&amp;&gt;"))
                File(html.localPath).copyTo(File(out,"${f.code}_observaciones.html"),overwrite=true)
                comparison.put(f.code,JSONObject().put("columnas",format.columns.map {it.label}).put("partesXlsxIdenticas",true).put("observaciones","HTML complementario y metadata JSON"))
                val snapshot=db.inventoryDao().snapshot(f.code)!!
                val folder=DrivePathPlanner.plan(listOf(RouteDelivery.row(snapshot)),com.tuempresa.inventariovial.road.ChainageCalculator()).folders.single()
                val metadata=DeliveryMetadata.element(folder,emptyList())
                assertEquals("Línea 1\nLínea 2 <&>",metadata.getString("observations"));assertTrue(metadata.getJSONObject("recordLocation").isNull("latitude"))
                File(out,"${f.code}_metadata.json").writeText(metadata.toString(2))
            }
            File(out,"comparacion.json").writeText(comparison.toString(2))
        } finally {db.close()}
    }
    @Test fun automaticDeliveryWithoutGeoreferencingStaysPendingAndDoesNotInventData()=runBlocking {
        val db=database();try {
            SicFieldRepository(db).save("local",fajaState(SicFieldFormat.SIC08),"engineer",true)
            val intent=db.automaticSyncDao().get("SIC/local")!!
            assertTrue(AutomaticDelivery.validated(db,intent))
            val result=runCatching {AutomaticDelivery.prepare(context,db,intent)}
            assertTrue(result.exceptionOrNull() is SyncPending)
            assertEquals("ACTIVE",db.inventoryDao().recordById("local")!!.status)
            assertNull(db.inventoryDao().recordById("local")!!.latitude)
        } finally {db.close()}
    }
    @Test fun migration16to17PreservesEveryHistoricalTableAndAllowsNullCoordinates()=runBlocking {
        val name="nullable-migration-${System.nanoTime()}"
        val schema=JSONObject(File("schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/16.json").readText()).getJSONObject("database")
        val old=android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name).also {it.parentFile!!.mkdirs()},null)
        val before=mutableMapOf<String,List<String>>()
        try {
            val entities=schema.getJSONArray("entities")
            for(i in 0 until entities.length()) {
                val entity=entities.getJSONObject(i);val table=entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}",table))
                val indices=entity.optJSONArray("indices") ?: org.json.JSONArray()
                for(j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}",table))
            }
            val temporary=database()
            try {
                val repo=SicFieldRepository(temporary);repo.saveFaja("historical08",fajaState(SicFieldFormat.SIC08),"historical09",mapOf("usefulWidthM" to "3","totalWidthM" to "4"),"engineer",true)
                temporary.inventoryDao().insertPhoto(PhotoEntity("photo","historical08",1,"/original.jpg",true,null,null,null,"SYNCED",123))
                temporary.inventoryDao().insertTrackPoint(TrackPointEntity("point","historical08",0,-12.0,-77.0,null,1.0,123))
                val source=temporary.openHelper.writableDatabase
                // Copy representative parent, child, photo, track and outbox using column names.
                for(table in listOf("inventory_records","sic_extension_details","photos","track_points","drive_sync_outbox")) source.query("SELECT * FROM `$table`").use {cursor->
                    while(cursor.moveToNext()) {
                        val values=android.content.ContentValues()
                        for(c in 0 until cursor.columnCount) {
                            val key=cursor.getColumnName(c)
                            when(cursor.getType(c)) {
                                android.database.Cursor.FIELD_TYPE_NULL -> if(table=="inventory_records" && key in setOf("latitude","longitude")) values.put(key,if(key=="latitude") -12.123 else -77.456) else values.putNull(key)
                                android.database.Cursor.FIELD_TYPE_INTEGER -> values.put(key,cursor.getLong(c))
                                android.database.Cursor.FIELD_TYPE_FLOAT -> values.put(key,cursor.getDouble(c))
                                else -> values.put(key,cursor.getString(c))
                            }
                        }
                        assertTrue(old.insertOrThrow(table,null,values)>0)
                    }
                }
            } finally {temporary.close()}
            // An additional sentinel in every table verifies SCAP, pavement, users and
            // all SIC details as well as the representative linked 08/09 records.
            for(i in 0 until entities.length()) {
                val entity=entities.getJSONObject(i);val table=entity.getString("tableName")
                val fields=entity.getJSONArray("fields");val values=android.content.ContentValues()
                for(j in 0 until fields.length()) {
                    val field=fields.getJSONObject(j);val column=field.getString("columnName")
                    when(field.getString("affinity")) {
                        "INTEGER" -> values.put(column,1L)
                        "REAL" -> values.put(column,1.25)
                        else -> values.put(column,"keep")
                    }
                }
                // Schema columns such as SCAP's `group` are SQL keywords.
                val columns=values.keySet().toList()
                old.execSQL(
                    "INSERT INTO `$table` (${columns.joinToString(",") { "`$it`" }}) VALUES (${columns.joinToString(",") { "?" }})",
                    columns.map { values.get(it) }.toTypedArray()
                )
            }
            for(i in 0 until entities.length()) {
                val table=entities.getJSONObject(i).getString("tableName")
                before[table]=old.rawQuery("SELECT * FROM `$table` ORDER BY 1",null).use {c->buildList {while(c.moveToNext())add((0 until c.columnCount).joinToString("|") {c.getString(it).orEmpty()})}}
            }
            old.version=16
        } finally {old.close()}
        val upgraded=Room.databaseBuilder(context,InventoryDatabase::class.java,name).addMigrations(InventoryDatabase.MIGRATION_16_17).build()
        try {
            val connection=upgraded.openHelper.writableDatabase // Full Room schema validation.
            before.forEach {(table,expected)->connection.query("SELECT * FROM `$table` ORDER BY 1").use {c->
                val actual=buildList {while(c.moveToNext())add((0 until c.columnCount).joinToString("|") {c.getString(it).orEmpty()})}
                assertEquals(table,expected,actual)
            }}
            connection.query("PRAGMA foreign_key_check").use {assertEquals(0,it.count)}
            SicFieldRepository(upgraded).save("new",fajaState(SicFieldFormat.SIC08),"engineer",true)
            assertNull(upgraded.inventoryDao().recordById("new")!!.latitude)
            assertEquals(-12.123,upgraded.inventoryDao().recordById("historical08")!!.latitude!!,0.0)
        } finally {upgraded.close();context.deleteDatabase(name)}
    }
}
