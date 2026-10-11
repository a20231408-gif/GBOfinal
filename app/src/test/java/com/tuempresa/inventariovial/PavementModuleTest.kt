package com.tuempresa.inventariovial

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.pavement.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import java.io.File
import org.json.JSONObject
import android.database.sqlite.SQLiteDatabase
import com.tuempresa.inventariovial.auth.AuthSessionManager
import com.tuempresa.inventariovial.auth.UserCacheEntity

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class PavementModuleTest {
    private fun sample()=PavementSampleEntity("s","Ruta A","A",1,"Derecho","","0001","0002",5.0,46.0,"ASPHALT","2026-09-28","u","Inspector")
    private fun damage(id:String="d",code:Int=1)=PavementDamageEntity(id,"s",1,"ASPHALT",code,"Daño","B","AREA",0.0,0.0,2.0,2.0,null,2.0,2.0,4.0,null,null,"")

    @Test fun rulerAndDamageUseSameBottomLeftTransform() {
        for(length in listOf(8.0,10.0,40.0,100.0)) {
            val transform=PavementScale(length,10f)
            assertEquals((length*10).toFloat(),transform.canvasY(0.0),0f)
            assertEquals(0f,transform.canvasY(length),0f)
            for(d in PavementRuler.ticks(length)) assertEquals(d,transform.metresY(transform.canvasY(d)),1e-6)
        }
        assertEquals(listOf(0.0,5.0,10.0),PavementRuler.ticks(10.0).filter{it%5==0.0})
        assertEquals((0..8).map{it*5.0},PavementRuler.ticks(40.0).filter{it%5==0.0})
        assertEquals(2,PavementRuler.labelStep(40.0,800f,30f))
        assertTrue(PavementRuler.labelStep(100.0,200f,30f)>5)
        // Grid spacing is intentionally absent from the metric transform.
        assertEquals(200f,PavementScale(40.0,10f).canvasY(20.0),0f)
    }

    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Test fun importedPhotosPersistAndDeleteOnlySelectedManagedCopy()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="pavement-photos-${System.nanoTime()}.db"
        fun open()=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        var db=open()
        val sessions=AuthSessionManager();sessions.login(UserCacheEntity("u","u","Inspector","OPERATOR",true,"",0,0,0))
        val source=File(context.cacheDir,"original-${System.nanoTime()}.png")
        val bitmap=android.graphics.Bitmap.createBitmap(2,2,android.graphics.Bitmap.Config.ARGB_8888)
        source.outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        val copies=mutableListOf<File>()
        try {
            val dao=db.pavementDao();dao.saveSample(sample());dao.saveDamage(damage());dao.saveDamage(damage("other").copy(correlativo=2))
            val originalDamage=requireNotNull(dao.damage("d"))
            val before=PavementCalculator(context).calculate(sample(),dao.damages("s"))
            val repo=PavementRepository(db,sessions)
            for(i in 1..3) {
                val copy=PavementPhotos.importImage(context,android.net.Uri.fromFile(source));copies+=copy
                repo.savePhoto(PavementDamagePhotoEntity("p$i","d",copy.absolutePath),"s")
            }
            repo.savePhoto(PavementDamagePhotoEntity("external","other",source.absolutePath),"s")
            assertTrue(runCatching { repo.deletePhoto("p2","other",File(context.filesDir,"images")) }.isFailure)
            db.close();db=open()
            val reopened=PavementRepository(db,sessions)
            assertEquals(3,reopened.photos("d").size)
            assertTrue(reopened.photos("d").all { File(it.localPath).exists() })
            reopened.deletePhoto("p2","d",File(context.filesDir,"images"))
            assertEquals(setOf("p1","p3"),reopened.photos("d").map{it.id}.toSet())
            assertFalse(copies[1].exists());assertTrue(copies[0].exists());assertTrue(copies[2].exists())
            assertEquals("external",reopened.photos("other").single().id)
            reopened.deletePhoto("external","other",File(context.filesDir,"images"));assertTrue(source.exists())
            assertEquals(originalDamage,db.pavementDao().damage("d"))
            assertEquals(before,PavementCalculator(context).calculate(sample(),db.pavementDao().damages("s")))
        } finally { db.close();context.deleteDatabase(name);source.delete();copies.forEach{it.delete()} }
    }

    @Test fun migrationFromDeliveredVersion10KeepsExistingUsers()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val name="pavement-migration-${System.nanoTime()}.db"
        val path=context.getDatabasePath(name);path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path,null).use { old ->
            val entities=JSONObject(File("schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/10.json").readText()).getJSONObject("database").getJSONArray("entities")
            for(i in 0 until entities.length()) {
                val e=entities.getJSONObject(i);old.execSQL(e.getString("createSql").replace("\u0024{TABLE_NAME}",e.getString("tableName")))
                val indices=e.optJSONArray("indices")?:org.json.JSONArray();for(j in 0 until indices.length())old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\u0024{TABLE_NAME}",e.getString("tableName")))
            }
            old.execSQL("INSERT INTO user_cache VALUES ('keep','inspector','Inspector','OPERATOR',1,'cipher',1,1,1)")
            old.version=10
        }
        val db=Room.databaseBuilder(context,InventoryDatabase::class.java,name).addMigrations(InventoryDatabase.MIGRATION_10_11,InventoryDatabase.MIGRATION_11_12,InventoryDatabase.MIGRATION_12_13, InventoryDatabase.MIGRATION_13_14,InventoryDatabase.MIGRATION_14_15).build()
        try {
            assertNull(db.pavementDao().sample("missing"))
            db.openHelper.readableDatabase.query("SELECT username FROM user_cache WHERE id='keep'").use{assertTrue(it.moveToFirst());assertEquals("inspector",it.getString(0))}
            db.pavementDao().saveSample(sample());assertEquals("Inspector",db.pavementDao().sample("s")!!.inspectorNombre)
        } finally {db.close();context.deleteDatabase(name)}
    }
    @Test fun closedSamplesAndOtherInspectorsCannotWrite()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),InventoryDatabase::class.java).build()
        val sessions=AuthSessionManager();sessions.login(UserCacheEntity("u","u","Inspector","OPERATOR",true,"",0,0,0))
        val repo=PavementRepository(db,sessions)
        try {
            db.pavementDao().saveSample(sample())
            repo.saveDamage(damage());repo.closeSample("s")
            assertTrue(runCatching{repo.saveDamage(damage("another"))}.isFailure)
            assertTrue(runCatching{repo.deleteDamage(damage())}.isFailure)
            db.pavementDao().saveSample(sample().copy(status="DRAFT"))
            sessions.logout();sessions.login(UserCacheEntity("other","other","Other","OPERATOR",true,"",0,0,0))
            assertTrue(runCatching{repo.saveSample(sample())}.isFailure)
        } finally {db.close()}
    }

    @Test fun updatingHeaderAndDamagePreservesChildren()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),InventoryDatabase::class.java).build()
        try {
            val dao=db.pavementDao();dao.saveSample(sample());dao.saveDamage(damage())
            dao.savePhoto(PavementDamagePhotoEntity("p","d","/photo.jpg"))
            dao.saveSample(sample().copy(carril="Izquierdo"));dao.saveDamage(damage().copy(observation="Editado"))
            assertEquals(1,dao.damages("s").size);assertEquals("p",dao.photos("d").single().id)
            dao.deleteDamage("d");assertTrue(dao.photos("d").isEmpty())
        } finally {db.close()}
    }
    @Test fun areaLimitUsesSameCodeAcrossSeverities() {
        val s=sample().copy(anchoM=5.0,largoM=2.0)
        PavementRules.validate(s,damage(),listOf(damage("other",2)))
        assertTrue(runCatching{PavementRules.validate(s,damage().copy(areaM2=8.0,severity="M"),listOf(damage("other")))}.isFailure)
    }
    @Test fun coordinatesCannotEscapeSampleOrBeNan() {
        assertTrue(runCatching{PavementRules.validate(sample(),damage().copy(x1=Double.NaN),emptyList())}.isFailure)
        assertTrue(runCatching{PavementRules.validate(sample(),damage().copy(x2=6.0),emptyList())}.isFailure)
    }
    // Synthetic points test interpolation mechanics only; these are NOT manual curves.
    @Test fun logarithmicInterpolationAndNoExtrapolation() {
        val points=listOf(CurvePoint(1.0,10.0),CurvePoint(100.0,30.0))
        assertEquals(20.0,CurveInterpolation.value(points,10.0,true),1e-9)
        assertTrue(runCatching{CurveInterpolation.value(points,0.0,true)}.isFailure)
        assertTrue(runCatching{CurveInterpolation.value(points,101.0,true)}.isFailure)
    }
    @Test fun missingCurvesNeverProduceNumericIndex() {
        val result=PavementCalculator(ApplicationProvider.getApplicationContext<Context>()).calculate(sample(),listOf(damage()))
        assertNull(result.index);assertTrue(result.pending.any{it.contains("asfalto:1:B")})
        assertEquals(4.0/230.0*100,result.totals.single().density!!,1e-9)
    }
    @Test fun singleDeductionUsesTotalWithoutCorrection() {
        val result=PciCorrection.calculate(listOf(15.0,2.0,1.0)){_,_->error("No correction required")}
        assertEquals(18.0,result.first,0.0);assertTrue(result.second.isEmpty())
    }
    @Test fun urciUsesFiveAndNinetyFiveInsteadOfPciConstants() {
        val single=PciCorrection.calculate(listOf(20.0,5.0,3.0),unpaved=true){_,_->error("No correction required")}
        assertEquals(28.0,single.first,0.0)
        val (_,rows)=PciCorrection.calculate(listOf(80.0,40.0,30.0),unpaved=true){_,total->total/2}
        assertEquals(listOf(3,2,1),rows.map{it.q})
        assertEquals(30*(1+9.0/95*20-2),rows.first().values.last(),1e-9)
        assertEquals(listOf(80.0,5.0,5.0),rows.last().values)
    }
    @Test fun unpavedWithoutDamageNeedsNoCurveAndRigidRequiresSlabGrid() {
        val calculator=PavementCalculator(ApplicationProvider.getApplicationContext<Context>())
        assertEquals(100.0,calculator.calculate(sample().copy(surface="UNSURFACED"),emptyList()).index!!,0.0)
        assertNull(calculator.calculate(sample().copy(surface="RIGID"),emptyList()).index)
    }
    @Test fun translatingClampsBothEndpointsAndPreservesMeasuredQuantity() {
        val moved=PavementMovement.translate(sample(),damage().copy(areaM2=3.5),100.0,-20.0)
        assertEquals(3.0,moved.x1!!,0.0);assertEquals(5.0,moved.x2!!,0.0)
        assertEquals(0.0,moved.y1!!,0.0);assertEquals(2.0,moved.y2!!,0.0)
        assertEquals(3.5,moved.areaM2!!,0.0)
    }
    @Test fun additionalSamplesUseSectionAreaAndMissingSamplesBlockResult() {
        fun row(id:String,index:Double)=PavementExportSample(sample().copy(id=id,anchoM=5.0,largoM=20.0,status="CLOSED"),emptyList(),PavementResult(emptyList(),emptyList(),index,null,emptyList()))
        val samples=listOf(row("r",80.0),row("a",20.0))
        val config=PavementSectionEntity("A","Ruta A","ASPHALT",1000.0,"a","u")
        assertEquals(74.0,PavementSectionCalculation.calculate(samples,config).index!!,1e-9)
        assertEquals(50.0,PavementSectionCalculation.calculate(samples,null).index!!,1e-9)
        assertNull(PavementSectionCalculation.calculate(samples.take(1),config).index)
        assertNull(PavementSectionCalculation.calculate(samples,config.copy(areaM2=150.0)).index)
    }
    @Test fun forgedSampleIdsCannotChangeAnotherInspectorsDamageOrPhotos()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),InventoryDatabase::class.java).build()
        val sessions=AuthSessionManager();sessions.login(UserCacheEntity("u","u","Inspector","OPERATOR",true,"",0,0,0))
        val repo=PavementRepository(db,sessions)
        try {
            db.pavementDao().saveSample(sample())
            db.pavementDao().saveSample(sample().copy(id="other",muestra=2,inspectorUserId="other"))
            db.pavementDao().saveDamage(damage().copy(sampleId="other"))
            assertTrue(runCatching{repo.saveDamage(damage())}.isFailure)
            assertTrue(runCatching{repo.deleteDamage(damage())}.isFailure)
            assertTrue(runCatching{repo.savePhoto(PavementDamagePhotoEntity("p","d","x"),"s")}.isFailure)
            assertEquals("other",db.pavementDao().damage("d")!!.sampleId)
        } finally {db.close()}
    }
    @Test fun fractionalRetentionAndIterationsDoNotMutateInput() {
        val values=listOf(80.0,40.0,30.0)
        val (_,rows)=PciCorrection.calculate(values){_,total->total/2}
        assertEquals(listOf(3,2,1),rows.map{it.q})
        val m=1+(9.0/98)*20
        assertEquals(30*(m-2),rows.first().values.last(),1e-9)
        assertEquals(listOf(80.0,40.0,30.0),values)
    }
    @Test fun potholeSuggestionDoesNotGuessAmbiguousLimits() {
        assertEquals("B",PavementRules.potholeSeverity(13,0.15,20.0))
        assertEquals("A",PavementRules.potholeSeverity(13,0.5,30.0))
        assertEquals("M",PavementRules.potholeSeverity(85,0.4,60.0))
        assertNull(PavementRules.potholeSeverity(13,0.2,20.0))
        assertNull(PavementRules.potholeSeverity(85,0.4,50.5))
    }
    @Test fun calculationSnapshotRetainsPendingReasonsAndIterationValues() {
        val result=PavementResult(listOf(PavementTotal(26,"M",0.0,null,null)),listOf(CorrectionIteration(listOf(20.0,2.0),1,22.0,20.0)),null,null,listOf("Falta curva 26"))
        assertEquals(result,PavementResultJson.decode(PavementResultJson.encode(result)))
    }
    @Test fun exportKeepsExactThirteenHeadersAndEscapesText() {
        val result=PavementResult(emptyList(),emptyList(),null,null,listOf("Falta curva"))
        val out=ByteArrayOutputStream()
        PavementExcel.write(out,listOf(PavementExportSample(sample(),listOf(damage().copy(observation="<x>& =1+1")),result)))
        val entries=mutableMapOf<String,String>()
        ZipInputStream(out.toByteArray().inputStream()).use{z->var e=z.nextEntry;while(e!=null){entries[e.name]=z.readBytes().toString(Charsets.UTF_8);e=z.nextEntry}}
        assertEquals(13,PavementExcel.headers.size)
        val sheet=entries.getValue("xl/worksheets/sheet1.xml")
        assertTrue(sheet.contains("&lt;x&gt;&amp; =1+1"));assertFalse(sheet.contains("<f>"))
        assertTrue(entries.getValue("xl/workbook.xml").contains("Resumen"))
    }
}
