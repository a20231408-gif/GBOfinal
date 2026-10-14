package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.sic.*
import com.tuempresa.inventariovial.field.SicCoreDraft
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.repository.*
import com.tuempresa.inventariovial.export.CellKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class SicFinalize10KTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    @Test fun standaloneFormsPersistCurrentValuesBeforeReviewAndConfirmSameUuid()=runBlocking {
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use {db->
            val repo=SicFieldRepository(db)
            for(format in SicFieldFormat.entries.filter{it!=SicFieldFormat.SIC18A}) {
                val id="test10k-${format.name}"
                val fields=format.fields.associate {f->f.key to when {
                    f.options.isNotEmpty()->f.options.first{it.code!=ACCIDENT_TYPE_SOURCE_CONFLICT}.code
                    f.key=="surveyDate"->"14/10/2026"
                    f.kind in setOf(CellKind.INTEGER,CellKind.DECIMAL)->"2"
                    else->"dato"
                }}+mapOf("routeCode" to "PE-3N","roadbedCode" to "UC","startPrCode" to "0001","startDistanceM" to "10","endPrCode" to "0002","endDistanceM" to "20","observations" to "ACTUAL 10K")
                val state=SicFieldState(format,fields)
                assertTrue(state.completion().issues.toString(),state.completion().canFinalize)
                repo.save(id,state,"test-operator",false)
                val draft=db.inventoryDao().recordById(id)!!
                assertEquals("DRAFT",draft.status);assertEquals("ACTUAL 10K",draft.observations)
                assertNull(db.automaticSyncDao().get("SIC/$id"))
                repo.save(id,state,"test-operator",true,expectedUpdatedAt=draft.updatedAt)
                val saved=db.inventoryDao().recordById(id)!!
                assertEquals(draft.id,saved.id);assertEquals(draft.createdAt,saved.createdAt)
                assertEquals("ACTIVE",saved.status);assertEquals("QUEUED",db.automaticSyncDao().get("SIC/$id")!!.state)
                assertTrue(runCatching{repo.save(id,state,"test-operator",true,expectedUpdatedAt=draft.updatedAt)}.isFailure)
                assertEquals(1,db.automaticSyncDao().all().count{it.key=="SIC/$id"})
            }
        }
    }
    @Test fun currentCoreReplacesIncompletePersistedHeaderBeforeValidationAndStaleConfirmationIsRejected()=runBlocking {
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use {db->
            val id="test10k-culvert"
            val record=testRecord(id,sic="SIC-18").copy(status="DRAFT",routeCode="",roadbedCode="",startPrCode="",endPrCode=null,endDistanceM=null,latitude=null,longitude=null)
            InventoryRepository(db).saveSic18(record,Sic18Entity(id,"06","1",1,"2",1.2,null,"1","1"),emptyList())
            val detail=SicFormDetail.Sic18(Sic18FormState(dimension1M="1.2"))
            val core=SicCoreDraft("PE-3N","UC","0001","42")
            // Direct confirmation must validate this header, not the old empty one.
            EngineeringEdits.save(db,id,detail,emptyList(),emptyMap(),expectedUpdatedAt=record.updatedAt,observations="CAMBIO ACTUAL",core=core)
            val saved=db.inventoryDao().recordById(id)!!
            assertEquals("PE-3N",saved.routeCode);assertEquals("0001",saved.startPrCode);assertEquals("CAMBIO ACTUAL",saved.observations)
            assertEquals("ACTIVE",saved.status);assertNotNull(db.automaticSyncDao().get("SIC/$id"))
            assertTrue(runCatching{EngineeringEdits.save(db,id,detail,emptyList(),emptyMap(),expectedUpdatedAt=record.updatedAt,core=core)}.isFailure)
            assertEquals(saved,db.inventoryDao().recordById(id))
        }
    }
    @Test fun invalidStandaloneReviewRemainsDraftAndDoesNotQueue()=runBlocking {
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use {db->
            val repo=SicFieldRepository(db);val id="test10k-invalid"
            val state=SicFieldState(SicFieldFormat.SIC09,mapOf("routeCode" to "PE-3N","roadbedCode" to "UC","startPrCode" to "0001","startDistanceM" to "10","endPrCode" to "0002","endDistanceM" to "20","observations" to "NO PERDER"))
            repo.save(id,state,"test",false)
            assertTrue(runCatching{repo.save(id,state,"test",true)}.isFailure)
            assertEquals("NO PERDER",db.inventoryDao().recordById(id)!!.observations)
            assertEquals("DRAFT",db.inventoryDao().recordById(id)!!.status)
            assertNull(db.automaticSyncDao().get("SIC/$id"))
        }
    }
}
