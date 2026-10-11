package com.tuempresa.inventariovial

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.export.*
import com.tuempresa.inventariovial.supplementary.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class BridgeSicFlowTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    private fun repo(db:InventoryDatabase)=ScapRepository(db,ScapCatalog.load(context))
    private fun value(field:SupplementaryField):String=when(field.kind) {
        FieldKind.CODE,FieldKind.DESCRIBED_CODE -> field.options.first()
        FieldKind.INTEGER -> "2";FieldKind.YEAR->"2001";FieldKind.LATITUDE->"-12.123456789";FieldKind.LONGITUDE->"-77.123456789"
        FieldKind.DECIMAL,FieldKind.SIGNED_DECIMAL->"8.40"
        else->"SIC_${field.key}"
    }
    private suspend fun fill(repo:ScapRepository,id:String,format:String) {
        BridgeSicContract.fields(format).filter{it.key !in BridgeSicContract.unresolved(format)}.forEach { field ->
            repo.setSicField(id,format,field.key,value(field))
            val preview=ScapSicExporter.previews(repo.dao.snapshot(id)!!).single {it.format==format}
            assertFalse(field.label,(preview.missingRequired+preview.missingOptional).contains(field.label))
        }
    }
    @Test fun contractualLocationIsInheritedAndNeverReplacedByScapEdits()=runBlocking {
        val db=db()
        try {
            val r=repo(db);val id=r.create("Inspector","device",null)
            r.saveSicLocation(id,"PE-3N","UC","0042","125.5","0043","10")
            r.setField(id,"inspection","route","DIFFERENT_SCAP_ROUTE")
            r.setField(id,"inspection","progressive","99+999")
            r.setField(id,"inspection","bridgeName","Puente")
            r.setField(id,"inspection","sic17.roadbedCode","UD") // historical duplicate must not win
            val s=r.dao.snapshot(id)!!
            val fields=ScapToSicMapper.mapToSic17(s).fields.associate{it.sicField to it.value}
            assertEquals("0042",fields["startPrCode"]);assertEquals("125.5",fields["startDistanceM"])
            assertEquals("PE-3N",fields["routeCode"]);assertEquals("UC",fields["roadbedCode"])
            assertEquals("0043",fields["endPrCode"])
            assertFalse(s.values().containsKey("sic17.startPrCode"))
            val preview=ScapSicExporter.preview(ScapToSicMapper.mapToSic17(s))
            assertEquals("0042",preview.values[2]);assertEquals(125.5,preview.values[3])
            assertFalse(preview.missingRequired.any{it in setOf("Código PR","Distancia desde PR (m)")})
        } finally {db.close()}
    }
    @Test fun allDefinedSupplementaryFieldsReopenAndExportRealCodesAndValues()=runBlocking {
        val name="bridge-sic-${System.nanoTime()}.db"
        fun open()=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        var db=open()
        try {
            var r=repo(db);val id=r.create("Inspector","device",null)
            r.setField(id,"inspection","bridgeName","SCAP ORIGINAL")
            r.setField(id,"inspection","roadwayWidthM","6.4")
            r.addRow(id,"PIER");r.addRow(id,"PIER")
            fill(r,id,"SIC-17A");fill(r,id,"SIC-17B")
            r.setSicField(id,"SIC-17B","wearingSurface","3 - Acero")
            r.setSicField(id,"SIC-17B","comments","Comentario SIC\nSegunda línea")
            val keys=listOf("pierElevationCode","pierMaterialCode","pierFoundationCode")
            keys.forEach{r.setSicField(id,"SIC-17B",it,"0 - No aplica")}
            r.setField(id,"inspection","route","PE-22A")
            val point=com.tuempresa.inventariovial.road.RoadReferenceRepository(context).loadFromAssets().segments.single {it.routeCode=="PE-22A"}.points.first()
            r.setLocation(id,com.tuempresa.inventariovial.location.GeoLocation(point.latitude,point.longitude,1.0f))
            val stored=r.dao.snapshot(id)!!
            ScapHumanDelivery.prepare(context,db,stored)
            assertEquals(stored,r.dao.snapshot(id))
            assertEquals("3",stored.sic17b!!.wearingSurface)
            assertEquals("3 - Acero",stored.values()["sic17b.wearingSurfaceDescription"])
            assertEquals("SCAP_OVERRIDE:roadwayWidthM",stored.values()["sic17a.override.roadwayWidthM"])
            db.close();db=open();r=repo(db)
            r.setField(id,"inspection","bridgeName","SCAP EDITED")
            val reopened=r.dao.snapshot(id)!!
            assertEquals(stored.sic17a,reopened.sic17a);assertEquals(stored.sic17b,reopened.sic17b)
            assertEquals("SCAP EDITED",reopened.values()["bridgeName"])
            for(format in listOf("SIC-17A","SIC-17B")) {
                val mapping=if(format=="SIC-17A") ScapToSicMapper.mapToSic17A(reopened) else ScapToSicMapper.mapToSic17B(reopened)
                val state=BridgeSicContract.assess(mapping)
                assertTrue(state.missing.toString(),state.missing.isEmpty())
                val preview=ScapSicExporter.preview(mapping)
                assertTrue(preview.missingRequired.isEmpty());assertTrue(preview.missingOptional.isEmpty())
                val out=ByteArrayOutputStream();ScapSicExporter.write(out,reopened,format)
                val workbook=TemplateWorkbook(out.toByteArray().inputStream())
                val expected=preview.values
                val cells=workbook.sheet(1).nodes("c").filter{it.getAttribute("r").endsWith("4")}
                assertEquals(expected.size,cells.size)
                expected.forEachIndexed {index,v->if(v!=null) assertEquals(v.toString(),cells[index].textContent)}
                if(format=="SIC-17A") {
                    assertEquals("7",cells[7].getAttribute("s"));assertEquals("7",cells[8].getAttribute("s"))
                    java.util.zip.ZipInputStream(out.toByteArray().inputStream()).use {zip ->
                        var style="";var entry=zip.nextEntry
                        while(entry!=null) {if(entry.name=="xl/styles.xml") style=zip.readBytes().toString(Charsets.UTF_8);entry=zip.nextEntry}
                        assertTrue(style.contains("0.000000000"))
                    }
                }
                if(format=="SIC-17B") {
                    assertEquals(listOf("3"),mapping.fields.filter{it.sicField=="wearingSurface"}.map{it.value})
                    assertTrue(state.notApplicable.containsAll(listOf("Pilares: elevación","Pilares: material","Pilares: cimentación")))
                    assertTrue(state.unresolved.single().startsWith("Capacidad máxima"))
                    assertNull(preview.values[2]);assertEquals("Comentario SIC\nSegunda línea",preview.values.last())
                }
            }
            assertEquals(15,db.openHelper.readableDatabase.version)
        } finally {db.close();context.deleteDatabase(name)}
    }
    @Test fun overridesKeepSourceRestoreScapAndNeverInferStructuresOrLanes()=runBlocking {
        val db=db()
        try {
            val r=repo(db);val id=r.create("Inspector","device",null)
            r.setField(id,"inspection","roadwayWidthM","6.0")
            r.setField(id,"inspection","trafficWays","4")
            r.addRow(id,"SPAN");r.addRow(id,"SPAN")
            r.setSicField(id,"SIC-17A","roadwayWidthM","8.5")
            r.setSicField(id,"SIC-17A","roadwayWidthM","8.6")
            r.setField(id,"inspection","roadwayWidthM","7.0")
            var s=r.dao.snapshot(id)!!
            assertEquals("8.6",ScapToSicMapper.mapToSic17A(s).fields.single{it.sicField=="roadwayWidthM"}.value)
            assertEquals("SCAP_OVERRIDE:roadwayWidthM",s.values()["sic17a.override.roadwayWidthM"])
            assertFalse(ScapToSicMapper.mapToSic17A(s).fields.any{it.sicField=="lanes"})
            assertFalse(ScapToSicMapper.mapToSic17B(s).fields.any{it.sicField in setOf("beams","abutmentMaterialCode","pierMaterialCode")})
            r.setSicField(id,"SIC-17A","roadwayWidthM","")
            assertFalse(ScapToSicMapper.mapToSic17A(r.dao.snapshot(id)!!).fields.any{it.sicField=="roadwayWidthM"})
            r.restoreSicField(id,"SIC-17A","roadwayWidthM");s=r.dao.snapshot(id)!!
            assertEquals("7.0",ScapToSicMapper.mapToSic17A(s).fields.single{it.sicField=="roadwayWidthM"}.value)
            assertTrue(runCatching{r.setSicField(id,"SIC-17B","maximumCapacity","10")}.isFailure)
        } finally {db.close()}
    }
    @Test fun publishedVersion11MigrationKeepsHistoricalBridgeLocationPhotosAndSupplements()=runBlocking {
        val name="bridge-sic-migration-${System.nanoTime()}.db"
        val file=context.getDatabasePath(name);file.parentFile!!.mkdirs()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file,null).use { old ->
            val schema=org.json.JSONObject(File("schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/11.json").readText()).getJSONObject("database").getJSONArray("entities")
            for(i in 0 until schema.length()) {
                val e=schema.getJSONObject(i);val table=e.getString("tableName")
                old.execSQL(e.getString("createSql").replace("\u0024{TABLE_NAME}",table))
                val indices=e.optJSONArray("indices") ?: org.json.JSONArray()
                for(j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\u0024{TABLE_NAME}",table))
            }
            fun insert(table:String,values:Map<String,String>) {
                val cv=android.content.ContentValues()
                old.rawQuery("PRAGMA table_info(`$table`)",null).use { cursor -> while(cursor.moveToNext()) {
                    val column=cursor.getString(1);val type=cursor.getString(2);val required=cursor.getInt(3)==1
                    when {
                        column in values -> cv.put(column,values.getValue(column))
                        !required -> cv.putNull(column)
                        type=="TEXT" -> cv.put(column,"")
                        else -> cv.put(column,0)
                    }
                } }
                old.insertOrThrow(table,null,cv)
            }
            insert("inventory_records",mapOf("id" to "road","sicCode" to "SCAP","assetType" to "PUENTE","startPrCode" to "0042","startDistanceM" to "123.4"))
            insert("scap_inspections",mapOf("id" to "inspection","roadRecordId" to "road","status" to "IN_PROGRESS"))
            insert("sic17a_details",mapOf("recordId" to "road","lanes" to "3","deckWidthM" to "8.5"))
            insert("sic17b_details",mapOf("recordId" to "road","wearingSurface" to "3 - Concreto pobre","maximumCapacity" to "12.5","comments" to "HISTÓRICO"))
            insert("photos",mapOf("id" to "photo","recordId" to "road","originalPath" to "/original/historical.jpg","scapInspectionId" to "inspection"))
            old.version=11
        }
        val db=Room.databaseBuilder(context,InventoryDatabase::class.java,name).addMigrations(InventoryDatabase.MIGRATION_11_12,InventoryDatabase.MIGRATION_12_13, InventoryDatabase.MIGRATION_13_14,InventoryDatabase.MIGRATION_14_15).addMigrations(*allInventoryMigrations()).build()
        try {
            val s=db.scapDao().snapshot("inspection")!!
            assertEquals("0042",s.roadRecord!!.startPrCode);assertEquals(123.4,s.roadRecord!!.startDistanceM,0.0)
            assertEquals("3",s.sic17a!!.lanes);assertEquals("8.5",s.sic17a!!.deckWidthM)
            assertEquals("12.5",s.sic17b!!.maximumCapacity);assertEquals("3 - Concreto pobre",s.sic17b!!.wearingSurface)
            assertEquals("/original/historical.jpg",s.photos.single().originalPath)
            repo(db).setSicField("inspection","SIC-17B","beams","4")
            val updated=db.scapDao().snapshot("inspection")!!
            assertEquals("HISTÓRICO",updated.sic17b!!.comments);assertEquals("12.5",updated.sic17b!!.maximumCapacity)
            assertEquals("3",ScapToSicMapper.mapToSic17B(updated).fields.single{it.sicField=="wearingSurface"}.value)
            assertEquals(15,db.openHelper.readableDatabase.version)
        } finally {db.close();context.deleteDatabase(name)}
    }

    @Test fun dynamicWarningsRejectInvalidNumbersAndDoNotBlameOperatorForUnresolvedCapacity()=runBlocking {
        val db=db()
        try {
            val r=repo(db);val id=r.create("Inspector","device",null)
            val before=BridgeSicContract.assess(ScapToSicMapper.mapToSic17A(r.dao.snapshot(id)!!))
            r.setSicField(id,"SIC-17A","lanes","2")
            var preview=ScapSicExporter.previews(r.dao.snapshot(id)!!).single{it.format=="SIC-17A"}
            assertFalse(preview.missingRequired.contains("Número de carriles"))
            assertEquals(before.missing.size-1,BridgeSicContract.assess(ScapToSicMapper.mapToSic17A(r.dao.snapshot(id)!!)).missing.size)
            for(value in listOf("0","-1","1.5")) {
                r.setSicField(id,"SIC-17A","lanes",value)
                preview=ScapSicExporter.previews(r.dao.snapshot(id)!!).single{it.format=="SIC-17A"}
                assertTrue(preview.missingRequired.contains("Número de carriles"))
            }
            val b=ScapSicExporter.previews(r.dao.snapshot(id)!!).single{it.format=="SIC-17B"}
            assertFalse((b.missingRequired+b.missingOptional).any{it.contains("Capacidad")})
            assertTrue(b.pending.any{it.contains("pendiente de definición contractual")})
        } finally {db.close()}
    }
}
