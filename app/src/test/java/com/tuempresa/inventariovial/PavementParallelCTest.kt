package com.tuempresa.inventariovial

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.*
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.pavement.ui.PavementDrawing
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.*
import java.time.LocalDate
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class PavementParallelCTest {
    private fun sample()=PavementSampleEntity("s","T","R",1,"Derecho","UC","00+000","00+040",5.0,40.0,"ASPHALT","2026-10-06","u","Inspector")
    private fun rigid()=sample().copy(surface="RIGID",slabRows=10,slabColumns=2,slabWidthM=2.5,slabLengthM=4.0)
    private fun damage(id:String="d",code:Int=7)=PavementDamageEntity(id,"s",1,"ASPHALT",code,"Grieta","L","LINEAR",1.0,2.0,2.0,15.0,null,1.0,13.0,null,null,null,"")
    private fun slab(id:String,code:Int,level:String?,ids:String)=damage(id,code).copy(surface="RIGID",geometry="SLABS",severity=level,slabs=ids)
    private val context get()=ApplicationProvider.getApplicationContext<Context>()

    @Test fun chainageUsesKilometresAndMetresWithoutRoundingAwayDimensions() {
        val inputs=listOf(0.0,35.0,1025.0,12540.0,1025.125)
        assertEquals(listOf("00+000","00+035","01+025","12+540","01+025.125"),inputs.map(PavementChainage::format))
        inputs.forEach { assertEquals(it,PavementChainage.parse(PavementChainage.format(it)),.0005) }
        assertEquals("01+000",PavementChainage.format(999.9999))
    }
    @Test fun chainageRejectsNonFiniteNegativeAndMalformedValues() {
        for(text in listOf("NaN","-1","01+1000","1+2+3","text"))assertTrue(text,runCatching{PavementChainage.parse(text)}.isFailure)
    }
    @Test fun consecutiveSamplesReuseGeometryAndChainEnd() {
        val a=sample().copy(muestra=7)
        val b=PavementHeader.newSample("b",PavementSurface.ASPHALT,"u","Inspector",listOf(a),LocalDate.of(2026,10,7))
        assertEquals(8,b.muestra);assertEquals(a.ruta,b.ruta);assertEquals(a.tramo,b.tramo)
        assertEquals(a.carril,b.carril);assertEquals(a.calzada,b.calzada);assertEquals(a.anchoM,b.anchoM,0.0)
        assertEquals("00+040",b.prInicial);assertEquals("00+080",b.prFinal);assertEquals("2026-10-07",b.fecha)
    }
    @Test fun sequenceIsEditableAndScopedByRoadbedAndSurface() {
        val a=sample().copy(muestra=20)
        assertFalse(PavementHeader.duplicate(a.copy(id="b",muestra=3),listOf(a)))
        assertTrue(PavementHeader.duplicate(a.copy(id="b"),listOf(a)))
        assertFalse(PavementHeader.duplicate(a.copy(id="b",calzada="UD"),listOf(a)))
        assertEquals(1,PavementHeader.nextNumber(a.copy(surface="RIGID"),listOf(a)))
    }
    @Test fun rigidLengthUsesRowsAndRemembersIndividualSlabDimensions() {
        val a=rigid();val b=PavementHeader.newSample("b",PavementSurface.RIGID,"u","Inspector",listOf(a))
        assertEquals(20,PavementHeader.totalSlabs(b));assertEquals(2,b.slabColumns);assertEquals(10,b.slabRows)
        assertEquals(2.5,PavementHeader.slabWidth(b),0.0);assertEquals(4.0,PavementHeader.slabLength(b),0.0)
        val normalized=PavementHeader.normalize(b.copy(slabRows=3,prInicial="1025",largoM=999.0))
        assertEquals(12.0,normalized.largoM,0.0);assertEquals("01+037",normalized.prFinal)
    }
    @Test fun finalRecomputesFromEditableStartAndUnitLength() {
        val a=PavementHeader.normalize(sample().copy(prInicial="35",largoM=22.5,prFinal="999"))
        assertEquals("00+035",a.prInicial);assertEquals("00+057.5",a.prFinal)
    }
    @Test fun laneAndRoadbedUseExistingDropdownCatalog() {
        assertEquals(listOf("Derecho","Izquierdo"),PavementHeader.lanes)
        assertEquals(com.tuempresa.inventariovial.catalog.SicCatalogRepository.roadbedCodes,PavementHeader.roadbeds)
        assertEquals("Derecho",PavementHeader.lane("D"));assertEquals("Izquierdo",PavementHeader.lane("I"))
    }
    @Test fun repositoryPersistsCreationDateAndPreventsDuplicateNumbers()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        val sessions=AuthSessionManager();sessions.login(UserCacheEntity("u","u","Inspector","OPERATOR",true,"",0,0,0))
        val repo=PavementRepository(db,sessions)
        try {
            db.pavementDao().saveSample(sample())
            val id=repo.newSample(PavementSurface.ASPHALT);val next=db.pavementDao().sample(id)!!
            assertEquals(2,next.muestra);assertEquals(LocalDate.now().toString(),next.fecha)
            repo.saveSample(next.copy(muestra=5,prInicial="100",fecha="1900-01-01"))
            val saved=db.pavementDao().sample(id)!!
            assertEquals(next.fecha,saved.fecha);assertEquals("00+140",saved.prFinal);assertEquals(5,saved.muestra)
            assertTrue(runCatching { repo.saveSample(saved.copy(muestra=1)) }.exceptionOrNull()?.message.orEmpty().contains("duplicada"))
            repo.saveSample(saved.copy(muestra=1,calzada="UD"))
            assertEquals("UD",db.pavementDao().sample(id)!!.calzada)
        } finally { db.close() }
    }
    @Test fun persistentDateAndSicSurviveDatabaseReopen()=runBlocking {
        val name="parallel-c-reopen-${System.nanoTime()}";fun open()=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        var db=open()
        try { db.pavementDao().saveSample(rigid().copy(sicDataJson="{\"SIC15\":{}}"));db.close();db=open()
            val saved=db.pavementDao().sample("s")!!;assertEquals("2026-10-06",saved.fecha);assertEquals(4.0,saved.slabLengthM!!,0.0);assertEquals("{\"SIC15\":{}}",saved.sicDataJson)
        } finally {db.close();context.deleteDatabase(name)}
    }
    @Test fun rigidDensityCountsUniqueSlabsRatherThanSquareMetres() {
        val result=PavementCalculator(context).calculate(rigid(),listOf(slab("a",22,"M","1,2"),slab("b",22,"M","2,3")))
        assertEquals(3.0,result.totals.single().quantity,0.0);assertEquals(15.0,result.totals.single().density!!,0.0)
    }
    @Test fun largePotholesUseMeasuredAreaAndManualEquivalentDivisors() {
        val a=damage(code=13).copy(geometry="POTHOLE",diameterM=1.0,areaM2=.94,severity="M")
        val u=a.copy(code=85,surface="UNSURFACED",areaM2=1.3)
        assertEquals(2.0,PavementRules.quantity(a),1e-9);assertEquals(2.0,PavementRules.quantity(u),1e-9)
        assertTrue(runCatching { PavementRules.validate(sample(),a.copy(areaM2=null),emptyList()) }.isFailure)
        assertTrue(PavementCalculator(context).calculate(sample(),listOf(a.copy(areaM2=null))).pending.any { it.contains("área medida") })
        assertEquals("M",PavementRules.potholeSeverity(13,1.0,10.0))
        assertEquals("A",PavementRules.potholeSeverity(13,1.0,30.0))
    }
    @Test fun rigidLongSlabsRequireManualJointSubdivisionBeforeIndex() {
        assertNull(PavementCalculator(context).calculate(rigid().copy(slabLengthM=10.0,largoM=100.0),emptyList()).index)
    }
    @Test fun slabSeverityAndDividedExclusionFollowManual() {
        val rows=PavementSlabTotals.records(listOf(slab("a",22,"L","1"),slab("b",22,"H","1"),slab("c",23,"M","2"),slab("d",29,"H","2,3")))
        assertEquals("H",rows.single{it.code==22}.severity);assertEquals("3",rows.single{it.code==29}.slabs)
        assertEquals("2",rows.single{it.code==23}.slabs)
        val two=PavementSlabTotals.records(listOf(slab("a",28,"M","4"),slab("b",28,"M","4")))
        assertEquals("H",two.single().severity)
    }
    @Test fun severityCanonicalStorageRetainsLegacyCurveLookup()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        val sessions=AuthSessionManager();sessions.login(UserCacheEntity("u","u","Inspector","OPERATOR",true,"",0,0,0))
        try { db.pavementDao().saveSample(sample());PavementRepository(db,sessions).saveDamage(damage().copy(severity="B"))
            assertEquals("L",db.pavementDao().damage("d")!!.severity)
            assertTrue(PavementCalculator(context).calculate(sample(),listOf(damage().copy(severity="L"))).pending.any{it.contains("asfalto:7:B")})
            assertEquals(Color.rgb(255,193,7),PavementSeverity.color("L"));assertEquals("H · Alto",PavementSeverity.label("A"))
        } finally {db.close()}
    }
    @Test fun zoomKeepsFocalMetricPointAndRulerSharesOrigin() {
        val before=PavementViewport.fit(5.0,40.0,400f,600f,64f,1f,0f,0f)
        val after=before.zoomAt(100f,300f,2.5f)
        assertEquals(before.metresX(100f),after.metresX(100f),1e-6)
        assertEquals(before.metresY(300f),after.metresY(300f),1e-6)
        assertEquals(64f,before.originX,0f);assertEquals(15f,before.canvasY(40.0),0f)
        val fit=PavementViewport.fit(5.0,40.0,400f,600f,64f,1f,0f,0f)
        assertTrue(fit.canvasY(0.0)<=600);assertTrue(fit.originX+5*fit.scale<=400)
    }
    @Test fun rulerLabelsUseFiveTwoAndOneMetresAtSuitableZoom() {
        assertEquals(5,PavementRuler.labelStep(40.0,400f,30f))
        assertEquals(2,PavementRuler.labelStep(40.0,800f,30f))
        assertEquals(1,PavementRuler.labelStep(40.0,1600f,30f))
    }
    @Test fun crackIsDeterministicAcrossIdsScaleAndReopenAndEndpointsAreExact() {
        val a=SketchPoint(1.0,2.0);val b=SketchPoint(2.0,15.0)
        val first=PavementSketch.crack("uuid",a,b,5.0,40.0)
        assertEquals(first,PavementSketch.crack("uuid",a,b,5.0,40.0));assertEquals(a,first.first());assertEquals(b,first.last())
        assertNotEquals(first,PavementSketch.crack("other-uuid",a,b,5.0,40.0))
        assertTrue(first.drop(1).dropLast(1).any { kotlin.math.abs((it.x-a.x)*(b.y-a.y)-(it.y-a.y)*(b.x-a.x))>1e-5 })
    }
    @Test fun everyManualDamageHasDistinctApplicableVisualRecipe() {
        val kinds=PavementSurface.entries.flatMap { PavementCatalog.forSurface(it) }.map { PavementSketch.kind(it.code) }
        assertTrue(kinds.toSet().size>=12);assertEquals(PavementSketchKind.IRREGULAR_LINE,PavementSketch.kind(10))
        assertEquals(PavementSketchKind.CORNER,PavementSketch.kind(22));assertEquals(PavementSketchKind.TRANSVERSE_PATTERN,PavementSketch.kind(83))
        assertEquals(PavementSketchKind.LONGITUDINAL_BANDS,PavementSketch.kind(15))
    }
    @GraphicsMode(GraphicsMode.Mode.NATIVE) @Test fun selectedSlabHasThickRedBorderAboveDamage() {
        val b=Bitmap.createBitmap(200,800,Bitmap.Config.ARGB_8888)
        try { PavementDrawing.draw(Canvas(b),rigid(),listOf(slab("a",22,"L","1")),20f,setOf(1))
            assertEquals(Color.RED,b.getPixel(25,799));assertEquals(Color.RED,b.getPixel(49,740))
        } finally {b.recycle()}
    }
    @Test fun pdfSegmentsKeepFullScaleAndClipDamageAtSharedBoundaries() {
        val segments=PavementPdfLayout.segments(5.0,50.0)
        assertEquals(listOf(0.0 to 22.4375,22.4375 to 44.875,44.875 to 50.0),segments.map{it.start to it.end})
        val point=damage().copy(geometry="POTHOLE",y1=22.4375,y2=null,diameterM=0.0)
        assertEquals(listOf(2),segments.filter{it.contains(sample(),point)}.map{it.index})
        val cross=damage().copy(y1=21.4375,y2=23.4375)
        assertEquals(listOf(1,2),segments.filter{it.contains(sample(),cross)}.map{it.index})
    }
    @Test fun workbookHidesIterationsButCalculatorAndSnapshotRetainThem() {
        val (_,iterations)=PciCorrection.calculate(listOf(80.0,40.0,30.0)){_,total->total/2}
        assertEquals(listOf(3,2,1),iterations.map { it.q })
        val r=PavementResult(emptyList(),iterations,50.0,"Regular",emptyList())
        assertEquals(iterations,PavementResultJson.decode(PavementResultJson.encode(r)).iterations)
        val out=ByteArrayOutputStream();PavementExcel.write(out,listOf(PavementExportSample(sample(),listOf(damage()),r)))
        val entries=mutableMapOf<String,String>();ZipInputStream(out.toByteArray().inputStream()).use { z -> while(true) { val e=z.nextEntry?:break;entries[e.name]=z.readBytes().toString(Charsets.UTF_8) } }
        assertFalse(entries.getValue("xl/workbook.xml").contains("Iteraciones"));assertEquals(3,entries.keys.count{it.startsWith("xl/worksheets/")})
    }
    @Test fun sicHasOnlySourceColumnsReusesFieldsAndRejectsAmbiguousMappings() {
        val d=damage(code=10);val status=PavementSic.status(sample(),listOf(d),PavementSicFormat.SIC14)
        assertEquals(13,PavementSic.headers.size);assertTrue("tipo" in status.rows.single().missing)
        assertEquals("R",status.rows.single().values[0]);assertEquals("2026-10-06",status.rows.single().values.last())
        assertFalse("ruta" in status.rows.single().missing);assertFalse("gravedad" in status.rows.single().missing)
        assertNull(PavementSic.knownType(PavementSicFormat.SIC14A,20));assertNull(PavementSic.knownType(PavementSicFormat.SIC16,82))
    }
    @Test fun sicManualPendingFieldsCompleteAndExportWithoutPciCodeLeak() {
        val d=damage(code=10)
        val json=PavementSic.update(sample(),PavementSicFormat.SIC14,"d",mapOf("tipo" to "8","faja" to "2","prInicio" to "0000","distanciaInicio" to "2","prFin" to "0000","distanciaFin" to "15"))
        val s=sample().copy(sicDataJson=json);assertTrue(json.contains(PavementSic.manualOrigin))
        val status=PavementSic.status(s,listOf(d),PavementSicFormat.SIC14)
        assertTrue(status.complete);assertEquals(8,status.rows.single().values[7])
        val out=ByteArrayOutputStream();PavementSic.write(out,s,listOf(d),PavementSicFormat.SIC14);assertTrue(out.size()>1000)
    }
    @Test fun sic14aDensityContradictionRemainsBlockedRatherThanInvented() {
        val d=damage(code=13).copy(geometry="POTHOLE",diameterM=.4)
        val status=PavementSic.status(sample(),listOf(d),PavementSicFormat.SIC14A)
        assertFalse(status.complete);assertTrue(status.blockers.single().contains("07"))
    }
    @Test fun sic16UsesContractualFajaTwoAndOwnDamageCatalog() {
        val d=damage(code=85).copy(surface="UNSURFACED",geometry="POTHOLE",diameterM=.5)
        val status=PavementSic.status(sample().copy(surface="UNSURFACED"),listOf(d),PavementSicFormat.SIC16)
        assertEquals(2,status.rows.single().values[2]);assertEquals(3,status.rows.single().values[7])
        assertTrue("densidad" in status.rows.single().missing)
    }
    @Test fun generatedFileManifestRetainsNativeIdentityHashAndPhotoAssociation() {
        val source=File(context.cacheDir,"parallel-c-source-${System.nanoTime()}.json").apply {writeText("{\"sample\":\"s\"}")}
        var copy:File?=null
        try {
            val r=PavementGeneratedFiles.preservePrepared(context,sample(),"SAMPLE_DATA","application/json",source,photoId="photo",damageId="damage")
            copy=File(r.localPath);assertEquals(source.readText(),copy!!.readText());assertEquals(64,r.sha256.length)
            assertEquals(r,PavementGeneratedFiles.records(context).single { it.id==r.id })
            val changed=PavementGeneratedFiles.register(context,sample(),"SAMPLE_DATA","application/json",copy!!,"content://chosen/destination",photoId="photo",damageId="damage")
            assertEquals(r.id,changed.id);assertEquals("damage",changed.damageId);assertEquals("content://chosen/destination",changed.destinationUri)
            source.delete();assertTrue(copy!!.exists())
        } finally {source.delete();copy?.delete()}
    }
    @GraphicsMode(GraphicsMode.Mode.NATIVE) @Test fun denseReportPaginatesRowsAndRendersEveryVisualRecipe() {
        val output=File("build/reports/parallel-c").apply{mkdirs()}
        val s=sample().copy(largoM=50.0,prFinal="00+050")
        val dense=(1..70).map { i -> damage("damage-$i").copy(correlativo=i,y1=10.0,y2=15.0) }
        val pages=mutableListOf<Bitmap>()
        val r=PavementResult(emptyList(),emptyList(),null,null,listOf("Curvas pendientes; datos sintéticos de prueba"))
        PavementPdf.render(PavementExportSample(s,dense,r),emptyMap(),{Bitmap.createBitmap(PavementPdfLayout.PAGE_WIDTH,PavementPdfLayout.PAGE_HEIGHT,Bitmap.Config.ARGB_8888).also { pages+=it }.let { Canvas(it).apply{drawColor(Color.WHITE)} }},{})
        assertTrue("Dense table must continue on additional pages",pages.size>=5)
        pages.forEachIndexed { i,b -> File(output,"dense-${i+1}.png").outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle() }
        for(surface in PavementSurface.entries) {
            val unit=if(surface==PavementSurface.RIGID)rigid()else sample().copy(surface=surface.name)
            val items=PavementCatalog.forSurface(surface).mapIndexed { i,definition ->
                damage("recipe-${definition.code}",definition.code).copy(correlativo=i+1,surface=surface.name,name=definition.name,severity=definition.severities.firstOrNull(),geometry=definition.geometry.name,
                    x1=1.0,y1=(i%10)*3.0+1,x2=3.0,y2=(i%10)*3.0+3,diameterM=.6,slabs=((i%20)+1).toString())
            }
            val b=Bitmap.createBitmap(500,820,Bitmap.Config.ARGB_8888)
            val c=Canvas(b);c.drawColor(Color.WHITE);c.translate(30f,10f);PavementDrawing.draw(c,unit,items,20f)
            File(output,"recipes-${surface.name}.png").outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle()
        }
    }
    @Test fun suppliedSicLayoutsExportNativeGroupsAndThirteenColumns() {
        val output=File("build/reports/parallel-c").apply{mkdirs()}
        for(format in PavementSicFormat.entries) {
            val s=sample().copy(surface=format.surface)
            val d=when(format) { PavementSicFormat.SIC15 -> slab("d",22,"M","1");PavementSicFormat.SIC16 -> damage(code=85).copy(surface="UNSURFACED",geometry="POTHOLE",diameterM=.5);else -> damage(code=1).copy(geometry="AREA",areaM2=13.0) }
            val json=PavementSic.update(s,format,"d",mapOf("faja" to "2","prInicio" to "0000","distanciaInicio" to "2","prFin" to "0000","distanciaFin" to "15","longitud" to "13","ancho" to "1","densidad" to "2"))
            val out=ByteArrayOutputStream();PavementSic.write(out,s.copy(sicDataJson=json),listOf(d),format)
            File(output,"${format.label}-synthetic.xlsx").writeBytes(out.toByteArray())
            val xml=ZipInputStream(out.toByteArray().inputStream()).use { z -> var sheet="";while(true) { val e=z.nextEntry?:break;if(e.name=="xl/worksheets/sheet1.xml")sheet=z.readBytes().toString(Charsets.UTF_8) };sheet }
            assertTrue(xml.contains("D1:E1"));assertTrue(xml.contains("F1:G1"));assertTrue(xml.contains("ySplit=\"2\""))
            val doc=javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.byteInputStream())
            val rows=doc.getElementsByTagName("row");assertEquals(3,rows.length);assertEquals(13,(rows.item(2) as org.w3c.dom.Element).getElementsByTagName("c").length)
        }
    }
    @Test fun migration12to13PreservesChildrenClosedSnapshotsAndBackfillsSlabGeometry()=runBlocking {
        val name="parallel-c-migration-${System.nanoTime()}";val path=context.getDatabasePath(name);path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path,null).use { old ->
            val entities=JSONObject(File("schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/12.json").readText()).getJSONObject("database").getJSONArray("entities")
            for(i in 0 until entities.length()) { val e=entities.getJSONObject(i);old.execSQL(e.getString("createSql").replace("\u0024{TABLE_NAME}",e.getString("tableName")));val indices=e.optJSONArray("indices")?:org.json.JSONArray();for(j in 0 until indices.length())old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\u0024{TABLE_NAME}",e.getString("tableName"))) }
            old.execSQL("INSERT INTO pavement_samples VALUES ('s','T','R',1,'D','UC','0','40',5,40,'RIGID','2026-10-06','u','Inspector','CLOSED',1,1,10,2,NULL,'immutable')")
            old.execSQL("INSERT INTO pavement_damages (id,sampleId,correlativo,surface,code,name,severity,geometry,createdAt) VALUES ('d','s',1,'RIGID',22,'Esquina','A','SLABS',1)")
            old.execSQL("INSERT INTO pavement_damage_photos VALUES ('p','d','/unchanged.jpg',1)");old.execSQL("INSERT INTO user_cache VALUES ('keep','u','Inspector','OPERATOR',1,'cipher',1,1,1)");old.version=12
        }
        val db=Room.databaseBuilder(context,InventoryDatabase::class.java,name).addMigrations(InventoryDatabase.MIGRATION_12_13, InventoryDatabase.MIGRATION_13_14,InventoryDatabase.MIGRATION_14_15).addMigrations(*allInventoryMigrations()).build()
        try { val s=db.pavementDao().sample("s")!!;assertEquals("immutable",s.calculationSnapshot);assertEquals(4.0,s.slabLengthM!!,0.0);assertEquals(2.5,s.slabWidthM!!,0.0);assertEquals("H",db.pavementDao().damage("d")!!.severity);assertEquals("/unchanged.jpg",db.pavementDao().photos("d").single().localPath)
            db.openHelper.readableDatabase.query("SELECT id FROM user_cache").use{assertTrue(it.moveToFirst());assertEquals("keep",it.getString(0))}
        } finally {db.close();context.deleteDatabase(name)}
    }
}
