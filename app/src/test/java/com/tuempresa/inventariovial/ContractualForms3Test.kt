package com.tuempresa.inventariovial

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.sic.*
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.repository.EngineeringEdits
import com.tuempresa.inventariovial.validation.*
import com.tuempresa.inventariovial.data.entity.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class ContractualForms3Test {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    private fun request()=InventorySaveRequest("SIC-21","MARKS","PE-22A","CD","0010","125.50000","0011","999,75000","D",Double.NaN,Double.NaN,null,"11/10/2026","","",SicFormDetail.Sic21(Sic21FormState("18","1","1","1")))
    @Test fun noGpsAndDeniedPermissionAllowTechnicalValidation(){
        shadowOf(context as Application).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)
        assertTrue(CaptureValidation.errors(request()).isEmpty())
        assertFalse(CaptureValidation.warnings(request(),com.tuempresa.inventariovial.road.RoadReferenceData(),1).any{it.code in setOf("GPS_ACCURACY","GPS_STALE","MISSING_END_GPS")})
    }
    @Test fun coordinatePairsAreOptionalButNeverPartiallyValid(){
        assertNull(ContractualLocationInput.optionalCoordinateError("",""))
        assertNull(ContractualLocationInput.optionalCoordinateError("-12,125","-76.25"))
        for(pair in listOf("91" to "20","12" to "", "NaN" to "20", "12" to "181"))assertNotNull(ContractualLocationInput.optionalCoordinateError(pair.first,pair.second))
        assertTrue(CaptureValidation.errors(request().copy(latitude=12.0)).any{it.field=="location"})
    }
    @Test fun prPreservesLeadingZerosRejectsPasteAndFifthDigit(){
        assertTrue(ContractualLocationInput.acceptPr("0010"));assertNull(ContractualLocationInput.prError("0010"))
        for(value in listOf("00010","10a","10.0"," 0010")){assertFalse(ContractualLocationInput.acceptPr(value));assertNotNull(ContractualLocationInput.prError(value))}
        assertTrue(CaptureValidation.errors(request().copy(startPrCode="00010")).any{it.field=="startPrCode"})
    }
    @Test fun offsetsKeepPrecisionAndBothSeparators(){
        for(v in listOf("125.50","999.75","125,500000","0.000001","999")){assertTrue(ContractualLocationInput.acceptDistance(v));assertNull(ContractualLocationInput.distanceError(v))}
        for(v in listOf("1234.50","1234,50","1e3","-1","1.2.3")){assertFalse(ContractualLocationInput.acceptDistance(v));assertNotNull(ContractualLocationInput.distanceError(v))}
        assertTrue(CaptureValidation.errors(request().copy(endDistanceM="1234.50")).any{it.field=="endDistanceM"})
    }
    @Test fun fullRoadCatalogueDoesNotBypassOffsetLimit(){
        val pr=com.tuempresa.inventariovial.road.RoadPr("PE-22A","CD","0010",-12.0,-76.0,10000.0)
        assertFalse(com.tuempresa.inventariovial.road.ContractualPrInput.valid("0010","1234.50",listOf(pr),"PE-22A","CD"))
    }
    @Test fun dimensionsDoNotReceiveOffsetLimit(){
        val detail=SicFormDetail.Sic18(Sic18FormState(dimension1M="1234.50",dimension2M="4567.25",spans="1"))
        assertFalse(CaptureValidation.errors(request().copy(detail=detail)).any{it.field in setOf("dimension1M","dimension2M")})
    }
    @Test fun allExtensionLocationsRejectFourIntegerDigits(){
        for(format in listOf(SicFieldFormat.SIC08,SicFieldFormat.SIC09,SicFieldFormat.SIC13,SicFieldFormat.SIC34)){
            val s=fajaState(format).set("startDistanceM","1234.50")
            assertTrue(s.completion().issues.any{it.field=="Distancia inicio (m)"})
        }
    }
    @Test fun criticalAndAccidentObservationsPersistEditAndReopen()=runBlocking {
        val database=db();try{
            val repo=SicFieldRepository(database)
            assertTrue(runCatching{repo.save("bad-offset",fajaState(SicFieldFormat.SIC08).set("startDistanceM","1234.50"),"engineer",false)}.isFailure)
            assertNull(database.inventoryDao().recordById("bad-offset"))
            val notes="Observación de campo áéíóú\n".repeat(2000)
            for(format in listOf(SicFieldFormat.SIC13,SicFieldFormat.SIC34)){
                val s=fajaState(format).set("sideCode","D").set("classCode","15").set("typeCode","1").set("levelCode","1").set("observations",notes)
                repo.save(format.code,s,"engineer",true)
                val loaded=repo.load(format.code,format)!!;assertEquals(notes,loaded.text("observations"));assertNull(database.inventoryDao().recordById(format.code)!!.latitude)
                repo.save(format.code,loaded.set("observations",notes+"Corrección"),"engineer",true)
                assertEquals(notes+"Corrección",database.inventoryDao().recordById(format.code)!!.observations)
            }
        }finally{database.close()}
    }
    @Test fun historicalGpsIsPreservedUntilExplicitlyCleared()=runBlocking {
        val database=db();try{
            val repo=SicFieldRepository(database);repo.save("old",fajaState(SicFieldFormat.SIC08),"engineer",true)
            val original=database.inventoryDao().recordById("old")!!.copy(sideCode="D",latitude=-12.5,longitude=-76.5,gpsAccuracyM=0.02,gpsTimestamp=123,isRtkFixed=true,locationSource="EXTERNAL_GNSS")
            database.inventoryDao().updateRecord(original)
            var s=repo.load("old",SicFieldFormat.SIC08)!!;assertEquals("-12.5",s.text("_latitude"))
            repo.save("old",s.set("observations","No altera GPS"),"engineer",true)
            assertEquals(original.latitude,database.inventoryDao().recordById("old")!!.latitude);assertTrue(database.inventoryDao().recordById("old")!!.isRtkFixed);assertEquals("D",database.inventoryDao().recordById("old")!!.sideCode)
            s=repo.load("old",SicFieldFormat.SIC08)!!.set("_gpsExplicit","true").set("_latitude","").set("_longitude","").set("_coordinateSource","CONTRACTUAL_PR_ONLY")
            repo.save("old",s,"engineer",true);val cleared=database.inventoryDao().recordById("old")!!
            assertNull(cleared.latitude);assertNull(cleared.longitude);assertFalse(cleared.isRtkFixed)
        }finally{database.close()}
    }
    @Test fun manualGpsIsNotCertifiedOrSubmetric()=runBlocking {
        val database=db();try{
            val s=fajaState(SicFieldFormat.SIC09).set("_gpsExplicit","true").set("_latitude","-12.5").set("_longitude","-76.5").set("_coordinateSource","MANUAL_COORDINATES").set("_gpsTimestamp","123")
            SicFieldRepository(database).save("manual",s,"engineer",true)
            val r=database.inventoryDao().recordById("manual")!!;assertEquals("MANUAL_COORDINATES",r.locationSource);assertFalse(r.isRtkFixed);assertNull(r.gpsAccuracyM)
        }finally{database.close()}
    }
    @Test fun observationsReachDriveMetadataWithoutChangingContractualColumns()=runBlocking {
        val database=db();try{
            val repo=SicFieldRepository(database);repo.save("delivery",fajaState(SicFieldFormat.SIC08).set("observations","Corregido\nSin GPS"),"engineer",true)
            val intent=database.automaticSyncDao().get("SIC/delivery")!!
            val payload=AutomaticDelivery.prepare(context,database,intent)
            val files=payload.getJSONArray("files");val meta=(0 until files.length()).map{files.getJSONObject(it)}.first{it.getString("key")=="metadata"}
            val text=File(meta.getString("localFile")).readText();assertTrue(text.contains("Corregido"));assertTrue(text.contains("Sin GPS"))
            assertEquals(3,payload.getInt("syncProtocol"));assertEquals(4,payload.getInt("pathVersion"))
            assertEquals(10,SicFieldFormat.SIC08.fields.size)
        }finally{database.close()}
    }

    @Test fun scapSicOffsetsUseSameLimitsWithoutConstrainingFullProgressive()=runBlocking {
        val database=db();try{
            val repo=com.tuempresa.inventariovial.scap.data.ScapRepository(database,com.tuempresa.inventariovial.scap.catalog.ScapCatalog.load(context))
            val id=repo.create("engineer","test-device",null)
            val roadId=database.scapDao().snapshot(id)!!.inspection.roadRecordId
            assertNull(database.inventoryDao().recordById(roadId)!!.latitude)
            assertTrue(runCatching{repo.saveSicLocation(id,"PE-22A","CD","0010","1234.50")}.isFailure)
            repo.saveSicLocation(id,"PE-22A","CD","0010","999,750000","0011","125.50000")
            val record=database.inventoryDao().recordById(roadId)!!
            assertEquals("0010",record.startPrCode);assertEquals(999.75,record.startDistanceM,0.0)
            assertNotNull(com.tuempresa.inventariovial.scap.domain.ScapNumbers.chainage("30+298"))
        }finally{database.close()}
    }
}
