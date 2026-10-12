package com.tuempresa.inventariovial

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.FieldSession
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.core.InventoryDraftStore
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.sic.*
import org.json.JSONObject
import com.tuempresa.inventariovial.dashboard.DashboardDrafts
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28],application=Application::class)
class Dashboard6PersistenceTest {
    @Test fun legacyDraftReopensWithPhotosObservationsAndNoInventedSession()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val auth=AuthGraph.get(context).sessions
        auth.login(UserCacheEntity("test-owner","test-owner","Operador ficticio","OPERATOR",true,"",1,1,1))
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).allowMainThreadQueries().build().use {db->
            val record=testRecord("historical-draft",created=1770000000000,status="DRAFT",sic="SIC-22").copy(sessionId=null,observations="Observación original")
            db.inventoryDao().insertRecord(record)
            val form=Sic22FormState(signalDescription="Texto incompleto conservado")
            val request=InventorySaveRequest("SIC-22","VERTICAL","R","CD","0030","12,",null,null,null,
                Double.NaN,Double.NaN,null,"11/10/2026","Observación editada","photo-original.jpg",SicFormDetail.Sic22(form),
                recordId=record.id,photoPaths=listOf("photo-original.jpg"),photoCategories=mapOf("photo-original.jpg" to "PANORAMICA"),sessionId="today-session")
            try {
                assertEquals(record.id,InventoryDraftStore.save(context,db,request))
                val saved=db.inventoryDao().recordById(record.id)!!
                assertNull(saved.sessionId);assertEquals(record.createdAt,saved.createdAt)
                assertEquals("Observación editada",saved.observations)
                val raw=InventoryDraftStore.read(context,saved.id)!!
                assertEquals("photo-original.jpg",raw.getJSONArray("photoPaths").getString(0))
                assertEquals("PANORAMICA",raw.getJSONObject("photoCategories").getString("photo-original.jpg"))
                assertEquals(form,InventoryDraftStore.restore(raw,Sic22FormState()))
                assertEquals("12,",raw.getString("startDistanceM"))
            } finally {auth.logout()}
        }
    }
    @Test fun fieldFormatDraftPreservesNullHistoricalSessionEvenWhenNewSessionSupplied()=runBlocking {
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),InventoryDatabase::class.java).build().use {db->
            val record=testRecord("historical-sic08",created=1770000000000,status="DRAFT",sic="SIC-08").copy(sessionId=null)
            db.inventoryDao().insertRecord(record)
            val state=SicFieldState(SicFieldFormat.SIC08,values=mapOf("routeCode" to "R","roadbedCode" to "CD","startPrCode" to "0030","startDistanceM" to "25"))
            SicFieldRepository(db).save(record.id,state,"test-owner",false,sessionId="today-session")
            val saved=db.inventoryDao().recordById(record.id)!!
            assertNull(saved.sessionId);assertEquals(record.createdAt,saved.createdAt)
        }
    }
    @Test fun configureKeepsSessionDateOperatorIdentityAndSavedDraftContext()=runBlocking {
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),InventoryDatabase::class.java).build().use {db->
            val dao=db.inventoryDao()
            val session=FieldSession("session","Proyecto","Operador","test","R",null,"CD","INCREASING",1770000000000)
            dao.insertSession(session)
            val record=testRecord("draft",created=session.startTime,status="DRAFT").copy(sessionId=session.sessionId,observations="Texto histórico")
            dao.insertRecord(record)
            assertEquals(1,dao.configureSession("session","Proyecto corregido","R",null,"UD","DECREASING"))
            val edited=dao.sessionById("session")!!
            assertEquals(session.startTime,edited.startTime);assertEquals(session.operator,edited.operator)
            assertEquals(session.sessionId,edited.sessionId);assertEquals("DECREASING",edited.direction)
            dao.updateRecord(record.copy(startPrCode="0030",updatedAt=record.updatedAt+86400000))
            val saved=dao.recordById(record.id)!!
            assertEquals(DashboardDrafts.date(record,listOf(session)),DashboardDrafts.date(saved,dao.observeSessions().first()))
            assertEquals("CD",saved.roadbedCode);assertEquals("Texto histórico",saved.observations)
            assertEquals(record.createdAt,saved.createdAt)
            dao.endSessions(session.startTime+1)
            assertEquals(0,dao.configureSession("session","No modificar","R",null,"UD","INCREASING"))
        }
    }
    @Test fun storedDraftsRemainDiscoverableAndReactiveWithoutParallelStore()=runBlocking {
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),InventoryDatabase::class.java).build().use {db->
            val dao=db.inventoryDao()
            val a=testRecord("a",pr="10",status="DRAFT",created=1)
            val b=testRecord("b",pr="20",status="DRAFT",created=2)
            dao.insertRecord(a);dao.insertRecord(b)
            assertEquals(listOf("a","b"),DashboardDrafts.order(dao.observeDrafts().first(),emptyList(),emptyList()).map {it.id})
            dao.updateRecord(a.copy(startPrCode="30"))
            assertEquals(listOf("b","a"),DashboardDrafts.order(dao.observeDrafts().first(),emptyList(),emptyList()).map {it.id})
            assertEquals(2,dao.observeDrafts().first().size)
        }
    }
}
