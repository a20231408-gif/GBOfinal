package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.sic.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import org.robolectric.shadows.ShadowStatFs
import java.io.File
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,shadows=[SyntheticPdf146::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LocalFiles148BuildersTest {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    @Before fun space(){ShadowStatFs.registerStats(c.filesDir.path,1_000_000,900_000,900_000)}
    private fun db()=Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build()
    private fun jpeg():File {val f=File(c.filesDir,"images/TEST_148.jpg").apply{parentFile!!.mkdirs()};val b=Bitmap.createBitmap(32,24,Bitmap.Config.ARGB_8888);f.outputStream().use{b.compress(Bitmap.CompressFormat.JPEG,95,it)};b.recycle();return f}
    @Test fun sicDocuments()=runBlocking{db().use{d->
        SicFieldRepository(d).save("TEST_SIC_148",fajaState(SicFieldFormat.SIC08),"TEST_USER",true)
        val r=d.inventoryDao().recordById("TEST_SIC_148")!!;val f=jpeg();d.inventoryDao().insertPhoto(PhotoEntity("TEST_PHOTO",r.id,1,f.path,true,null,null,null,"PENDING",1))
        val queue=d.automaticSyncDao().all();val original=fileDigest(f);val job=LocalExportJob("SIC_BUILD", "SIC/${r.id}","SIC",r.id,r.updatedAt)
        val plan=LocalExportBuilder.build(c,d,job)
        assertTrue(plan.problems.joinToString(),plan.files.any{it.name.endsWith("xlsx")});assertTrue(plan.files.any{it.mimeType=="image/jpeg"&&it.sha256==original.second});assertEquals(original,fileDigest(f));assertEquals(queue,d.automaticSyncDao().all())
        val book=plan.files.first{it.name.endsWith("xlsx")};ZipFile(book.source).use{assertNotNull(it.getEntry("xl/workbook.xml"))}
    }}
    @Test fun pavementDocuments()=runBlocking{db().use{d->
        var s=PavementSampleEntity("TEST_PAV_148","TRAMO_TEST","PE-22A",1,"Derecho","CD","00+000","00+020",10.0,20.0,"RIGID","2026-10-16","TEST_USER","FICTICIO",status="CLOSED",updatedAt=123,slabRows=4,slabColumns=2)
        val global=PavementDamageEntity("TEST_GLOBAL",s.id,1,"RIGID",26,"Daño del sello de junta","M","GLOBAL",null,null,null,null,null,null,null,null,null,null,"Texto literal")
        s=s.copy(sicDataJson=PavementJointAnnotations.update(s,global,setOf(PavementJointAnnotations.Mark("V",0,1))))
        d.pavementDao().saveSample(s);d.pavementDao().saveDamage(global)
        val corner=global.copy(id="TEST_CORNER",correlativo=2,code=22,name="Rotura de esquina",geometry="SLABS",x1=.5,y1=2.0,x2=2.0,y2=.5,slabs="1");d.pavementDao().saveDamage(corner)
        val f=jpeg();d.pavementDao().savePhoto(PavementDamagePhotoEntity("TEST_PHOTO",corner.id,f.path,123))
        val before=d.pavementDao().sample(s.id);val job=LocalExportJob("PAV_BUILD","PAVEMENT/${s.id}","PAVEMENT",s.id,123);val plan=LocalExportBuilder.build(c,d,job)
        assertTrue(plan.files.any{it.name=="Inspeccion.pdf"});assertTrue(plan.files.any{it.name=="Datos.xlsx"});val json=JSONObject(File(plan.files.single{it.name=="Muestra.json"}.source).readText());assertTrue(json.getJSONObject("sample").getJSONObject("sicData").has("_gboJointAnnotations"))
        assertTrue(plan.files.any{it.sha256==fileDigest(f).second});assertEquals(before,d.pavementDao().sample(s.id));assertTrue(d.automaticSyncDao().all().isEmpty())
        val zip=LocalExportBuilder.build(c,d,job.copy(id="PAV_ZIP",scope="ZIP"));ZipFile(zip.files.single().source).use{z->val names=z.entries().asSequence().map{it.name}.toSet();for(name in listOf("Inspeccion.pdf","Datos.xlsx","MANIFIESTO_LOCAL.json"))assertTrue(names.toString(),names.any{it.endsWith("/$name")})}
    }}
    @Test fun scapDocuments()=runBlocking{db().use{d->
        // Reuse an existing comprehensive synthetic fixture; no new contractual template invented.
        val fixture=ScapFullExportTest();val method=fixture.javaClass.getDeclaredMethod("fixture").apply{isAccessible=true};val original=method.invoke(fixture) as ScapInspectionSnapshot
        val s=original.copy(inspection=original.inspection.copy(status="COMPLETE",updatedAt=123))
        d.inventoryDao().insertRecord(testRecord(s.inspection.roadRecordId,sic="SCAP"));val dao=d.scapDao();dao.insertInspection(s.inspection)
        s.values.forEach{dao.putValue(it)};s.spans.forEach{dao.putSpan(it)};s.substructures.forEach{dao.putSubstructure(it)};s.supports.forEach{dao.putSupport(it)}
        s.elements.forEach{dao.putElement(it.element);it.condition?.let{row->dao.putCondition(row)}};s.defects.forEach{dao.putDefect(it)};s.sketches.forEach{dao.insertSketch(it)};s.profile.forEach{dao.putProfilePoint(it)};s.joints.forEach{dao.putJoint(it)}
        d.inventoryDao().insertPhotos(s.photos);val before=dao.snapshot(s.inspection.id)
        val job=LocalExportJob("SCAP_BUILD","SCAP/${s.inspection.id}","SCAP",s.inspection.id,123);val plan=LocalExportBuilder.build(c,d,job)
        assertTrue(plan.problems.joinToString(),plan.files.any{it.name=="SCAP_PROVISIONAL.xlsx"});assertTrue(plan.files.any{it.relativePath.contains("CROQUIS")});assertTrue(plan.files.any{it.relativePath.contains("FOTOGRAFIAS")})
        assertEquals(before,dao.snapshot(s.inspection.id));assertTrue(d.automaticSyncDao().all().isEmpty());val book=plan.files.single{it.name=="SCAP_PROVISIONAL.xlsx"};ZipFile(book.source).use{assertNotNull(it.getEntry("xl/workbook.xml"))}
    }}
    @Test fun pre29NeedsExplicitLocalFolder(){
        // URI persistence/provider UI is a physical check; no broad permission is added.
        val text=File("src/main/AndroidManifest.xml").readText();assertFalse(text.contains("MANAGE_EXTERNAL_STORAGE"));assertFalse(text.contains("WRITE_EXTERNAL_STORAGE"))
    }
}
