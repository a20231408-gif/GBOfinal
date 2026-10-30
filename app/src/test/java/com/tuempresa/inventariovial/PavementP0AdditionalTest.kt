package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.scap.export.*
import kotlinx.coroutines.*
import org.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.*

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class,shadows=[SyntheticPdf146::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementP0AdditionalTest {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    @Before fun idle(){initializeIdleUiWorkers()}
    private fun db()=Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build()
    private fun sessions()=AuthSessionManager().apply{login(UserCacheEntity("u","u","FICTICIO","ENGINEER",true,"",0,0,0))}
    private fun sample(surface:String="ASPHALT",id:String="TEST_PV_P0",date:String="2026-10-20")=PavementSampleEntity(id,"T1","PE-28H",1,"Derecho","CD","","",7.0,20.0,surface,date,"u","FICTICIO",createdAt=100,updatedAt=100,slabRows=4,slabColumns=2)
    private fun damage(s:PavementSampleEntity,id:String="D1",code:Int=if(s.surface=="ASPHALT")1 else if(s.surface=="RIGID")39 else 85,at:Long=100)=PavementDamageEntity(id,s.id,1,s.surface,code,"FICTICIO","L",when{code==26->"GLOBAL";s.surface=="RIGID"->"SLABS";code==85->"POTHOLE";else->"AREA"},null,null,null,null,if(code==85).3 else null,1.0,2.0,2.0,null,null,"OBS_$id",createdAt=at,slabs=if(s.surface=="RIGID"&&code!=26)"1"else"")
    private fun filled(s:PavementSampleEntity,d:PavementDamageEntity,f:PavementSicFormat)=s.copy(sicDataJson=PavementSic.update(s,f,d.id,mapOf("longitud" to "2","ancho" to "1","densidad" to "1")))
    private fun dump(name:String,payload:JSONObject){
        val wire=JSONObject(payload.toString());val files=wire.getJSONArray("files")
        for(n in 0 until files.length()){val f=files.getJSONObject(n);f.put("base64",android.util.Base64.encodeToString(File(f.getString("localFile")).readBytes(),android.util.Base64.NO_WRAP));f.remove("localFile")}
        File(System.getProperty("gbo.p0.evidence"),name).apply{parentFile!!.mkdirs();writeText(wire.toString(2))}
    }
    private fun cells(bytes:ByteArray,row:Int=3):List<String>{val b=TemplateWorkbook(bytes.inputStream());return b.sheet(1).nodes("row").single{it.getAttribute("r")==row.toString()}.children().map{it.textContent}}
    @Test fun allFourSicsExportBlankOptionalLocationsWithoutChangingTechnicalCells(){
        for(f in PavementSicFormat.entries){val s0=sample(f.surface);val d=damage(s0);val s=filled(s0,d,f)
            val status=PavementSic.status(s,listOf(d),f);assertTrue(status.toString(),status.complete)
            val bytes=ByteArrayOutputStream().also{PavementSic.write(it,s,listOf(d),f)}.toByteArray();assertEquals(1,SicRouteWorkbook.inspect(bytes,f.label))
            val values=cells(bytes);for(n in 2..6)assertEquals("${f.label} col $n","",values[n])
            assertEquals("2.0",values[8]);assertEquals("1.0",values[9]);assertEquals(s.fecha,values[12])
            val voluntary=s.copy(sicDataJson=PavementSic.update(s,f,d.id,mapOf("faja" to "3","prInicio" to "0012","distanciaInicio" to "0","prFin" to "0013","distanciaFin" to "15")))
            val updated=cells(ByteArrayOutputStream().also{PavementSic.write(it,voluntary,listOf(d),f)}.toByteArray())
            assertEquals("0012",updated[3]);assertEquals("0.0",updated[4]);assertEquals("15.0",updated[6])
        }
    }
    @Test fun chronologyUsesPersistedDateTimeAndIdentityNeverPrOrExportTime()=runBlocking {db().use{d->
        val late=sample(id="LATE",date="2026-10-22").copy(updatedAt=999999);val early=sample(id="EARLY",date="2026-10-20").copy(muestra=2)
        val a=damage(late,"A",at=10).copy(lengthM=3.0);val b=damage(early,"B",at=500).copy(lengthM=4.0)
        d.pavementDao().saveSample(late);d.pavementDao().saveSample(early);d.pavementDao().saveDamage(a);d.pavementDao().saveDamage(b)
        val storage=SicTestStorage();val first=LocalSicConsolidates.update(c,d,"PE-28H","SIC-14",storage)
        assertEquals(2,first.rows);assertEquals("4.0",cells(first.file.readBytes(),3)[8]);assertEquals("3.0",cells(first.file.readBytes(),4)[8])
        d.pavementDao().saveDamage(b.copy(widthM=2.0));val second=LocalSicConsolidates.update(c,d,"PE-28H","SIC-14",storage)
        assertEquals(2,second.rows);assertEquals("2.0",cells(second.file.readBytes(),3)[9]);assertEquals(1,storage.entries.values.count{it.name=="SIC-14.xlsx"})
        val tie=listOf(damage(early,"Z",at=200).copy(lengthM=8.0),damage(early,"B",at=100).copy(lengthM=5.0),damage(early,"A",at=100).copy(lengthM=6.0))
        val bytes=ByteArrayOutputStream().also{PavementSic.write(it,early,tie,PavementSicFormat.SIC14)}.toByteArray()
        assertEquals(listOf("6.0","5.0","8.0"),(3..5).map{cells(bytes,it)[8]})
    }}
    @Test fun threeJointOccurrencesPersistIndependentlyEditArchiveAndDoNotTriplePci()=runBlocking {
        val name="joint-p0.db";val auth=sessions();val s=sample("RIGID")
        fun open()=Room.databaseBuilder(c,InventoryDatabase::class.java,name).build()
        val originals=mutableListOf<PavementDamageEntity>()
        open().use{d->d.pavementDao().saveSample(s);val repo=PavementRepository(d,auth)
            val marks=listOf(PavementJointAnnotations.Mark("H",1,0),PavementJointAnnotations.Mark("H",2,0),PavementJointAnnotations.Mark("V",0,1))
            for(n in 0..2){val x=damage(s,"J$n",26,100L+n).copy(severity=listOf("L","M","H")[n]);repo.saveDamageWithJointMarks(x,setOf(marks[n]))}
            originals+=d.pavementDao().damages(s.id);assertEquals(3,originals.size)
            val snapshot=d.pavementDao().sample(s.id)!!;assertTrue(PavementCalculator(c).calculate(snapshot,originals).pending.any{it.contains("evaluación global")})
            assertTrue(PavementJointOccurrences.calculationRecords(snapshot,originals).none{it.code==26})
            assertTrue(runCatching{repo.saveDamageWithJointMarks(damage(s,"DUP",26).copy(observation="OBS_J0"),setOf(marks[0]))}.isFailure)
            repo.closeSample(s.id);assertEquals(3,d.pavementDao().damages(s.id).size)
        }
        open().use{d->val repo=PavementRepository(d,auth);assertEquals(originals,d.pavementDao().damages(s.id));repo.reopenSample(s.id)
            val edit=originals[1].copy(severity="H",observation="EDITADO",createdAt=99999)
            repo.saveDamageWithJointMarks(edit,setOf(PavementJointAnnotations.Mark("H",2,0)))
            assertEquals(originals[0],d.pavementDao().damage("J0"));assertEquals(originals[2],d.pavementDao().damage("J2"));assertEquals(originals[1].createdAt,d.pavementDao().damage("J1")!!.createdAt)
            repo.saveSealUnitAssessment(s.id,"H");val current=d.pavementDao().sample(s.id)!!;val records=d.pavementDao().damages(s.id)
            val projection=PavementJointOccurrences.calculationRecords(current,records);assertEquals(1,projection.count{it.code==26});assertEquals("H",projection.single().severity)
            val legacy=PavementCalculator(c).calculate(s,listOf(damage(s,code=26).copy(severity="H")))
            val result=PavementCalculator(c).calculate(current,records);assertEquals(legacy.totals,result.totals);assertEquals(legacy.index,result.index)
            val xlsx=ByteArrayOutputStream().also{PavementExcel.write(it,listOf(PavementExportSample(current,records,result)))}.toByteArray()
            assertEquals(4,TemplateWorkbook(xlsx.inputStream()).sheet(1).nodes("row").size)
            val detail=TemplateWorkbook(xlsx.inputStream()).sheet(3).nodes("row")
            assertEquals(1,detail.count{it.textContent.contains("GLOBAL PCI")})
            val pdf=ByteArrayOutputStream().also{PavementPdf.write(it,PavementExportSample(current,records,result),emptyMap())}.toByteArray();assertTrue(String(pdf).startsWith("%PDF"))
            val photo=File(c.filesDir,"JOINT_EVIDENCE.jpg").apply{writeBytes(byteArrayOf(1,2,3,4))};repo.savePhoto(PavementDamagePhotoEntity("PJ1","J1",photo.path),s.id)
            val before=photo.readBytes();repo.deleteDamage(d.pavementDao().damage("J1")!!)
            assertEquals(2,d.pavementDao().damages(s.id).size);assertEquals(3,d.pavementDao().damageHistory(s.id).size);assertNotNull(d.pavementDao().damage("J1")!!.retiredAt)
            assertEquals(1,d.pavementDao().photoHistory("J1").size);assertArrayEquals(before,photo.readBytes());assertEquals(originals[0],d.pavementDao().damage("J0"))
            assertEquals(1,PavementDamageArchive.manifest(d,current).getJSONArray("damages").length());assertEquals("H",PavementJointOccurrences.assessment(d.pavementDao().sample(s.id)!!))
        }
    }
    @Test fun v21BackupWithoutPrGetsFullParentAckAndNewConsolidationIsExplicitlyPending()=runBlocking {db().use{d->
        val s=sample();d.pavementDao().saveSample(s);d.pavementDao().saveDamage(damage(s));PavementRepository(d,sessions()).closeSample(s.id)
        var posts=0
        val transport=object:AutomaticDriveTransport {
            override fun supportsV3()=true;override fun supportsFamilyPaths()=true;override fun supportsReadablePhotoNames()=true
            override fun reservePhotoNames(payload:JSONObject)=ReleasePhotoV2Fixture.reservation(payload,"FOLDER_TEST")
            override fun post(p:JSONObject):JSONObject {posts++;dump("ANDROID_PAVEMENT_V21.json",p);assertEquals(0,p.getJSONArray("sicRows").length());assertTrue(p.getJSONArray("pendingSic").length()>0)
                val prefix="PE-28H/EVALUACION_SUPERFICIAL/T1/ASFALTICO/MUESTRA 1";val files=p.getJSONArray("files")
                assertTrue((0 until files.length()).any{files.getJSONObject(it).getString("path")=="DATOS_APP/SIC-14.xlsx"})
                return JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",4).put("key",p.getString("key")).put("revision",p.getLong("revision")).put("contentHash",p.getString("contentHash")).put("folderId","FOLDER_TEST").put("logicalPath",prefix)
                    .put("files",JSONArray((0 until files.length()).map{n->val f=files.getJSONObject(n);JSONObject().put("key",f.getString("key")).put("sha256",f.getString("sha256")).put("fileId","FILE_$n").put("folderId","FOLDER_TEST").put("requestedPath",f.getString("path")).put("logicalPath",prefix+"/"+f.getString("path"))})).put("sicFiles",JSONArray())
            }
        }
        val needsRetry=AutomaticSyncDrain.run(c,d,{transport},null);val intent=d.automaticSyncDao().all().single()
        assertFalse("Unexpected retry, state=${intent.state}, error=${intent.error}",needsRetry);assertEquals("SYNCED",intent.state);assertTrue(AutomaticDelivery.validAck(JSONObject(intent.preparedJson!!),JSONObject(intent.receiptJson!!)))
        assertFalse(AutomaticSyncDrain.run(c,d,{transport},null));assertEquals(1,posts)
        val nextSample=s.copy(id="TEST_CHRONO",muestra=2);val nextDamage=damage(nextSample,"D_CHRONO")
        d.pavementDao().saveSample(nextSample);d.pavementDao().saveDamage(nextDamage);PavementRepository(d,sessions()).closeSample(nextSample.id)
        val nextIntent=d.automaticSyncDao().get("PAVEMENT/${nextSample.id}")!!
        val prepared=AutomaticDelivery.prepare(c,d,nextIntent,persist=false,pavementChronological=true)
        assertEquals(1,prepared.optInt("pavementSicChronologicalVersion"));assertEquals(2,prepared.getJSONArray("sicRows").length())
        val row=prepared.getJSONArray("sicRows").getJSONObject(0);assertTrue(row.isNull("startPrNumber"));assertTrue(row.isNull("startDistanceM"));assertEquals(nextDamage.createdAt,row.getLong("registeredAt"))
        val wire=JSONObject(prepared.toString());val files=wire.getJSONArray("files")
        for(n in 0 until files.length()){val f=files.getJSONObject(n);f.put("base64",android.util.Base64.encodeToString(File(f.getString("localFile")).readBytes(),android.util.Base64.NO_WRAP));f.remove("localFile")}
        File(System.getProperty("gbo.p0.evidence"),"ANDROID_PAVEMENT_CHRONO.json").apply{parentFile!!.mkdirs();writeText(wire.toString(2))}
        Unit

    }}
    @Test fun historicalGlobalAndManualGlobalRemainIndependentFromIndividualEdits()=runBlocking{db().use{d->
        val s=sample("RIGID");d.pavementDao().saveSample(s);val repo=PavementRepository(d,sessions())
        repo.saveDamage(damage(s,"LEGACY",26).copy(severity="M"));val before=d.pavementDao().damage("LEGACY")!!
        val original=PavementCalculator(c).calculate(s,listOf(before))
        repo.saveDamageWithJointMarks(damage(s,"NEW",26).copy(severity="L"),setOf(PavementJointAnnotations.Mark("H",1,0)))
        var saved=d.pavementDao().sample(s.id)!!;assertEquals("M",PavementJointOccurrences.assessment(saved));assertEquals(before,d.pavementDao().damage("LEGACY"))
        assertEquals(original.totals,PavementCalculator(c).calculate(saved,d.pavementDao().damages(s.id)).totals)
        repo.saveSealUnitAssessment(s.id,"H")
        repo.saveDamageWithJointMarks(d.pavementDao().damage("NEW")!!.copy(severity="H"),setOf(PavementJointAnnotations.Mark("H",2,0)))
        assertEquals("H",PavementJointOccurrences.assessment(d.pavementDao().sample(s.id)!!))
        repo.deleteDamage(d.pavementDao().damage("NEW")!!);saved=d.pavementDao().sample(s.id)!!
        assertEquals("H",PavementJointOccurrences.calculationRecords(saved,d.pavementDao().damages(s.id)).single().severity)
        assertEquals(before,d.pavementDao().damage("LEGACY"))
        repo.deleteDamage(before);saved=d.pavementDao().sample(s.id)!!
        assertTrue(d.pavementDao().damages(s.id).isEmpty());assertEquals("H",PavementJointOccurrences.calculationRecords(saved,emptyList()).single().severity)
    }}

}
