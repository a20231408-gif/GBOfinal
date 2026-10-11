package com.tuempresa.inventariovial

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.delivery.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.export.*
import com.tuempresa.inventariovial.sic.*
import com.tuempresa.inventariovial.supplementary.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Audit only: synthetic SQLite fixtures and real exporters; never performs network writes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AcceptanceDeliveryAuditTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private val root=File("../docs/acceptance-audit-v15-2026-10-10/scenarios").apply{mkdirs()}
    private fun folder(name:String)=File(root,name).apply{mkdirs()}
    private fun cleanPhoto(dir:File,name:String):File=File(dir,name).apply {
        val bitmap=Bitmap.createBitmap(480,320,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.rgb(48,125,89))}
        outputStream().use{assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG,95,it))};bitmap.recycle()
    }
    private fun note(dir:File,data:JSONObject)=File(dir,"fixture.json").writeText(data.put("synthetic",true)
        .put("physicalTablet","NO PROBADO").put("realUpload","NO PROBADO").toString(2))

    @Test @Config(sdk=[34]) fun migrationChainKeepsHistoricalColumns() {
        val migrations=arrayOf(InventoryDatabase.MIGRATION_1_2,InventoryDatabase.MIGRATION_2_3,InventoryDatabase.MIGRATION_3_4,
            InventoryDatabase.MIGRATION_4_5,InventoryDatabase.MIGRATION_5_6,InventoryDatabase.MIGRATION_6_7,InventoryDatabase.MIGRATION_7_8,
            InventoryDatabase.MIGRATION_8_9,InventoryDatabase.MIGRATION_9_10,InventoryDatabase.MIGRATION_10_11,InventoryDatabase.MIGRATION_11_12,
            InventoryDatabase.MIGRATION_12_13,InventoryDatabase.MIGRATION_13_14,InventoryDatabase.MIGRATION_14_15)
        val evidence=JSONArray()
        for(version in 4..14) {
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
            fun open()=Room.databaseBuilder(context,InventoryDatabase::class.java,name).addMigrations(*migrations).allowMainThreadQueries().build()
            try {
                val room=open()
                try {val db=room.openHelper.writableDatabase;assertEquals(15,db.version)
                    for((table,columns) in expected)db.query("SELECT * FROM `$table`").use{c->
                        assertTrue("Lost table $version/$table",c.moveToFirst())
                        for((column,value) in columns)assertEquals("Lost value $version/$table.$column",
                            if(table=="pavement_damages" && column=="severity")"H" else value,c.getString(c.getColumnIndexOrThrow(column)))
                        assertFalse("Unexpected duplicate $version/$table",c.moveToNext())
                    }
                }finally{room.close()}
                val reopened=open();try{assertEquals(15,reopened.openHelper.readableDatabase.version)}finally{reopened.close()}
                evidence.put(JSONObject().put("fromVersion",version).put("toVersion",15).put("tables",expected.size)
                    .put("columnsVerified",expected.values.sumOf{it.size}).put("result","PASS_LOCAL_SQLITE"))
            }finally{context.deleteDatabase(name)}
        }
        File(root,"migration-chain.json").writeText(JSONObject().put("cases",evidence).put("physicalUpgrade","NO PROBADO")
            .put("versions1to3","Original schema files unavailable; no claim of coverage").toString(2))
    }

    @Test fun actualSicRoomDeliveryFilesAndAllFormats()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        try {
            @Suppress("UNCHECKED_CAST")
            val fixtures=SicExcelWriterTest::class.java.getDeclaredMethod("fixtures").apply{isAccessible=true}
                .invoke(SicExcelWriterTest()) as List<InventoryRecordForExport>
            for(row in fixtures.map{it.copy(sic20=it.sic20?.copy(classCode="12"))}) {
                val r=row.record.copy(routeCode="PE-22A",latitude=0.0,longitude=0.005,observations="AUDITORIA SINTETICA",assetType=when(row.record.sicCode){"SIC-18"->"CULVERT";"SIC-17"->"BRIDGE";"SIC-20"->"FORD";"SIC-21"->"HORIZONTAL_MARKS";else->row.record.assetType})
                val dao=db.inventoryDao();dao.insertRecord(r)
                row.sic17?.let{dao.insertSic17(it)};row.sic18?.let{
                    val parent=it.copy(structuralConditionCode="3",functionalConditionCode="3")
                    dao.insertSic18(parent)
                    val values=SupplementaryForms.fields(SupplementaryFormat.SIC18A).associate{f->f.key to (f.options.firstOrNull()?.substringBefore(" - ") ?: "2")}
                    dao.saveSic18A(Sic18AEntity.from(r.id,values+Sic18AStatus.inherited(parent,null)))
                }
                row.sic19?.let{dao.insertSic19(it)};row.sic20?.let{dao.insertSic20(it)}
                row.sic21?.let{dao.insertSic21(it)};row.sic22?.let{dao.insertSic22(it)};row.sic23?.let{dao.insertSic23(it)}
                val dir=folder(when(r.sicCode){"SIC-18"->"01-alcantarilla";"SIC-20"->"02-baden";"SIC-21","SIC-22"->"03-senales";else->"09-misma-ruta-sib"})
                val categories=if(r.sicCode=="SIC-18")listOf("CULVERT_PANORAMIC","CULVERT_INLET","CULVERT_OUTLET","CULVERT_PANORAMIC")else listOf("GENERAL")
                categories.forEachIndexed{i,category->
                    val photo=cleanPhoto(dir,"${r.sicCode}-${i+1}-original.jpg")
                    dao.insertPhoto(PhotoEntity("audit-${r.id}-$i",r.id,i+1,photo.absolutePath,i==0,null,null,null,"PENDING",1791580800000,
                        photoCategory=category,photoLatitude=r.latitude,photoLongitude=r.longitude,photoGpsAccuracyM=2.0))
                }
                assertEquals(r,dao.recordById(r.id))
                val input=JSONObject().put("route",r.routeCode).put("record",r.toString()).put("sicDetails",row.copy(record=r).toString()).put("photoCategories",JSONArray(categories))
                note(dir,input);File(dir,"data-${r.sicCode}.json").writeText(input.toString(2))
            }
            val repo=SicFieldRepository(db)
            for(f in SicFieldFormat.entries.filter{it!=SicFieldFormat.SIC18A}) {
                val state=SicFieldState(f,mapOf("routeCode" to "PE-22A","roadbedCode" to "CD","faja" to "2","startPrCode" to "0005","startDistanceM" to "12.5",
                    "endPrCode" to "0006","endDistanceM" to "20.0","surveyDate" to "10/10/2026","typeCode" to "1","directionCode" to "C","sideCode" to "D",
                    "classCode" to "15","usefulWidthM" to "3.5","totalWidthM" to "4.5","levelCode" to "2","description" to "AUDITORIA SINTETICA"))
                repo.save("audit-${f.code}",state,"AUDITORIA",true)
                assertEquals(state,repo.load("audit-${f.code}",f))
            }
            val rows=db.inventoryDao().recordsForExport(null,"PE-22A")
            val inventory=JSONArray()
            for(format in SicExportFormat.entries) {
                val file=File(folder("all-sic"),"${format.code}.xlsx")
                file.outputStream().use{SicExcelWriter.write(it,rows,format,ExportHeading("AUDITORIA","DATOS SINTETICOS","TRAMO PRUEBA"))}
                inventory.put(JSONObject().put("code",format.code).put("columns",JSONArray(format.columns.map{it.label})).put("columnCount",format.columns.size)
                    .put("matchingRecords",rows.count{format.appliesTo(it)}).put("artifact",file.name))
            }
            File(root,"format-inventory.json").writeText(JSONObject().put("sic",inventory).put("bridgeSupplements",JSONArray(listOf(SupplementaryFormat.SIC17A,SupplementaryFormat.SIC17B).map{f->
                JSONObject().put("format",f.title).put("standaloneFlagEnabled",f.enabled).put("scapProjectionImplemented",true).put("fields",JSONArray(SupplementaryForms.fields(f).map{it.key}))}))
                .put("pavementSic",JSONArray(PavementSicFormat.entries.map{it.label}))
                .put("surfaces",JSONArray(PavementSurface.entries.map{it.name})).put("scap",JSONArray(ScapBridgeType.entries.map{it.label})).toString(2))
            val reference=testAxis().let{a->a.copy(routes=listOf(RoadRoute("PE-22A")),segments=a.segments.map{s->s.copy(routeCode="PE-22A",points=s.points.map{it.copy(routeCode="PE-22A")})},prs=a.prs.map{it.copy(routeCode="PE-22A")})}
            val prepared=RouteDelivery(context,db).prepare("PE-22A",reference,MatchConfig())
            val manifest=JSONObject(File(prepared.batch.manifestPath).readText())
            val destination=folder("09-misma-ruta-sib")
            File(destination,"manifest.local.json").writeText(manifest.toString(2))
            val entries=manifest.getJSONArray("files")
            for(i in 0 until entries.length()) {val e=entries.getJSONObject(i);File(File(prepared.batch.manifestPath).parentFile,e.getString("localFile")).copyTo(File(destination,e.getString("localFile")),true)}
            // Demonstrate whether the current ACK validator accepts a receipt for the wrong path.
            db.inventoryDao().deliveryStatus(prepared.batch.id,"QUEUED",null)
            val transport=object:DeliveryTransport {
                override fun supportsDelivery()=true
                override fun post(payload:JSONObject)=JSONObject().put("ok",true).put("protocol",1).put("deliveryId",payload.getString("deliveryId"))
                    .put("fileName",payload.getString("fileName")).put("sha256",payload.getString("sha256")).put("fileId","synthetic-file")
                    .put("folderId","synthetic-wrong-folder").put("pathVersion",1).put("logicalPath","WRONG/PATH")
            }
            DeliverySync.sync(db,prepared.batch.id,transport)
            val observed=db.inventoryDao().delivery(prepared.batch.id)!!.status
            File(root,"wrong-path-ack.json").writeText(JSONObject().put("syntheticContractProbe",true).put("observedStatus",observed)
                .put("acceptance","FAIL").put("reason","Receipt has wrong logicalPath/pathVersion; server is synthetic, no real upload").toString(2))
            assertEquals("Current implementation behavior, not acceptance approval","SYNCED",observed)
            val other=fixtures.first().record.copy(id="audit-route2",routeCode="PE-3N")
            db.inventoryDao().insertRecord(other)
            db.inventoryDao().insertSic17(fixtures.first().sic17!!.copy(recordId=other.id))
            val second=db.inventoryDao().recordsForExport(null,"PE-3N")
            File(folder("10-dos-rutas"),"PE-3N-SIC-17.xlsx").outputStream().use{SicExcelWriter.write(it,second,SicExportFormat.SIC17,ExportHeading("AUDITORIA","DATOS SINTETICOS","DOS RUTAS"))}
            note(folder("10-dos-rutas"),JSONObject().put("routes",JSONArray(listOf("PE-22A","PE-3N"))).put("route2Rows",second.size))
        } finally {db.close()}
    }

    @Test fun actualCanonicalScapRoomExcelAndBundles()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        try {for(type in ScapBridgeType.entries) {
            val s0=ScapCanonicalExportTest::class.java.getDeclaredMethod("snapshot",ScapBridgeType::class.java).apply{isAccessible=true}
                .invoke(ScapCanonicalExportTest(),type) as ScapInspectionSnapshot
            val dir=folder(if(type==ScapBridgeType.SLAB)"04-puente-losa" else "05-puente-modular")
            val id=if(type==ScapBridgeType.SLAB)"12345678-1234-1234-1234-123456789010" else "12345678-1234-1234-1234-123456789011"
            val road="audit-road-${type.name}"; val photo=cleanPhoto(dir,"original.jpg")
            val s=s0.copy(inspection=s0.inspection.copy(id=id,roadRecordId=road,bridgeName="AUDITORIA ${type.label}"),values=s0.values.map{it.copy(inspectionId=id)},
                elements=s0.elements.map{it.copy(element=it.element.copy(id="manual-${type.name}",inspectionId=id),condition=it.condition?.copy(id="condition-${type.name}",scapElementId="manual-${type.name}"))},
                photos=listOf(PhotoEntity("audit-photo-${type.name}",road,1,photo.absolutePath,true,null,null,null,"PENDING",1791580800000,scapInspectionId=id,photoCategory="GENERAL",description="Fotografia sintetica limpia",
                    capturedAt=1791580800000,photoLatitude=-12.75,photoLongitude=-77.5,photoGpsAccuracyM=2.0,photoGnssProvider="SYNTHETIC_AUDIT",photoLocationSource="MANUAL")),
                sketches=ScapRepository.SKETCH_TYPES.map{kind->ScapSketchEntity("audit-$id-$kind",id,kind,photo.absolutePath)})
            db.inventoryDao().insertRecord(testRecord(road,sic="SIC-17",route="PE-28H"))
            val dao=db.scapDao();dao.insertInspection(s.inspection);s.values.forEach{dao.putValue(it)}
            s.elements.forEach{dao.putElement(it.element);it.condition?.let{c->dao.putCondition(c)}};s.photos.forEach{db.inventoryDao().insertPhoto(it)};s.sketches.forEach{dao.insertSketch(it)}
            val persisted=dao.snapshot(id)!!;assertEquals(s.values(),persisted.values())
            ScapDelivery.writeExcel(context,persisted,File(dir,"SCAP_${type.label}.xlsx"))
            ScapDelivery.bundle(context,persisted).copyTo(File(dir,"SCAP_${type.label}.zip"),true)
            note(dir,JSONObject().put("inspection",persisted.inspection.toString()).put("values",JSONObject(persisted.values())).put("completion","INCOMPLETE")
                .put("bundleVersion",ScapDelivery.SCAP_BUNDLE_VERSION).put("reviewErrors",JSONArray(ScapDelivery.exporter(context,persisted).review(persisted).errors)))
        }} finally{db.close()}
    }

    @Test fun actualPavementRoomExcelPdfAndSicFiles()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        try {for(surface in PavementSurface.entries) {
            val dir=folder(when(surface){PavementSurface.ASPHALT->"06-asfaltico";PavementSurface.RIGID->"07-rigido";else->"08-no-pavimentado"})
            val id="audit-${surface.name}";val def=PavementCatalog.forSurface(surface).first()
            var sample=PavementSampleEntity(id,"TRAMO_AUDITORIA","PE-22A",1,"Derecho","CD","001+000","001+046",5.0,46.0,surface.name,"2026-10-10","AUDITORIA","Auditor sintético",slabRows=10,slabColumns=2,laneWidthM=5.0,slabWidthM=2.5,slabLengthM=4.6)
            val damage=PavementDamageEntity("damage-$id",id,1,surface.name,def.code,def.name,"M",def.geometry.name,1.0,10.0,3.0,15.0,null,2.0,5.0,10.0,null,2.0,"AUDITORIA SINTETICA",slabs="1,2")
            db.pavementDao().saveSample(sample);db.pavementDao().saveDamage(damage)
            assertEquals(damage,db.pavementDao().damages(id).single())
            val result=PavementCalculator(context).calculate(sample,listOf(damage))
            sample=sample.copy(status="CLOSED",calculationSnapshot=PavementResultJson.encode(result));db.pavementDao().saveSample(sample)
            assertEquals(sample,db.pavementDao().sample(id))
            val data=PavementExportSample(sample,listOf(damage),result)
            val code=when(surface){PavementSurface.ASPHALT->"ASF";PavementSurface.RIGID->"RIG";else->"NPV"}
            File(dir,"PAV_${code}.xlsx").outputStream().use{PavementExcel.write(it,listOf(data))}
            val original=cleanPhoto(dir,"original.jpg")
            val pvPhoto=PavementDamagePhotoEntity("photo-$id",damage.id,original.absolutePath,1791580800000)
            db.pavementDao().savePhoto(pvPhoto)
            val photos=mapOf(damage.id to listOf(pvPhoto))
            val native=PavementDriveUploader::class.java.getDeclaredMethod("nativeJson",PavementSampleEntity::class.java,List::class.java,PavementResult::class.java,Map::class.java).apply{isAccessible=true}
                .invoke(PavementDriveUploader,sample,listOf(damage),result,photos) as JSONObject
            File(dir,"M_001.json").writeText(native.toString(2))
            val pdfError=runCatching{File(dir,"HC_001.pdf").outputStream().use{PavementPdf.write(it,data,photos)}}.exceptionOrNull()?.javaClass?.simpleName
            File(dir,"calculation.json").writeText(PavementResultJson.encode(result))
            val statuses=JSONArray()
            for(f in PavementSicFormat.entries.filter{it.surface==surface.name}) {
                val manual=mapOf("faja" to "2","prInicio" to "0001","distanciaInicio" to "0","prFin" to "0001","distanciaFin" to "46","tipo" to "1","longitud" to "5","ancho" to "2","gravedad" to "2")
                val withSic=sample.copy(sicDataJson=PavementSic.update(sample,f,damage.id,manual))
                val status=PavementSic.status(withSic,listOf(damage),f)
                if(status.complete)File(dir,"${f.label}.xlsx").outputStream().use{PavementSic.write(it,withSic,listOf(damage),f)}
                statuses.put(JSONObject().put("format",f.label).put("complete",status.complete).put("missing",status.missingCount))
            }
            note(dir,JSONObject().put("sample",sample.toString()).put("damage",damage.toString()).put("calculation",JSONObject(PavementResultJson.encode(result)))
                .put("pdfException",pdfError).put("sic",statuses))
        }}finally{db.close()}
    }

    @Test fun importedPngBytesRemainPngUnderJpegDeliveryName()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        try {
            val dir=folder("photo-png-contract-probe")
            val imported=File(dir,"imported-as-jpg.jpg")
            val bitmap=Bitmap.createBitmap(80,60,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.BLUE)}
            imported.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
            val record=testRecord("audit-png",sic="SIC-18").copy(assetType="CULVERT")
            db.inventoryDao().insertRecord(record)
            db.inventoryDao().insertSic18(Sic18Entity(record.id,"06","1",1,"1",1.0,null,"1","1"))
            db.inventoryDao().insertPhoto(PhotoEntity("audit-png-photo",record.id,1,imported.absolutePath,true,null,null,null,"PENDING",1791580800000,photoCategory="CULVERT_PANORAMIC"))
            val prepared=RouteDelivery(context,db).prepare("R",testAxis(),MatchConfig())
            val manifest=JSONObject(File(prepared.batch.manifestPath).readText());val entries=manifest.getJSONArray("files")
            val entry=(0 until entries.length()).map{entries.getJSONObject(it)}.single{it.getJSONObject("payload").getString("kind")=="photo"}
            val payload=entry.getJSONObject("payload");val source=File(File(prepared.batch.manifestPath).parentFile,entry.getString("localFile"))
            val delivered=File(dir,payload.getString("fileName"));source.copyTo(delivered,true)
            assertArrayEquals(imported.readBytes(),delivered.readBytes())
            assertEquals(0x89,delivered.readBytes().first().toInt() and 255)
            File(dir,"contract-probe.json").writeText(JSONObject().put("status","FAIL").put("mimeType",payload.getString("mimeType"))
                .put("fileName",payload.getString("fileName")).put("decodedBytes","PNG").put("realUpload","NO PROBADO").toString(2))
        }finally{db.close()}
    }
}
