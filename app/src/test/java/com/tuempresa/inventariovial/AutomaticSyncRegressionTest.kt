package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.repository.InventoryRepository
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.export.*
import com.tuempresa.inventariovial.supplementary.*
import com.tuempresa.inventariovial.export.*
import kotlinx.coroutines.runBlocking
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AutomaticSyncRegressionTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private val root=File("../docs/automatic-sync-v16/examples").apply {mkdirs()}
    private fun db()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    private fun record(id:String="bridge-A",revision:Long=1,status:String="ACTIVE")=InventoryRecordEntity(
        id,"SIC-17","BRIDGE","PE-22A","CD","0005",100.0,"0005",125.5,"D",-12.0,-77.0,100.0,3.0,
        "11/10/2026","DATOS SINTETICOS",status,"PENDING","PENDING",1,revision,axisMeasureM=100.0)
    private fun detail(id:String="bridge-A",name:String="RIO A")=Sic17Entity(id,"01","2","GBO-${id.removePrefix("bridge-")}","S",3,25.5,4.25,"1","2","1","1",name,0.0)
    private suspend fun save(db:InventoryDatabase,id:String="bridge-A",revision:Long=1,status:String="ACTIVE")=InventoryRepository(db).saveSic17(record(id,revision,status),detail(id),emptyList())
    @Test fun validatedSicQueuesInSameLocalTransactionWithoutNetwork()=runBlocking {db().use {d->save(d);assertEquals("QUEUED",d.automaticSyncDao().get("SIC/bridge-A")!!.state);assertEquals("ACTIVE",d.inventoryDao().recordById("bridge-A")!!.status)}}
    @Test fun draftSicCreatesNoDefinitiveQueue()=runBlocking {db().use {d->save(d,status="DRAFT");assertNull(d.automaticSyncDao().get("SIC/bridge-A"))}}
    @Test fun duplicateClosureSchedulesOneStableRevision()=runBlocking {db().use {d->save(d);AutomaticSync.request(d,"SIC","bridge-A",1);assertEquals(1,d.automaticSyncDao().all().size);assertEquals(1L,d.automaticSyncDao().all().single().revision)}}
    @Test fun outboxSurvivesDatabaseReopen()=runBlocking {
        val name="queue-${System.nanoTime()}"
        fun open()=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        try {open().use {save(it)};open().use {assertEquals("QUEUED",it.automaticSyncDao().all().single().state);assertEquals(1L,it.automaticSyncDao().all().single().revision)}}finally {context.deleteDatabase(name)}
    }
    @Test fun editRetainsIdentityAndPreviousAck()=runBlocking {db().use {d->save(d);d.automaticSyncDao().acknowledge("SIC/bridge-A",1,10,"{\"folderId\":\"stable\"}");AutomaticSync.request(d,"SIC","bridge-A",2);val i=d.automaticSyncDao().all().single();assertEquals("bridge-A",i.recordId);assertEquals("UPDATE_PENDING",i.state);assertEquals(1L,i.syncedRevision);assertEquals(2L,i.revision);assertTrue(i.receiptJson!!.contains("stable"))}}
    @Test fun staleAckCannotHideNewerEdit()=runBlocking {db().use {d->save(d);AutomaticSync.request(d,"SIC","bridge-A",2);assertEquals(0,d.automaticSyncDao().acknowledge("SIC/bridge-A",1,20,"{}"));assertEquals(0L,d.automaticSyncDao().all().single().syncedRevision)}}
    @Test fun draftAfterSyncKeepsPreviousReceiptAndRequiresClosure()=runBlocking {db().use {d->save(d);d.automaticSyncDao().acknowledge("SIC/bridge-A",1,10,"{}");AutomaticSync.draft(d,"SIC","bridge-A");assertEquals("UPDATE_PENDING",d.automaticSyncDao().all().single().state);assertEquals("Actualización pendiente · requiere cierre",SyncIntentPresentation.label(d.automaticSyncDao().all().single(),false,true,true))}}
    @Test fun rolledBackValidationCannotLeaveQueuedRecord()=runBlocking {db().use {d->try {d.withTransaction {save(d);error("synthetic commit failure")}}catch(_:IllegalStateException){};assertNull(d.inventoryDao().recordById("bridge-A"));assertTrue(d.automaticSyncDao().all().isEmpty())}}
    @Test fun sourceRevisionGuardRejectsConcurrentStateChanges()=runBlocking {db().use {d->save(d);AutomaticSync.request(d,"SIC","bridge-A",2);assertEquals(0,d.automaticSyncDao().state("SIC/bridge-A",1,"ERROR","old"));assertNull(d.automaticSyncDao().all().single().error)}}
    @Test fun localExportDoesNotAcknowledgeDrive()=runBlocking {db().use {d->save(d);val out=ByteArrayOutputStream();SicExcelWriter.write(out,d.inventoryDao().recordsForExport(null,null),SicExportFormat.SIC17,ExportHeading());assertTrue(out.size()>0);assertEquals(0L,d.automaticSyncDao().all().single().syncedRevision)}}
    @Test fun presentationCoversOfflineErrorsAndConfigurations() {
        val i=DriveSyncIntent("SIC/A","SIC","A",1)
        assertEquals("Pendiente de conexión",SyncIntentPresentation.label(i,true,false,true))
        assertEquals("Sin configurar",SyncIntentPresentation.label(i,true,true,false))
        assertEquals("Error",SyncIntentPresentation.label(i.copy(state="ERROR"),true,true,true))
        assertEquals("Bloqueado",SyncIntentPresentation.label(i.copy(state="BLOCKED"),true,true,true))
        assertEquals("Pendiente de validación/ordenamiento",SyncIntentPresentation.label(i.copy(state="PENDING_VALIDATION"),true,true,true))
        assertEquals("Sincronizado",SyncIntentPresentation.label(i.copy(state="SYNCED",syncedRevision=1),true,true,true))
        assertEquals("Subiendo",SyncIntentPresentation.label(i.copy(state="UPLOADING"),true,true,true))
    }
    @Test fun pngBecomesRealJpegWithoutChangingOriginal() {
        val original=File(context.cacheDir,"import.png");val b=Bitmap.createBitmap(48,32,Bitmap.Config.ARGB_8888)
        original.outputStream().use {assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,it))};b.recycle()
        val bytes=original.readBytes();val target=File(context.cacheDir,"001_F01.jpg");DeliveryJpeg.copy(original,target)
        assertArrayEquals(bytes,original.readBytes());assertEquals(255,target.readBytes()[0].toInt() and 255);assertNotNull(BitmapFactory.decodeFile(target.path))
        val options=BitmapFactory.Options().apply {inJustDecodeBounds=true};BitmapFactory.decodeFile(target.path,options);assertEquals("image/jpeg",options.outMimeType)
    }
    @Test fun jpegOrientationAndPixelsRemainPreserved() {
        val original=File(context.cacheDir,"original.jpg");val b=Bitmap.createBitmap(32,48,Bitmap.Config.ARGB_8888)
        original.outputStream().use {assertTrue(b.compress(Bitmap.CompressFormat.JPEG,95,it))};b.recycle()
        ExifInterface(original.path).apply {setAttribute("Orientation","6");saveAttributes()}
        val bytes=original.readBytes();val target=File(context.cacheDir,"orientation.jpg");DeliveryJpeg.copy(original,target)
        assertArrayEquals(bytes,target.readBytes());assertEquals(6,ExifInterface(target.path).getAttributeInt("Orientation",1))
    }
    @Test fun invalidMediaCannotBePreparedAsJpeg() {val bad=File(context.cacheDir,"bad.png").apply {writeText("not an image")};try {DeliveryJpeg.copy(bad,File(context.cacheDir,"bad.jpg"));fail("Must reject unreadable photos")}catch(_:IllegalArgumentException){}}
    @Test fun newScapBothTypesKeepIndependentAAndBAndExactTemplateCells()=runBlocking {
        db().use {d->val repo=ScapRepository(d,ScapCatalog.load(context))
            for(type in ScapBridgeType.entries) {
                val id=repo.create("Inspector","synthetic-device",null,type)
                repo.setField(id,"inspection","singularityName","Tambo")
                val s=d.scapDao().snapshot(id)!!;assertEquals("PUENTE",s.values()["categoryName"]);assertEquals("Nacional",s.values()["routeType"]);assertEquals("Tambo",s.inspection.bridgeName)
                val bytes=ByteArrayOutputStream();ScapDelivery.exporter(context,s).write(bytes,s){File(it).readBytes()}
                val workbook=TemplateWorkbook(bytes.toByteArray().inputStream());assertEquals("PUENTE",workbook.cell(1,"J10").textContent);assertEquals("Tambo",workbook.cell(1,"E30").textContent);assertEquals("Nacional",workbook.cell(1,"P20").textContent)
                File(root,"SCAP_${type.label}_Tambo.xlsx").writeBytes(bytes.toByteArray())
            }
        }
    }
    @Test fun historicalScapReopenNeverAddsNewDefaults()=runBlocking {db().use {d->val repo=ScapRepository(d,ScapCatalog.load(context));val id=repo.create("Inspector","d",null,ScapBridgeType.SLAB)
        d.scapDao().deleteValue(id,"inspection","categoryName");d.scapDao().putValue(ScapFieldValueEntity(id,"inspection","bridgeName","HISTORICO A","MANUAL"));repo.setField(id,"inspection","routeType","Vecinal")
        repo.reopen(id);val s=d.scapDao().snapshot(id)!!;assertNull(s.values()["categoryName"]);assertEquals("Vecinal",s.values()["routeType"]);assertEquals("HISTORICO A",s.values()["bridgeName"])
    }}
    @Test fun unconfirmedScapWarningsCannotQueue()=runBlocking {db().use {d->val repo=ScapRepository(d,ScapCatalog.load(context));val id=repo.create("Inspector","d",null,ScapBridgeType.SLAB);val before=d.scapDao().inspection(id)!!.status;try {repo.complete(id);fail("Must require warning consent")}catch(_:IllegalArgumentException){};assertTrue(d.automaticSyncDao().all().isEmpty());assertEquals(before,d.scapDao().inspection(id)!!.status)}}
    @Test fun confirmedScapClosurePersistsConsent()=runBlocking {db().use {d->val repo=ScapRepository(d,ScapCatalog.load(context));val id=repo.create("Inspector","d",null,ScapBridgeType.MODULAR);repo.complete(id,true);val i=d.automaticSyncDao().all().single();assertTrue(i.warningsConfirmed);assertEquals("SCAP",i.kind);assertEquals("COMPLETE",d.scapDao().inspection(id)!!.status)}}
    @Test fun sicProjectionDoesNotInventRiverFromNewBridgeName()=runBlocking {db().use {d->val repo=ScapRepository(d,ScapCatalog.load(context));val id=repo.create("Inspector","d",null,ScapBridgeType.SLAB);repo.setField(id,"inspection","singularityName","Tambo");val map=ScapToSicMapper.mapToSic17(d.scapDao().snapshot(id)!!);assertFalse(map.fields.any {it.sicField=="singularityName" && it.value=="Tambo"})}}
    @Test fun actualApplicationExportsCreateTraceableFourBridgeFixturesAndRouteSheets()=runBlocking {db().use {d->
        val all=mutableListOf<Pair<String,ByteArray>>()
        for((index,letter) in listOf("A","B","C","D","B_EDIT","OTHER").withIndex()) {
            val id="bridge-${if(letter=="B_EDIT") "B" else letter}";val route=if(letter=="OTHER")"PE-3N" else "PE-22A"
            val r=record(id).copy(routeCode=route,axisMeasureM=(index+1)*100.0)
            val prior=d.inventoryDao().recordById(id);if(prior==null)d.inventoryDao().insertRecord(r) else d.inventoryDao().updateRecord(r)
            val det=detail(id,"RIO $letter");if(prior==null)d.inventoryDao().insertSic17(det) else d.inventoryDao().updateSic17(det)
            val dir=File(root,"inputs/$letter").apply {mkdirs()}
            val out=ByteArrayOutputStream();SicExcelWriter.write(out,listOf(InventoryRecordForExport(r,sic17=det)),SicExportFormat.SIC17,ExportHeading())
            File(dir,"SIC-17.xlsx").writeBytes(out.toByteArray());if(letter !in setOf("B","D"))all.add(route to out.toByteArray())
            val rows=JSONObject().put("synthetic",true).put("recordId",id).put("bridgeCode",det.bridgeCode).put("river",det.singularityName).put("route",route)
            for(format in listOf("SIC-17A","SIC-17B")) {
                val values=BridgeSicContract.fields(format).associate {f->f.key to when(f.kind) {
                    FieldKind.TEXT->if(f.key=="bridgeCode")det.bridgeCode else if(f.key=="bridgeName")"PUENTE $letter" else "DATOS $letter"
                    FieldKind.YEAR->"2020";FieldKind.LATITUDE->"-12.0";FieldKind.LONGITUDE->"-77.0"
                    FieldKind.CODE,FieldKind.DESCRIBED_CODE->f.options.first().substringBefore(" - ")
                    else->"2"
                }}
                val mapping=ScapSicMapping(format,id,values.filterKeys {it !in BridgeSicContract.unresolved(format)}.map {(k,v)->ScapMappedField("MANUAL",k,v.orEmpty(),ScapMappingStatus.DIRECT)},emptyList())
                val preview=ScapSicExporter.preview(mapping);assertTrue("$letter $format ${preview.missingRequired}",preview.missingRequired.isEmpty())
                val bytes=ByteArrayOutputStream();SicExcelWriter.writeMappedRow(bytes,format,route,preview.columns,preview.values,if(format=="SIC-17A")setOf(7,8) else emptySet());File(dir,"$format.xlsx").writeBytes(bytes.toByteArray())
            }
            File(dir,"fixture.json").writeText(rows.toString(2))
        }
        val consolidated=ByteArrayOutputStream();SicConsolidatedWorkbook.write(consolidated,all);File(root,"SIC-17-local-routes.xlsx").writeBytes(consolidated.toByteArray())
        val book=TemplateWorkbook(consolidated.toByteArray().inputStream());assertEquals(listOf("PE-22A","PE-3N"),book.document("xl/workbook.xml").nodes("sheet").map {it.getAttribute("name")})
        assertTrue(book.sheet(1).nodes("row").count {it.getAttribute("r").toInt()>=14}==3)
    }}
    @Test @Config(sdk=[34]) fun migrationChainKeepsHistoricalColumns() {
        val migrations=arrayOf(InventoryDatabase.MIGRATION_1_2,InventoryDatabase.MIGRATION_2_3,InventoryDatabase.MIGRATION_3_4,
            InventoryDatabase.MIGRATION_4_5,InventoryDatabase.MIGRATION_5_6,InventoryDatabase.MIGRATION_6_7,InventoryDatabase.MIGRATION_7_8,
            InventoryDatabase.MIGRATION_8_9,InventoryDatabase.MIGRATION_9_10,InventoryDatabase.MIGRATION_10_11,InventoryDatabase.MIGRATION_11_12,
            InventoryDatabase.MIGRATION_12_13,InventoryDatabase.MIGRATION_13_14,InventoryDatabase.MIGRATION_14_15,InventoryDatabase.MIGRATION_15_16)
        val evidence=JSONArray()
        for(version in 4..15) {
            val name="audit-m$version-${System.nanoTime()}"
            val file=context.getDatabasePath(name).also{it.parentFile!!.mkdirs()}
            val entities=JSONObject(File("schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/$version.json").readText())
                .getJSONObject("database").getJSONArray("entities")
            val expected=linkedMapOf<String,Map<String,String?>>()
            android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file,null).use{old->
                for(i in 0 until entities.length()) {
                    val e=entities.getJSONObject(i);val table=e.getString("tableName")
                    old.execSQL(e.getString("createSql").replace("\u0024{TABLE_NAME}",table))
                    val indices=e.optJSONArray("indices") ?: JSONArray()
                    for(j in 0 until indices.length())old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\u0024{TABLE_NAME}",table))
                    val values=android.content.ContentValues();val fields=e.getJSONArray("fields")
                    for(j in 0 until fields.length()) {
                        val f=fields.getJSONObject(j);val column=f.getString("columnName")
                        when(f.getString("affinity")){"INTEGER"->values.put(column,1L);"REAL"->values.put(column,1.25);else->values.put(column,"keep")}
                    }
                    if(table=="user_cache")values.put("role","OPERATOR")
                    if(table=="pavement_damages")values.put("severity",if(version<14)"A" else "H")
                    if(table=="pavement_samples"){
                        values.put("surface","RIGID");values.put("anchoM",5.0);values.put("largoM",40.0)
                        values.put("slabRows",10);values.put("slabColumns",2);values.put("status","CLOSED")
                        values.put("calculationSnapshot","{\"immutable\":true}")
                    }
                    val names=values.keySet().toList()
                    old.execSQL("INSERT INTO `$table` (${names.joinToString{"`$it`"}}) VALUES (${names.joinToString{"?"}})",names.map{values.get(it)}.toTypedArray())
                    old.rawQuery("SELECT * FROM `$table`",null).use{c->assertTrue(c.moveToFirst());expected[table]=c.columnNames.associateWith{c.getString(c.getColumnIndexOrThrow(it))}}
                }
                old.version=version
            }
            fun open()=Room.databaseBuilder(context,InventoryDatabase::class.java,name).addMigrations(*migrations).addMigrations(*allInventoryMigrations()).allowMainThreadQueries().build()
            try {
                val room=open()
                try {val db=room.openHelper.writableDatabase;assertEquals(18,db.version)
                    for((table,columns) in expected)db.query("SELECT * FROM `$table`").use{c->
                        assertTrue("Lost table $version/$table",c.moveToFirst())
                        for((column,value) in columns)assertEquals("Lost value $version/$table.$column",
                            if(table=="pavement_damages" && column=="severity")"H" else value,c.getString(c.getColumnIndexOrThrow(column)))
                        assertFalse("Unexpected duplicate $version/$table",c.moveToNext())
                    }
                }finally{room.close()}
                val reopened=open();try{assertEquals(18,reopened.openHelper.readableDatabase.version)}finally{reopened.close()}
                evidence.put(JSONObject().put("fromVersion",version).put("toVersion",16).put("tables",expected.size)
                    .put("columnsVerified",expected.values.sumOf{it.size}).put("result","PASS_LOCAL_SQLITE"))
            }finally{context.deleteDatabase(name)}
        }
        File(root,"migration-chain-to16.json").writeText(JSONObject().put("cases",evidence).put("physicalUpgrade","NO PROBADO")
            .put("versions1to3","Original schema files unavailable; no claim of coverage").toString(2))
    }


}
