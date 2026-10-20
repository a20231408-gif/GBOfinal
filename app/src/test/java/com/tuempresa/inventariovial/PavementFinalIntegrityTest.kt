package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.pavement.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.*
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class PavementFinalIntegrityTest {
    private fun entries(bytes:ByteArray)=buildMap<String,String>{ZipInputStream(ByteArrayInputStream(bytes)).use{z->while(true){val e=z.nextEntry?:break;put(e.name,z.readBytes().toString(Charsets.UTF_8))}}}
    @Test fun patchModeDoesNotChangePciExcelOrMeasuredDamageAndKeepsOtherAnnotations() {
        val c=ApplicationProvider.getApplicationContext<Context>();val s=visualSample()
        val ds=listOf(visualDamage(29,1,.2,1.0,1.2,2.0,1),visualDamage(30,2,2.3,1.0,3.3,2.0,1))
        val calc=PavementCalculator(c);val result=calc.calculate(s,ds)
        var p=s
        for(d in ds)p=p.copy(sicDataJson=PavementPatchVisuals.update(p,d,PavementPatchVisuals.ANCHORED))
        assertEquals(result,calc.calculate(p,ds))
        fun xlsx(sample:PavementSampleEntity)=ByteArrayOutputStream().also{PavementExcel.write(it,listOf(PavementExportSample(sample,ds,result)))}.toByteArray()
        assertEquals(entries(xlsx(s)),entries(xlsx(p)))
        val a=PavementDriveUploader.nativeJson(s,ds,result,emptyMap());val b=PavementDriveUploader.nativeJson(p,ds,result,emptyMap())
        assertEquals(a.getJSONArray("damages").toString(),b.getJSONArray("damages").toString())
        val g=visualDamage(26,3,0.0,0.0,0.0,0.0,1).copy(geometry="GLOBAL",x1=null,y1=null,x2=null,y2=null,slabs="")
        val marks=setOf(PavementJointAnnotations.Mark("V",0,1))
        p=p.copy(sicDataJson=PavementJointAnnotations.update(p,g,marks))
        p=p.copy(sicDataJson=PavementPatchVisuals.update(p,ds.first(),PavementPatchVisuals.INTERIOR))
        assertEquals(marks,PavementJointAnnotations.marks(p,g.id))
        assertEquals(PavementPatchVisuals.ANCHORED,PavementPatchVisuals.mode(p,ds.last().id))
    }
    @Test fun cornersBothOrdersAndRectangularSlabsRemainValid(){PavementRigid147Test().allCornersBothTouchOrdersSquareAndRectangular()}
    @Test fun historicCornersRemainUnlocalized(){PavementRigid147Test().historicalCornerWithoutCoordinatesNeverInventsACrack()}
    @Test fun jointSegmentsAndCrossesRemainValid(){PavementRigid147Test().jointHitsSegmentsAndNodesWithNoInventedLongSections()}
    @Test fun jointMetadataAndContractualFieldsArePreserved(){PavementRigid147Test().annotationVersionGridReviewAndSicSupplementPreservation()}
}
