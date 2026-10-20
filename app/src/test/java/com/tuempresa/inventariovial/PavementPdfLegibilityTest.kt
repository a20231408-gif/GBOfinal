package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.*
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.pavement.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import org.robolectric.shadows.ShadowStatFs
import java.io.*
import kotlin.math.*

internal fun pdfReadableFixture(name:String):Pair<PavementSampleEntity,List<PavementDamageEntity>> {
    val length=when(name){"largo"->60.0;"pocos"->10.0;else->30.0}
    var s=visualSample(length)
    val ds=if(name=="concentrados") (1..24).map{i->
        visualDamage(if(i%2==0)29 else 30,i,1.2+(i%3)*.015,2.0+(i%4)*.01,1.8+(i%3)*.015,2.6+(i%4)*.01,1).copy(severity=listOf("L","M","H")[(i-1)%3])
    } else {
        val base= mutableListOf(visualDamage(24,1,.2,2.0,3.3,2.0,1).copy(severity="L"),
            visualDamage(24,2,5.0,.2,5.0,4.8,2).copy(severity="H"),
            visualDamage(29,3,.2,6.0,1.2,8.0,3),visualDamage(30,4,4.7,7.0,5.3,7.6,4))
        if(name=="largo")base+=visualDamage(22,5,6.8,56.0,5.8,55.2,24)
        if(name=="cercanos")base+=(5..14).map{i->visualDamage(24,i,.2+(i-5)*.12,11.0,.3+(i-5)*.12,14.0,5).copy(severity=listOf("L","M","H")[i%3])}
        base
    }
    for(d in ds.filter{it.code in listOf(29,30)})s=s.copy(sicDataJson=PavementPatchVisuals.update(s,d,if(d.code==29)PavementPatchVisuals.ANCHORED else PavementPatchVisuals.INTERIOR))
    return s to ds
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,shadows=[SyntheticPdf146::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementPdfLegibilityTest {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    @Test fun largeRegionsUseAtLeastEightyFivePercentWidthWithoutAnisotropicStretchOrMissingCoverage() {
        for(length in listOf(5.0,10.0,17.0,30.0,60.0,100.0)) {
            val s=visualSample(max(5.0,length)).copy(largoM=length)
            val regions=PavementRigidPdfLayout.regions(s)
            assertEquals(s.anchoM*s.largoM,regions.sumOf{(it.x1-it.x0)*(it.y1-it.y0)},1e-6)
            assertTrue(regions.all{it.width>=PavementRigidPdfLayout.WIDTH*.85-1e-6 && it.width<=750.00001 && it.height<=350.00001})
            assertTrue(regions.all{it.height>=150})
            regions.zipWithNext().forEach{(a,b)->assertEquals(a.scale,b.scale,1e-8)}
        }
    }
    @Test fun crowdedBandsNeverOverlapAndNoItemIsLostOrRenumbered() {
        for(name in listOf("pocos","concentrados","cercanos","largo")) {
            val (s,ds)=pdfReadableFixture(name);val before=ds.map{it.copy()};val plans=PavementRigidPdfScheme.plans(s,ds)
            assertEquals(ds.map{it.id}.toSet(),plans.flatMap{it.damages}.map{it.id}.toSet())
            for(p in plans)for(top in listOf(true,false)) {
                val groups=p.groups.filter{it.top==top};val refs=groups.flatMap{it.refs}
                assertEquals(refs.map{it.damage.id}.size,refs.map{it.damage.id}.toSet().size)
                groups.forEach{g->
                    assertTrue(g.left>=-1e-6 && g.left+g.width<=p.region.width+1e-6)
                    g.refs.mapNotNull{it.point}.forEach{assertTrue(it.x in -1e-8..p.region.width+1e-8 && it.y in -1e-8..p.region.height+1e-8)}
                }
                groups.zipWithNext().forEach{(a,b)->
                    assertTrue(a.left+a.width+PavementRigidPdfLayout.GAP<=b.left+1e-6)
                    if(a.anchor!=null && b.anchor!=null)assertTrue(a.anchor.x<=b.anchor.x)
                }
            }
            assertEquals(before,ds)
        }
    }
    @Test fun coincidentMarksUseSharedCalloutsAndExtraPanelsInsteadOfALeaderPerItem() {
        val (s,ds)=pdfReadableFixture("concentrados");val p=PavementRigidPdfScheme.plans(s,ds)
        assertTrue(p.any{it.count>1})
        val leaders=p.sumOf{it.groups.count{g->g.anchor!=null}}
        assertTrue("shared zone references must reduce leader bundles",leaders<ds.size/2)
        assertEquals(ds.map{it.correlativo}.toSet(),p.flatMap{it.damages}.map{it.correlativo}.toSet())
    }
    @Test fun labelsKeepCodeSeverityAndHistoricNumbersIncludingLargeIndices() {
        val d=visualDamage(29,1,.2,1.0,1.2,2.0,1).copy(correlativo=123456789,severity="H")
        assertEquals("#123456789 29/H",PavementRigidPdfScheme.label(d))
        assertTrue(PavementRigidPdfScheme.badgeWidth(d)>=Paint().apply{textSize=11f}.measureText(PavementRigidPdfScheme.label(d))+20)
        assertEquals(PavementSeverity.color("L"),PavementSeverity.color("B"));assertEquals(PavementSeverity.color("H"),PavementSeverity.color("A"))
    }
    @Test fun globalsAndHistoricUnlocatedDamageRemainExplicitWithoutAnInventedLeader() {
        val s=visualSample();val g=visualDamage(26,1,0.0,0.0,0.0,0.0,1).copy(geometry="GLOBAL",x1=null,y1=null,x2=null,y2=null,slabs="")
        val h=visualDamage(22,2,0.0,0.0,0.0,0.0,1).copy(x1=null,y1=null,x2=null,y2=null)
        val refs=PavementRigidPdfScheme.plans(s,listOf(g,h)).flatMap{it.groups}.flatMap{it.refs}
        assertTrue(refs.single{it.damage.id==g.id}.point==null)
        assertTrue(refs.filter{it.damage.id==h.id}.all{it.referential})
    }
    @Test fun pdfRenderingDoesNotMutateTechnicalInputsOrNativeJsonAndKeepsAllPhotos() {
        val(s,ds)=pdfReadableFixture("pocos");val result=PavementCalculator(c).calculate(s,ds);val before=PavementDriveUploader.nativeJson(s,ds,result,emptyMap()).toString()
        val bitmap=Bitmap.createBitmap(40,30,Bitmap.Config.ARGB_8888);val photo=File(c.filesDir,"TEST_READABILITY.jpg").apply{outputStream().use{bitmap.compress(Bitmap.CompressFormat.JPEG,90,it)}};bitmap.recycle()
        val evidence=mapOf(ds.first().id to listOf(PavementDamagePhotoEntity("TEST_PHOTO",ds.first().id,photo.path)))
        val pages=mutableListOf<Bitmap>()
        PavementPdf.render(PavementExportSample(s,ds,result),evidence,{Bitmap.createBitmap(842,595,Bitmap.Config.ARGB_8888).also{pages+=it}.let(::Canvas)},{})
        assertTrue(pages.size>=2);assertEquals(before,PavementDriveUploader.nativeJson(s,ds,result,emptyMap()).toString());assertTrue(photo.isFile)
        assertEquals(result,PavementCalculator(c).calculate(s,ds));pages.forEach{it.recycle()}
    }
    @Test fun localAndDrivePreparationStillUseTheSamePdfWriterWithoutCreatingARemoteIntent()=runBlocking {
        ShadowStatFs.registerStats(c.filesDir.path,1_000_000,900_000,900_000)
        val(s,ds)=pdfReadableFixture("pocos")
        Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use{db->
            db.pavementDao().saveSample(s);ds.forEach{db.pavementDao().saveDamage(it)}
            val job=LocalExportJob("TEST_READABILITY","PAVEMENT/${s.id}","PAVEMENT",s.id,s.updatedAt)
            val plan=LocalExportBuilder.build(c,db,job)
            assertTrue(plan.problems.joinToString(),plan.problems.isEmpty())
            val pdf=plan.files.single{it.mimeType=="application/pdf"};assertTrue(File(pdf.source).length()>1000)
            assertEquals(pdf.size to pdf.sha256,fileDigest(File(pdf.source)))
            assertTrue(plan.files.any{it.name=="Datos.xlsx"});assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }
    @Test fun renderVisualCasesWithTheActualPageRenderer() {
        val out=File(requireNotNull(System.getProperty("gbo.visual.output"))).apply{mkdirs()}
        for(name in listOf("pocos","concentrados","cercanos","largo")) {
            val(s,ds)=pdfReadableFixture(name);val data=PavementExportSample(s,ds,PavementCalculator(c).calculate(s,ds))
            File(out,"$name-SIMULADO.pdf").outputStream().use{PavementPdf.write(it,data,emptyMap())}
            val first=Bitmap.createBitmap(842,595,Bitmap.Config.ARGB_8888);var index=0
            val bitmaps=mutableListOf<Bitmap>()
            PavementPdf.render(data,emptyMap(),{index++;if(index==1)Canvas(first).apply{drawColor(Color.WHITE)}else Bitmap.createBitmap(842,595,Bitmap.Config.ARGB_8888).also{bitmaps+=it}.let(::Canvas)},{})
            File(out,"$name-primera-pagina-Canvas.png").outputStream().use{first.compress(Bitmap.CompressFormat.PNG,100,it)}
            first.recycle();bitmaps.forEach{it.recycle()}
        }
    }
    @Test fun emptyLongViewsDoNotCreateStandaloneNoticePages() {
        val s=visualSample(60.0);val data=PavementExportSample(s,emptyList(),PavementCalculator(c).calculate(s,emptyList()))
        val pages=mutableListOf<Bitmap>()
        PavementPdf.render(data,emptyMap(),{Bitmap.createBitmap(842,595,Bitmap.Config.ARGB_8888).also{pages+=it}.let(::Canvas)},{})
        assertTrue("four large views and at most one technical summary",pages.size<=5)
        pages.forEach{it.recycle()}
    }
}
