package com.tuempresa.inventariovial.core

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.location.*
import com.tuempresa.inventariovial.camera.PhotoCaptureMetadata
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class InventoryDraftStoreTest {
    @Test fun partialTextPhotosAndRtkSurviveReopeningWhileOtherUsersAndFinalizedRecordsAreBlocked()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val auth=AuthGraph.get(context).sessions
        fun login(id:String) { auth.logout();auth.login(UserCacheEntity(id,id,id,"OPERATOR",true,"",1,1,1)) }
        login("draft-owner")
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).allowMainThreadQueries().build()
        val photo=File(context.filesDir,"draft-original.jpg").apply { writeBytes(byteArrayOf(1,2,3)) }
        var id:String?=null
        try {
            val fix=GeoLocation(-12.0,-77.0,0.02f,100.0,"RTK",123,0.03f,GnssProvider.RTK_FIXED,"FIXED",18,0.6,1.0,true)
            val form=Sic22FormState(signWidthM="1,",signalCodeSource=SignalCodeSource.MANUAL,signalDescription="Parcial")
            val request=InventorySaveRequest("SIC-22","VERTICAL","PE-01","CD","","12,",null,"-",null,Double.NaN,Double.NaN,null,"08/10/2026","Parcial",photo.path,
                SicFormDetail.Sic22(form),photoPaths=listOf(photo.path),endLocation=fix,photoCaptures=mapOf(photo.path to PhotoCaptureMetadata(124,fix,"CAMERA")))
            id=InventoryDraftStore.save(context,db,request)
            val row=db.inventoryDao().recordById(id!!)!!
            assertEquals("DRAFT",row.status);assertEquals("DRAFT_LOCATION_PENDING",row.locationSource)
            assertEquals(-12.0,row.endLatitude!!,0.0)
            val raw=InventoryDraftStore.read(context,id)!!
            assertEquals("12,",raw.getString("startDistanceM"));assertEquals("-",raw.getString("endDistanceM"))
            assertEquals(form,InventoryDraftStore.restore(raw,Sic22FormState()))
            assertEquals(fix,InventoryDraftStore.restoreFix(raw.getJSONObject("endLocation")))
            assertEquals(fix,InventoryDraftStore.restoreFix(raw.getJSONObject("photoCaptures").getJSONObject(photo.path).getJSONObject("fix")))
            assertArrayEquals(byteArrayOf(1,2,3),photo.readBytes())
            login("other-owner")
            assertNull(InventoryDraftStore.read(context,id));assertFalse(InventoryDraftStore.accessible(context,id!!))
            assertTrue(runCatching { InventoryDraftStore.save(context,db,request.copy(recordId=id)) }.isFailure)
            login("draft-owner")
            db.inventoryDao().setRecordStatus(id!!,"ACTIVE",2)
            assertTrue(runCatching { InventoryDraftStore.save(context,db,request.copy(recordId=id)) }.isFailure)
            assertEquals("ACTIVE",db.inventoryDao().recordById(id!!)!!.status)
        } finally { id?.let { InventoryDraftStore.completed(context,it) };photo.delete();auth.logout();db.close() }
    }
}
